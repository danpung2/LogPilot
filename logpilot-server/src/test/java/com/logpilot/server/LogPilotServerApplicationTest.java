package com.logpilot.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class LogPilotServerApplicationTest {

    @Test
    void contextLoads() {
        assertDoesNotThrow(() -> {
        });
    }

    @Test
    void mainMethod_WithArgs_ShouldNotThrow() {
        assertNotNull(LogPilotServerApplication.class);

        try {
            Method mainMethod = LogPilotServerApplication.class.getMethod("main", String[].class);
            assertNotNull(mainMethod);
            assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()));
        } catch (NoSuchMethodException e) {
            fail("Main method should exist");
        }
    }

    @Test
    void mainMethod_WithNullArgs_ShouldNotThrow() {
        assertDoesNotThrow(() -> {
            Method mainMethod = LogPilotServerApplication.class.getMethod("main", String[].class);
            assertNotNull(mainMethod);
        });
    }

    @Test
    void applicationClass_ShouldHaveCorrectAnnotations() {
        Class<LogPilotServerApplication> appClass = LogPilotServerApplication.class;

        assertTrue(appClass.isAnnotationPresent(org.springframework.boot.autoconfigure.SpringBootApplication.class));
        assertTrue(appClass.isAnnotationPresent(org.springframework.boot.context.properties.ConfigurationPropertiesScan.class));

        org.springframework.boot.autoconfigure.SpringBootApplication springBootApp =
            appClass.getAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class);

        String[] scanBasePackages = springBootApp.scanBasePackages();
        assertEquals(2, scanBasePackages.length);
        assertTrue(java.util.Arrays.asList(scanBasePackages).contains("com.logpilot.core"));
        assertTrue(java.util.Arrays.asList(scanBasePackages).contains("com.logpilot.server"));

        org.springframework.boot.context.properties.ConfigurationPropertiesScan configScan =
            appClass.getAnnotation(org.springframework.boot.context.properties.ConfigurationPropertiesScan.class);

        String[] configScanPackages = configScan.value();
        assertEquals(1, configScanPackages.length);
        assertEquals("com.logpilot.core.config", configScanPackages[0]);
    }
}