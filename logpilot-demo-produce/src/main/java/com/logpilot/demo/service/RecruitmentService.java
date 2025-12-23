package com.logpilot.demo.service;

import com.logpilot.demo.domain.Job;
import com.logpilot.demo.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RecruitmentService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentService.class);
    private final List<Job> jobs = new CopyOnWriteArrayList<>();
    private final List<User> users = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    public RecruitmentService() {
        // 더미 데이터를 초기화합니다.
        // Initialize dummy data.
        jobs.add(new Job("JOB-001", "Backend Engineer", "TechCorp", "Seoul"));
        jobs.add(new Job("JOB-002", "Frontend Developer", "WebSolutions", "Busan"));
        jobs.add(new Job("JOB-003", "DevOps Engineer", "CloudSys", "Remote"));
        jobs.add(new Job("JOB-004", "Data Scientist", "DataAI", "Seoul"));
        jobs.add(new Job("JOB-005", "Product Manager", "InnoSoft", "Pangyo"));

        users.add(new User("USER-001", "Alice", "alice@example.com", "Junior dev"));
        users.add(new User("USER-002", "Bob", "bob@example.com", "Senior dev"));
        users.add(new User("USER-003", "Charlie", "charlie@example.com", "Student"));
    }

    public void viewJob(String userId, String jobId) {
        Job job = findJob(jobId);
        if (job != null) {
            try (var mdc = MDC.putCloseable("userId", userId)) {
                MDC.put("jobId", jobId);
                MDC.put("action", "VIEW_JOB");
                log.info("User {} viewed job {}", userId, jobId);

                // 가끔 발생하는 처리 지연을 시뮬레이션합니다.
                // Simulate occasional random processing delay.
                simulateLatency();
            }
        } else {
            log.error("Job not found: {}", jobId);
        }
    }

    public void applyForJob(String userId, String jobId) {
        Job job = findJob(jobId);
        User user = findUser(userId);

        if (job != null && user != null) {
            try (var mdc = MDC.putCloseable("userId", userId)) {
                MDC.put("jobId", jobId);
                MDC.put("action", "APPLY_JOB");

                // 비즈니스 로직(지원 처리)을 시뮬레이션합니다.
                // Simulate business logic (Processing application).
                boolean success = processApplication(user, job);

                if (success) {
                    log.info("User {} applied for job {} at {}", userId, job.title(), job.company());
                } else {
                    log.warn("Application failed for User {} on Job {}: Duplicate application", userId, jobId);
                }
            }
        } else {
            log.error("Invalid application request: User={}, Job={}", userId, jobId);
        }
    }

    private boolean processApplication(User user, Job job) {
        // 처리 시간을 시뮬레이션합니다.
        // Simulate processing time.
        simulateLatency();

        // NOTE: 랜덤 시스템 에러(예: DB 타임아웃)를 1% 확률로 발생시킵니다.
        // Simulate Random System Error (e.g. DB Timeout) with 1% chance.
        if (random.nextInt(100) == 0) {
            log.error("System Error: Database connection timed out while processing application for User {}",
                    user.id());
            return false;
        }

        // 10% 확률로 실패(예: 이미 지원함)를 시뮬레이션합니다.
        // Simulate 10% chance of failure (e.g., already applied).
        return random.nextInt(10) > 0;
    }

    private void simulateLatency() {
        try {
            // 10ms에서 200ms 사이의 랜덤 지연을 발생시킵니다.
            // Generate random latency between 10ms and 200ms.
            Thread.sleep(10 + random.nextInt(190));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public List<Job> getAllJobs() {
        return jobs;
    }

    public List<User> getAllUsers() {
        return users;
    }

    public User getRandomUser() {
        return users.get(random.nextInt(users.size()));
    }

    public Job getRandomJob() {
        return jobs.get(random.nextInt(jobs.size()));
    }

    private Job findJob(String jobId) {
        return jobs.stream().filter(j -> j.id().equals(jobId)).findFirst().orElse(null);
    }

    private User findUser(String userId) {
        return users.stream().filter(u -> u.id().equals(userId)).findFirst().orElse(null);
    }
}
