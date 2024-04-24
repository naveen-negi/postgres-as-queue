package io.knav.pgjobqueue.controller;

import io.knav.pgjobqueue.entities.Job;
import io.knav.pgjobqueue.repositories.JobsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jobs")
public class JobsController {

    @Autowired
    private JobsRepository jobsRepository;

    @PostMapping
    public Job  create(@RequestBody JobRequest request) {
        Job job = request.toDomain();
        jobsRepository.addJob(job);
        return job;
    }
}
