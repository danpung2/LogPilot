package com.logpilot.demo.service;

import com.logpilot.demo.domain.Job;
import com.logpilot.demo.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class TrafficGenerator {

    private static final Logger log = LoggerFactory.getLogger(TrafficGenerator.class);
    private final RecruitmentService recruitmentService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<SimulationMode> currentMode = new AtomicReference<>(SimulationMode.STOPPED);

    public TrafficGenerator(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
        startWorkerThreads();
    }

    public enum SimulationMode {
        STOPPED,
        STEADY, // Low traffic: Job views mostly
        PEAK // High traffic: Many applications, errors, high concurrency
    }

    public void setMode(SimulationMode mode) {
        log.info("Switching Traffic Mode: {} -> {}", currentMode.get(), mode);
        currentMode.set(mode);
        running.set(mode != SimulationMode.STOPPED);
    }

    public SimulationMode getMode() {
        return currentMode.get();
    }

    private void startWorkerThreads() {
        // 5 Worker threads to simulate concurrent users
        for (int i = 0; i < 5; i++) {
            executorService.submit(this::trafficLoop);
        }
    }

    private void trafficLoop() {
        while (true) {
            try {
                if (!running.get()) {
                    Thread.sleep(1000);
                    continue;
                }

                SimulationMode mode = currentMode.get();
                if (mode == SimulationMode.STEADY) {
                    performSteadyAction();
                    Thread.sleep(500 + (long) (Math.random() * 1000)); // 0.5s - 1.5s delay
                } else if (mode == SimulationMode.PEAK) {
                    performPeakAction();
                    Thread.sleep(50 + (long) (Math.random() * 100)); // 0.05s - 0.15s delay (FAST!)
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in traffic generator loop", e);
            }
        }
    }

    private void performSteadyAction() {
        // Mostly Just Viewing Jobs (90%), rarely applying (10%)
        User user = recruitmentService.getRandomUser();
        Job job = recruitmentService.getRandomJob();

        if (Math.random() > 0.9) {
            recruitmentService.viewJob(user.id(), job.id());
            recruitmentService.applyForJob(user.id(), job.id());
        } else {
            recruitmentService.viewJob(user.id(), job.id());
        }
    }

    private void performPeakAction() {
        // Deadline rush! 50% Apply, 50% View. High chance of errors simulated in
        // service.
        User user = recruitmentService.getRandomUser();
        Job job = recruitmentService.getRandomJob();

        if (Math.random() > 0.5) {
            recruitmentService.viewJob(user.id(), job.id());
            recruitmentService.applyForJob(user.id(), job.id());
        } else {
            recruitmentService.viewJob(user.id(), job.id());
        }
    }

    // Cleanup on destroy if needed
    public void shutdown() {
        executorService.shutdownNow();
    }
}
