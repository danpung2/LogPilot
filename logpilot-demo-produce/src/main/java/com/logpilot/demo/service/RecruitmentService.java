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
        // Initialize dummy data
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

                // Simulate occasional random processing delay
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

                // Simulate business logic
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
        // Simulate some processing
        simulateLatency();
        // 10% chance of failure (e.g., already applied)
        return random.nextInt(10) > 0;
    }

    private void simulateLatency() {
        try {
            // Random latency between 10ms and 200ms
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
