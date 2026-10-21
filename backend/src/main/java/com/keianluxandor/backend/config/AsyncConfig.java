package com.keianluxandor.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Lets emails be sent in the background, so requests don't wait for the mail server
@Configuration
@EnableAsync
public class AsyncConfig {
}