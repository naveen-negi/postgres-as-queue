package io.knav.pgjobqueue.advancedscheduler.services;

import io.knav.pgjobqueue.advancedscheduler.entities.JobNg;
import io.knav.pgjobqueue.advancedscheduler.entities.Metadata;
import io.knav.pgjobqueue.advancedscheduler.repositories.AdvancedJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdvancedJobScheduler {

    private final AdvancedJobRepository jobsRepository;
    private final AdvancedJobService jobService;

    @Autowired
    public AdvancedJobScheduler(AdvancedJobRepository jobsRepository,
                                AdvancedJobService jobService) {

        this.jobsRepository = jobsRepository;
        this.jobService = jobService;
    }

    @Scheduled(cron = "0/2 * * * * ?")
    public void addJobEveryMinute() {
        // Logic to add a row to the database
        System.out.println("Adding a row to the database every minute.");
        // Assume a service that handles the database operation
        jobsRepository.addJob(new JobNg(UUID.randomUUID(), new Metadata(UUID.randomUUID().toString()), "archive_pending"));
    }

    @Scheduled(cron = "0/10 * * * * ?")
    public void PollJobsEvery2Minute() {
        // Logic to add a row to the database
        System.out.println("Polling for Jobs");
        var jobs = jobsRepository.fetchJobs();
        jobs.forEach(jobService::processJob);

    }
}
