# Building a Real-Time, Thread-Safe, Resilient, and Type Safe Queue with PostgreSQL
For a detailed guide on implementing a queue system using PostgreSQL, check out the full blog post [here](https://naveennegi.medium.com/postgres-as-queue-deep-dive-into-fairly-advanced-implementation-68f28041853e).

This repository showcases a sophisticated use of PostgreSQL to manage queues effectively. If you're already utilizing PostgreSQL in your infrastructure, this method integrates seamlessly without the need for additional technologies. PostgreSQL's robust capabilities allow it to manage substantial loads with ease, and strategic database design can dramatically improve overall system performance.
## Topics Covered in This Blog

- Designing a table as a Queue
- Managing Concurrency
- Real-Time Job Processing
- Resilience and Failover Strategy
- Enforcing Valid State Transition for Jobs
- Managing "Stuck" Jobs with Lease Expiry
- Simplifying Table Design with Updatable Views

### Table Design

Consider a table designed to manage jobs, named `archive_jobs`. The structure of this table is straightforward and includes the following columns: Id, status, and description.

```plaintext
+-------------+--------------+---------------+
| Column Name | Data Type    | Constraints   |
+-------------+--------------+---------------+
| id          | UUID         | Not Null, Primary Key |
| description | VARCHAR(255) | Not Null              |
| status      | VARCHAR(255) |                       |
+-------------+--------------+---------------+
```

### Handling Concurrent Access with "skip locked"

If multiple processes or threads, such as several instances of a service running in Kubernetes pods, are reading from the table concurrently, effective concurrency management is crucial. You can achieve this with the SQL command:

```sql
SELECT * FROM archive_jobs WHERE status = 'active' FOR UPDATE SKIP LOCKED
```

This command specifically targets rows that are in the 'active' state and does two key things:

- **Skip Locked Rows**: It skips over any rows that are already locked by other processes. This feature is particularly useful in high-concurrency environments as it prevents processes from waiting on each other, thereby reducing bottlenecks.
- **Lock Rows**: Once it finds rows that are not locked, it locks them. This lock persists for the duration of the transaction, ensuring that no other process can modify these rows until the transaction is completed.

### Updating Job Status Within Transactions

```java
@Transactional
public List<Job> fetchJobs() {
    List<Job> jobs = jdbcTemplate.query(
        "SELECT * FROM archive_jobs WHERE status = ? FOR UPDATE SKIP LOCKED LIMIT ?",
        (rs, rowNum) -> new Job(rs.getObject("id", UUID.class), rs.getString("description"), rs.getString("status")),
        "active", 5);

    List<UUID> jobIds = jobs.stream().map(Job::id).toList();

    if (!jobIds.isEmpty()) {
        jdbcTemplate.update(
            "UPDATE archive_jobs SET status = ? WHERE id = ANY(?)",
            "processing_active", jobIds.toArray(UUID[]::new));
    }

    return jobs;
}
```

This function ensures that no other transaction can select these rows for updating until the current transaction is complete, clearly signals to other processes that these jobs are now in a ‘processing’ state, and commits the transaction to release the locks and finalize the state change.

### Real-Time Processing with NOTIFY and LISTEN

#### Database Side:

```sql
CREATE FUNCTION notify_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        PERFORM pg_notify('jobs_notification', row_to_json(NEW)::text);
    END IF;
    RETURN NEW;
END;
$$;

ALTER FUNCTION notify_change() OWNER TO "user";
```

#### Service Side in Java:

```java
@PostConstruct
public void init() {
    ObjectMapper om = new ObjectMapper();

    connection.connect().thenAccept(conn -> {
        System.out.println("Connected to PostgreSQL database!");
        conn.sendQuery("LISTEN jobs_notification").thenAccept(queryResult -> {
            System.out.println("Listening on channel 'jobs_notification'");
        });

        conn.registerNotifyListener(notification -> {
            System.out.println("Received notification on channel " + notification.getChannel() + ": " + notification.getPayload());
            try {
                var job = om.readValue(notification.getPayload(), Job.class);
                Job lockedJob = jobsRepository.fetchAndLockJobForProcessing(job.id());
                jobService.processJob(lockedJob);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }).exceptionally(throwable -> {
        System.err.println("Connection failed: " + throwable.getMessage());
        return null;
    });
}
```

### Making Tables Fast with Partial Indexes

```sql
CREATE INDEX idx_job_queue_archive_pending ON archive_jobs(status) WHERE status = 'active';
```

This partial index optimizes the performance by indexing only the rows that meet the specified condition, thus being smaller and faster.

Here's the continuation of your content formatted in Markdown for GitHub README or similar documentation:

---

## Enforcing Valid State Transitions

Our current design guidelines enable the creation of a robust queuing system with PostgreSQL. However, to enhance system reliability further, we introduce safeguards against improper state transitions, which can lead to significant challenges if not managed carefully.

### Scenario

Imagine a scenario where, due to a bug or incorrect implementation, a job moves from "completed" back to "active" without any checks. This can disrupt the flow and integrity of the process.

### State Transition Table

To prevent such issues, we implement a state transition table that explicitly defines allowed transitions:

```sql
-- Create the job_transitions table with primary key
CREATE TABLE job_transitions (
    prev_job_status VARCHAR(255) NOT NULL,
    curr_job_status VARCHAR(255) NOT NULL,
    PRIMARY KEY (prev_job_status, curr_job_status)
);

-- Insert valid state transitions into the job_transitions table
INSERT INTO job_transitions (prev_job_status, curr_job_status) VALUES 
('archive_pending', 'archive_processing'),
('archive_processing', 'archive_completed'),
('archive_completed', 'job_ready_for_deletion');
```

### Revised Job Queue Table

We also adjust our main job table to align with these transitions:

```sql
CREATE TABLE public.job_queue (
    prev_job_status VARCHAR(255) NOT NULL DEFAULT 'archive_pending',
    curr_job_status VARCHAR(255) NOT NULL DEFAULT 'archive_pending',
    job_status_time TIMESTAMP NOT NULL DEFAULT now(),
    id UUID NOT NULL UNIQUE,
    metadata JSON NOT NULL,
    lease_expire TIMESTAMP,
    CONSTRAINT fk_job_queue_job_transitions
        FOREIGN KEY (prev_job_status, curr_job_status) 
        REFERENCES job_transitions(prev_job_status, curr_job_status)
        ON DELETE CASCADE
);
```

This setup uses a foreign key constraint to validate transitions, ensuring that any changes in job status adhere to predefined rules, thereby preventing illegal changes.

## Dealing with Stuck Jobs

A common challenge in job queue systems is handling jobs that become stuck in a "processing" state indefinitely. To address this, we introduce a lease expiry mechanism.

### Function to Handle Job Updates

```sql
CREATE FUNCTION handle_job_update() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.curr_job_status IS DISTINCT FROM OLD.curr_job_status THEN
        UPDATE job_queue SET 
            prev_job_status = OLD.curr_job_status,
            curr_job_status = NEW.curr_job_status,
            metadata = NEW.metadata,
            lease_expire = CASE
                WHEN NEW.curr_job_status = 'archive_processing' THEN NOW() + INTERVAL '15 MINUTES'
                ELSE lease_expire
            END
        WHERE id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;

ALTER FUNCTION handle_job_update() OWNER TO "user";
```

### Trigger to Apply Function

```sql
CREATE TRIGGER job_queue_update
    INSTEAD OF UPDATE ON job_queue
    FOR EACH ROW EXECUTE PROCEDURE handle_job_update();
```

This approach ensures that jobs do not remain stuck by setting a timeout that allows other processes to pick up or reassign the job if it does not complete within the designated period.

## Simplifying Table Design with Updatable View

As our `job_queue` table evolves, it incorporates several fields and constraints critical for managing job processes effectively. To simplify interactions and focus on essential data:

### Updatable View

```sql
CREATE VIEW job_queue_view AS
SELECT id, curr_job_status, metadata FROM job_queue;

-- Adjusting the trigger for the view
CREATE TRIGGER job_queue_view_update
    INSTEAD OF UPDATE ON job_queue_view
    FOR EACH ROW EXECUTE PROCEDURE handle_job_update();
```

This view simplifies the interface for users and applications, focusing on key job details while maintaining data integrity and functionality through backend processes.

### Implementation in Java

```java
@Service
public class AdvancedJobRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AdvancedJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addJob(JobNg job) {
        jdbcTemplate.update("INSERT INTO job_queue_view (id, metadata, curr_job_status) VALUES (?, ?::json, ?)",
            job.id(), job.metadata().asJson(), "archive_pending");
    }

    public JobNg fetchAndLockJobForProcessing(UUID id) {
        return jdbcTemplate.queryForObject(
            "SELECT * FROM job_queue_view WHERE id = ? FOR UPDATE SKIP LOCKED",
            new Object[]{id},
            (rs, rowNum) -> new JobNg(
                rs.getObject("id", UUID.class),
                Metadata.fromJson(rs.getString("metadata")),
                rs.getString("curr_job_status")
            ));
    }

    @Transactional
    public List<JobNg> fetchJobs(String status, int limit) {
        return jdbcTemplate.query(
            "SELECT id, metadata, curr_job_status FROM job_queue_view WHERE curr_job_status = ? FOR UPDATE

 SKIP LOCKED LIMIT ?",
            new Object[]{status, limit},
            (rs, rowNum) -> new JobNg(
                rs.getObject("id", UUID.class),
                Metadata.fromJson(rs.getString("metadata")),
                rs.getString("curr_job_status")
            ));
    }

    public void updateJobStatus(UUID id, String status) {
        jdbcTemplate.update("UPDATE job_queue_view SET curr_job_status = ? WHERE id = ?", status, id);
    }
}
```

This setup allows for the management of job data using an abstraction layer that simplifies interactions but ensures operations like status updates and job processing adhere to business rules and data integrity constraints.

Enjoy the full implementation and extend it as needed for your specific requirements!
