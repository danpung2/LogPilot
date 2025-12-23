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
        STEADY, // 낮은 트래픽: 조회 위주 / Low traffic: Job views mostly
        PEAK // 높은 트래픽: 잦은 지원 및 에러, 높은 동시성 / High traffic: Many applications, errors, high
             // concurrency
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
        // 5개의 워커 스레드로 동시 사용자를 시뮬레이션합니다.
        // Simulate concurrent users with 5 worker threads.
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
                    // 0.5초 - 1.5초 지연
                    // 0.5s - 1.5s delay
                    Thread.sleep(500 + (long) (Math.random() * 1000));
                } else if (mode == SimulationMode.PEAK) {
                    performPeakAction();
                    // 0.05초 - 0.15초 지연 (빠름)
                    // 0.05s - 0.15s delay (FAST)
                    Thread.sleep(50 + (long) (Math.random() * 100));
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
        // 대부분 조회(90%)이며, 가끔 지원(10%)합니다.
        // Mostly Just Viewing Jobs (90%), rarely applying (10%).
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
        // 마감 임박으로 50% 지원, 50% 조회. 서비스에서 에러 발생 확률이 높습니다.
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

    // 필요 시 정리합니다.
    // Cleanup on destroy if needed.
    public void shutdown() {
        executorService.shutdownNow();
    }
}
