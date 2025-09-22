package com.logpilot.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.logpilot.core", "com.logpilot.server"})
@ConfigurationPropertiesScan("com.logpilot.core.config")
public class LogPilotServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogPilotServerApplication.class, args);
    }
}