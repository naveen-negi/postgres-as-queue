package io.knav.pgjobqueue.simplescheduler.services;

import io.knav.pgjobqueue.simplescheduler.entities.Job;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    public void processJob(Job job) {
        var simulations = new ProcessingSimulation();
            simulations.simulateProcessing(job);
    }
}
