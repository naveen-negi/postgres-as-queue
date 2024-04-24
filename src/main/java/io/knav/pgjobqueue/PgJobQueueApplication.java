package io.knav.pgjobqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PgJobQueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(PgJobQueueApplication.class, args);
    }

}
