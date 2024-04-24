package io.knav.pgjobqueue.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection;
import com.github.jasync.sql.db.postgresql.util.URLParser;
import io.knav.pgjobqueue.entities.Job;
import io.knav.pgjobqueue.repositories.JobsRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;

import static kotlin.jvm.internal.Reflection.typeOf;

/*
On each notification I should try fetching the row with skipped lock build with a notification

This also should be done everytime worker wakes up

what happens if job is processing and the worker dies ?


BEGIN;

-- Select and lock a few jobs for processing
SELECT * FROM jobs
WHERE status = 'active'
FOR UPDATE SKIP LOCKED
LIMIT 5;

-- Update their status to 'processing'
UPDATE jobs
SET status = 'processing'
WHERE id IN (SELECT id FROM fetched_jobs); -- Assume 'fetched_jobs' is the result of the SELECT query

COMMIT;


what happens if job is processing and the worker dies ?

UPDATE jobs
SET status = 'active'
WHERE status = 'processing' AND last_updated < NOW() - INTERVAL '1 hour';


// Enhancements: Use the state constraint from the sets books


 */

@Service
public class NotificationService {

    private final PostgreSQLConnection connection;
    private final JobService jobService;
    private final JobsRepository jobsRepository;

    @Autowired
    public NotificationService(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            JobService jobService,
            JobsRepository jobsRepository
            ) {
        this.jobService = jobService;
        this.jobsRepository = jobsRepository;

        String connectionUrl = String.format("%s?user=%s&password=%s", url, username, password);
        connection = new PostgreSQLConnection(URLParser.INSTANCE.parseOrDie(connectionUrl, Charset.defaultCharset()));
    }

    @PostConstruct
    public void init() {

        ObjectMapper om = new ObjectMapper();

        // Connect and listen to notifications
        connection.connect().thenAccept(conn -> {
            System.out.println("Connected to PostgreSQL database!");
            conn.sendQuery("LISTEN jobs_notification").thenAccept(queryResult -> {
                    System.out.println("Listening on channel 'jobs_notification'");
                    }
            );

            // Handle notifications
            conn.registerNotifyListener( notification -> {
                System.out.println("**************************************************");
                System.out.println("Received notification on channel " + notification.getChannel() + ": " + notification.getPayload());
                System.out.println("**************************************************");
                try {
                    var job = om.readValue(notification.getPayload(), Job.class);
                    Job lockedJob = jobsRepository.fetchAndLockJobForProcessing(job.id());
                    jobService.processJob(lockedJob);

                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        }).exceptionally(throwable -> {
            System.err.println("Connection failed: " + throwable.getMessage());
            return null;
        });
    }
}