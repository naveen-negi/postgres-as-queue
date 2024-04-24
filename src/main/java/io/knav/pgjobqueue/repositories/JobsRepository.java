package io.knav.pgjobqueue.repositories;

import io.knav.pgjobqueue.entities.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class JobsRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JobsRepository(JdbcTemplate jdbcTemplate){

        this.jdbcTemplate = jdbcTemplate;
    }

    public void addJob(Job job) {
         jdbcTemplate.update("INSERT INTO archive_jobs (id, description, status) VALUES (?,?,?)", job.id(), job.description(), job.status());
    }

    //TODO: Why do we need processing_active status if rows are being locked.
    // can't there be status like .. active => completed
    public Job fetchAndLockJobForProcessing(UUID id) {
        try {
            List<Job> jobs = jdbcTemplate.query(
                    "SELECT * FROM archive_jobs WHERE id = ? FOR UPDATE SKIP LOCKED",
                    (rs, rowNum) -> new Job(rs.getObject("id", UUID.class), rs.getString("description"), rs.getString("status")), id);

            if (jobs.isEmpty()) {
                return null; // No jobs available that match the ID, or they are locked
            }
            return jobs.get(0); // Return the first job since ID should be unique
        } catch (Exception e) {
            System.err.println("Error fetching job by ID: " + e.getMessage());
            return null; // or handle more appropriately as per your error handling policy
        }
    }

    // This method is called only when worker wakes up. Once it has clear backlog. It can continue listening to event and process
    // jobs real time
    @Transactional
    public List<Job> fetchJobs() {
        // Lock and select jobs
        List<Job> jobs = jdbcTemplate.query(
                "SELECT * FROM archive_jobs WHERE status = ? FOR UPDATE SKIP LOCKED LIMIT ?",
                (rs, rowNum) -> new Job(rs.getObject("id", UUID.class), rs.getString("description"), rs.getString("status")),
        "active", 5 );

        // Get IDs from the fetched jobs
        List<UUID> jobIds = jobs.stream().map(Job::id).toList();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Update statuses if jobs are found
        if (!jobIds.isEmpty()) {
            jdbcTemplate.update(
                    "UPDATE archive_jobs SET status = ? WHERE id = ANY(?)",
                    "processing_active", jobIds.toArray(UUID[]::new)
            );
        }

        return jobs;
    }

}
