-- This script creates or replaces a PostgreSQL function named handle_job_update
-- The function is intended to be used as a trigger function
CREATE OR REPLACE FUNCTION handle_job_update()
    RETURNS trigger AS $$
BEGIN
    -- Check if the current job status has changed
    IF NEW.curr_job_status IS DISTINCT FROM OLD.curr_job_status THEN
        -- Update the original table
        UPDATE job_queue
        SET prev_job_status = OLD.curr_job_status,
            curr_job_status = NEW.curr_job_status,
            metadata = NEW.metadata,
            lease_expire = CASE
                               WHEN NEW.curr_job_status = 'processing_active' THEN NOW() + INTERVAL '15 MINUTES'
                               ELSE lease_expire
                END
        WHERE id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
