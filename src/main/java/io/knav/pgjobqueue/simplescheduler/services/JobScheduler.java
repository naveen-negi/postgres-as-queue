package io.knav.pgjobqueue.simplescheduler.services;

import io.knav.pgjobqueue.simplescheduler.entities.Job;
import io.knav.pgjobqueue.simplescheduler.repositories.JobsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.UUID;

@Component
public class JobScheduler {

    private final JobsRepository jobsRepository;
    private final JobService jobService;

    @Autowired
    public JobScheduler(JobsRepository jobsRepository,
                        JobService jobService) {

        this.jobsRepository = jobsRepository;
        this.jobService = jobService;
    }

//    @Scheduled(cron = "0/2 * * * * ?")
//    public void addJobEveryMinute() {
//        // Logic to add a row to the database
//        System.out.println("Adding a row to the database every minute.");
//        // Assume a service that handles the database operation
//        jobsRepository.addJob(new Job(UUID.randomUUID(), "a new Job " + LocalTime.now().toString(), "active"));
//    }
//
//    @Scheduled(cron = "0/10 * * * * ?")
//    public void PollJobsEvery2Minute() {
//        // Logic to add a row to the database
//        System.out.println("Polling for Jobs");
//        var jobs = jobsRepository.fetchJobs();
//        jobs.forEach(jobService::processJob);
//    }
}
