1. Set Partial Index
2. Handling jobs which failed.
2. State Management both in code
3. State management in database
4. Multiple workers
5. Update job status to failed on failure
6. On receiving notification make sure that only archive_pending jobs are processed.


## Questions
I have a batch job .. which uses postgres as queue.  while fetching job from database I'm using "For update skip locked". job has status field .. which goes from active to completed. Help me come up with reason as to why I should introduce "processing" state .. active -> processing -> completed. Since jobs will be locked while being processed I don't really see any point in introducing processing status 

If a job fails during processing, having a distinct "processing" state can help in identifying jobs that didn't complete successfully. This can be crucial for troubleshooting issues such as why a job failed, especially if the system crashes or if there is a need to restart or roll back operations.

Crash Recovery: In the event of a system crash or failure, jobs marked as "processing" can be easily identified and either restarted or rolled back, depending on the system design and requirements. This helps in ensuring data consistency and reliability.
Failover Mechanisms: In distributed systems where multiple processors might pick up jobs, knowing that a job is in a "processing" state (as opposed to just being locked) can inform failover mechanisms and job redistribution strategies without risking re-processing the same job.


Single transaction:

```

postgres-1  | 2024-04-23 17:39:20.004 GMT [232] LOG:  duration: 0.116 ms  bind S_2:
postgres-1  | 2024-04-23 17:39:41.885 GMT [232] LOG:  duration: 0.203 ms  bind S_3: BEGIN
postgres-1  | 2024-04-23 17:39:41.885 GMT [232] LOG:  execute S_3: BEGIN
postgres-1  | 2024-04-23 17:39:41.885 GMT [232] LOG:  duration: 0.114 ms
postgres-1  | 2024-04-23 17:39:41.888 GMT [232] LOG:  duration: 2.471 ms  bind S_4: SELECT * FROM archive_jobs WHERE status = $1 FOR UPDATE SKIP LOCKED LIMIT $2
postgres-1  | 2024-04-23 17:39:41.888 GMT [232] DETAIL:  parameters: $1 = 'active', $2 = '5'
postgres-1  | 2024-04-23 17:39:41.888 GMT [232] LOG:  execute S_4: SELECT * FROM archive_jobs WHERE status = $1 FOR UPDATE SKIP LOCKED LIMIT $2
postgres-1  | 2024-04-23 17:39:41.888 GMT [232] DETAIL:  parameters: $1 = 'active', $2 = '5'
postgres-1  | 2024-04-23 17:39:41.891 GMT [232] LOG:  duration: 2.760 ms
postgres-1  | 2024-04-23 17:40:29.613 GMT [232] LOG:  duration: 1.616 ms  parse <unnamed>: UPDATE archive_jobs SET status = $1 WHERE id = ANY($2)
postgres-1  | 2024-04-23 17:40:29.615 GMT [232] LOG:  duration: 1.604 ms  bind <unnamed>: UPDATE archive_jobs SET status = $1 WHERE id = ANY($2)
postgres-1  | 2024-04-23 17:40:29.615 GMT [232] DETAIL:  parameters: $1 = 'processing_active', $2 = '{9556b65f-bea4-43b8-93d0-8ea05d86a6db}'
postgres-1  | 2024-04-23 17:40:29.615 GMT [232] LOG:  execute <unnamed>: UPDATE archive_jobs SET status = $1 WHERE id = ANY($2)
postgres-1  | 2024-04-23 17:40:29.615 GMT [232] DETAIL:  parameters: $1 = 'processing_active', $2 = '{9556b65f-bea4-43b8-93d0-8ea05d86a6db}'
postgres-1  | 2024-04-23 17:40:29.618 GMT [232] LOG:  duration: 2.163 ms
postgres-1  | 2024-04-23 17:40:34.981 GMT [232] LOG:  duration: 0.044 ms  bind S_1: COMMIT
postgres-1  | 2024-04-23 17:40:34.981 GMT [232] LOG:  execute S_1: COMMIT

```


Now write a liquibase migration for creating job_queue table (in yaml format). This table should have following columns
prev_job_status (not null), curr_job_status, not null defaults to archive_pending, also add a forgein key constraint on 

