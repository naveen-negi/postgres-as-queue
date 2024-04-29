package io.knav.pgjobqueue.advancedscheduler.repositories;


import io.knav.pgjobqueue.advancedscheduler.entities.JobNg;
import io.knav.pgjobqueue.advancedscheduler.entities.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdvancedJobRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AdvancedJobRepository(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    public void addJob(JobNg job) {
        jdbcTemplate.update("INSERT INTO job_queue_view (id, metadata, curr_job_status) VALUES (?,?::json,?)", job.id(), job.metadata().asJson(), "archive_pending");
    }

    public JobNg fetchAndLockJobForProcessing(UUID id) {
        try {

            List<JobNg> jobs = jdbcTemplate.query(
                    "SELECT * FROM job_queue_view WHERE id = ? FOR UPDATE SKIP LOCKED",
                    new Object[]{id},
                    (rs, rowNum) -> {
                        UUID jobId = rs.getObject("id", UUID.class);
                        Metadata metadata = Metadata.fromJson(rs.getString("metadata"));
                        String jobStatus = rs.getString("curr_job_status");
                        return new JobNg(jobId, metadata, jobStatus);
                    });

            return jobs.isEmpty() ? null : jobs.get(0);
        } catch (DataAccessException dae) {
            // Handle data access exceptions (such as SQL errors)
            System.err.println("Data Access Error fetching job by ID: " + dae.getMessage());
        } catch (Exception e) {
            // Handle other exceptions (such as JSON parsing errors)
            System.err.println("Error fetching job by ID: " + e.getMessage());
        }
        return null;
    }

    @Transactional
    public List<JobNg> fetchJobs() {
        // Lock and select jobs
        List<JobNg> jobs = jdbcTemplate.query(
                "SELECT id, metadata, curr_job_status FROM job_queue_view WHERE curr_job_status = ? FOR UPDATE SKIP LOCKED LIMIT ?",
                (rs, rowNum) -> {
                    UUID id = rs.getObject("id", UUID.class);
                    try {
                        String metadataJson = rs.getString("metadata");
                        var metadata = Metadata.fromJson(metadataJson);
                        String currentJobStatus = rs.getString("curr_job_status");
                        return new JobNg(id, metadata, currentJobStatus);
                    } catch (Exception e) {
                        // Log or handle the exception as needed
                        throw new RuntimeException("Error reading metadata for job: " + id, e);
                    }
                },
                "archive_pending", 5
        );

        // Get IDs from the fetched jobs
        List<UUID> jobIds = jobs.stream().map(JobNg::id).toList();

        // Update statuses if jobs are found
        if (!jobIds.isEmpty()) {
            jdbcTemplate.update(
                    "UPDATE job_queue_view SET curr_job_status = ? WHERE id = ANY(?)",
                    "archive_processing", jobIds.toArray(UUID[]::new)
            );
        }

        return jobs;
    }


    public void UpdateJobStatus(UUID id, String status) {
            jdbcTemplate.update(
                    "UPDATE job_queue_view SET curr_job_status = ? WHERE id = ?",
                    status, id);
    }


}
