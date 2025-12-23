package com.logpilot.demo;

import com.logpilot.demo.service.TrafficGenerator;
import com.logpilot.demo.service.TrafficGenerator.SimulationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);
    private final TrafficGenerator trafficGenerator;

    public DemoController(TrafficGenerator trafficGenerator) {
        this.trafficGenerator = trafficGenerator;
    }

    @PostMapping("/simulation/start")
    public String startSimulation(@RequestParam(defaultValue = "STEADY") String mode) {
        try {
            SimulationMode simulationMode = SimulationMode.valueOf(mode.toUpperCase());
            trafficGenerator.setMode(simulationMode);
            return "Simulation started in " + simulationMode + " mode.";
        } catch (IllegalArgumentException e) {
            return "Invalid mode. Use STEADY or PEAK.";
        }
    }

    @PostMapping("/simulation/stop")
    public String stopSimulation() {
        trafficGenerator.setMode(SimulationMode.STOPPED);
        return "Simulation stopped.";
    }

    @GetMapping("/simulation/status")
    public String getStatus() {
        return "Current Mode: " + trafficGenerator.getMode();
    }

    @GetMapping("/hello")
    public String hello() {
        log.info("Manual hello request triggered.");
        return "Hello from LogPilot Demo!";
    }
}
