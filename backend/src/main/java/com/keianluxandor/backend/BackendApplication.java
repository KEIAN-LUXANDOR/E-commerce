package com.keianluxandor.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        // Run the backend in UTC. PostgreSQL rejects Windows' old "Asia/Calcutta" name,
        // and UTC is the standard for servers. The website shows times in IST.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(BackendApplication.class, args);
    }
}