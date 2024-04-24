package io.knav.pgjobqueue.services;

import io.knav.pgjobqueue.entities.Job;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    public void processJob(Job job) {
        var simulations = new ProcessingSimulation();
            simulations.simulateProcessing(job);
    }
}
