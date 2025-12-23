package com.logpilot.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    @GetMapping("/hello")
    public String hello() {
        log.info("Hello LogPilot from Logback! (INFO)");
        log.debug("This is a debug message (DEBUG)");
        log.warn("This is a warning message (WARN)");
        log.error("This is an error message (ERROR)");

        return "Logs generated!";
    }

    @GetMapping("/error-test")
    public String errorTest() {
        try {
            throw new RuntimeException("Simulated exception");
        } catch (Exception e) {
            log.error("Caught an exception", e);
        }
        return "Error logged!";
    }
}
