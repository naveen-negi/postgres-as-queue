package io.knav.pgjobqueue.advancedscheduler.services;

import io.knav.pgjobqueue.advancedscheduler.entities.JobNg;
import io.knav.pgjobqueue.advancedscheduler.repositories.AdvancedJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdvancedJobService {

    private final AdvancedJobRepository advancedJobRepository;

    @Autowired
    public AdvancedJobService(AdvancedJobRepository advancedJobRepository) {

        this.advancedJobRepository = advancedJobRepository;
    }

    public void processJob(JobNg job) {

        try {
            var simulations = new AdvancedProcessingSimulation();
            simulations.simulateProcessing(job);
            advancedJobRepository.UpdateJobStatus(job.id(), "archive_completed");
        } catch (Exception e) {
            advancedJobRepository.UpdateJobStatus(job.id(), "archive_failed");
        }
    }
}
