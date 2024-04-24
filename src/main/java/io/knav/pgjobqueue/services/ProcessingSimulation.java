package io.knav.pgjobqueue.services;

import io.knav.pgjobqueue.entities.Job;

import java.util.Random;

public class ProcessingSimulation {

    private static final int MAX_SLEEP_TIME_MS = 5000; // Maximum sleep time in milliseconds
    private static final double FAILURE_RATE = 0.2; // 20% chance of failure

    /**
     * Simulates processing by performing some dummy operations, introducing a random delay, and randomly failing.
     */
    public void simulateProcessing(Job job) {
        Random random = new Random();
        int sleepTime = random.nextInt(MAX_SLEEP_TIME_MS + 1); // Random sleep time up to MAX_SLEEP_TIME_MS

        try {
            System.out.println("Processing starts. Sleeping for " + sleepTime + " ms.");
            Thread.sleep(sleepTime); // Sleep for a random time up to 5 seconds

            // Randomly decide if this attempt should fail
            if (random.nextDouble() < FAILURE_RATE) {
                throw new RuntimeException("Simulated processing failure for Job: " + job.id());
            }

            // Simulate computational work
            int data = random.nextInt(1000); // Generate some random data
            double result = 0;
            for (int i = 0; i < data; i++) {
                result += Math.sin(random.nextDouble()) * Math.cos(random.nextDouble());
            }

            System.out.println("Processing completed for Job : " + job.id());
        } catch (InterruptedException e) {
            System.err.println("Processing was interrupted for Job: "  + job.id());
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            System.err.println("Error during processing: " + e.getMessage());
        }
    }
}
