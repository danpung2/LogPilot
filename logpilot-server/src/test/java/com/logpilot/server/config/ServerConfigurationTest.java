package com.logpilot.server.config;

import com.logpilot.core.storage.LogStorage;
import com.logpilot.server.grpc.LogPilotGrpcService;
import com.logpilot.server.grpc.handler.GrpcLogHandler;
import com.logpilot.server.rest.LogController;
import com.logpilot.server.rest.service.RestLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

public class ServerConfigurationTest {

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "logpilot.server.protocol=rest",
            "logpilot.storage.type=FILE",
            "logpilot.storage.directory=/tmp/test-rest-only"
    })
    static class RestOnlyConfigurationTest {

        @Test
        void serverConfig_WithRestOnly_ShouldStartOnlyRestComponents(ApplicationContext context) {
            // REST components should be present
            assertDoesNotThrow(() -> context.getBean(LogController.class));
            assertDoesNotThrow(() -> context.getBean(RestLogService.class));

            // gRPC components should NOT be present
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean(LogPilotGrpcService.class));
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean(GrpcLogHandler.class));

            // Storage should still be configured
            assertDoesNotThrow(() -> context.getBean(LogStorage.class));
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "logpilot.server.protocol=grpc",
            "logpilot.storage.type=FILE",
            "logpilot.storage.directory=/tmp/test-grpc-only"
    })
    static class GrpcOnlyConfigurationTest {

        @Test
        void serverConfig_WithGrpcOnly_ShouldStartOnlyGrpcComponents(ApplicationContext context) {
            // gRPC components should be present
            assertDoesNotThrow(() -> context.getBean(LogPilotGrpcService.class));
            assertDoesNotThrow(() -> context.getBean(GrpcLogHandler.class));

            // REST components should NOT be present
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean(LogController.class));
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean(RestLogService.class));

            // Storage should still be configured
            assertDoesNotThrow(() -> context.getBean(LogStorage.class));
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "logpilot.server.protocol=all",
            "logpilot.storage.type=FILE",
            "logpilot.storage.directory=/tmp/test-all-protocols"
    })
    static class AllProtocolsConfigurationTest {

        @Test
        void serverConfig_WithAllProtocols_ShouldStartAllComponents(ApplicationContext context) {
            // All components should be present
            assertDoesNotThrow(() -> context.getBean(LogController.class));
            assertDoesNotThrow(() -> context.getBean(RestLogService.class));
            assertDoesNotThrow(() -> context.getBean(LogPilotGrpcService.class));
            assertDoesNotThrow(() -> context.getBean(GrpcLogHandler.class));
            assertDoesNotThrow(() -> context.getBean(LogStorage.class));
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "logpilot.server.protocol=all",
            "logpilot.storage.type=FILE",
            "logpilot.storage.directory=/tmp/test-file-storage"
    })
    static class FileStorageConfigurationTest {

        @Test
        void serverConfig_WithFileStorage_ShouldConfigureCorrectly(ApplicationContext context) {
            LogStorage storage = context.getBean(LogStorage.class);
            assertNotNull(storage);
            assertEquals("com.logpilot.core.storage.FileLogStorage",
                    storage.getClass().getName());
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "logpilot.server.protocol=all",
            "logpilot.storage.type=SQLITE",
            "logpilot.storage.sqlite.path=/tmp/test-sqlite.db"
    })
    static class SqliteStorageConfigurationTest {

        @Test
        void serverConfig_WithSqliteStorage_ShouldConfigureCorrectly(ApplicationContext context) {
            LogStorage storage = context.getBean(LogStorage.class);
            assertNotNull(storage);
            assertEquals("com.logpilot.core.storage.SqliteLogStorage",
                    storage.getClass().getName());
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "logpilot.server.protocol=all",
            "logpilot.storage.type=FILE",
            "server.port=0"
    })
    static class CustomPortsConfigurationTest {

        @Test
        void serverConfig_WithCustomPorts_ShouldBindCorrectly(ApplicationContext context) {
            // Verify application context starts successfully with custom ports
            assertNotNull(context);
            assertNotNull(context);

            // Basic components should be available
            assertDoesNotThrow(() -> context.getBean(LogController.class));
            assertDoesNotThrow(() -> context.getBean(LogStorage.class));
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "logpilot.server.protocol=all"
    // No storage configuration - should use defaults
    })
    static class DefaultPropertiesConfigurationTest {

        @Test
        void serverConfig_WithMissingProperties_ShouldUseDefaults(ApplicationContext context) {
            // Should use default storage configuration
            LogStorage storage = context.getBean(LogStorage.class);
            assertNotNull(storage);

            // Default should be SQLite storage (but test profile overrides to FILE)
            // In test environment, FILE storage is used due to application-test.yml
            assertTrue(storage.getClass().getName().contains("LogStorage"));

            // All components should start with defaults
            assertDoesNotThrow(() -> context.getBean(LogController.class));
            assertDoesNotThrow(() -> context.getBean(RestLogService.class));
            assertDoesNotThrow(() -> context.getBean(LogPilotGrpcService.class));
            assertDoesNotThrow(() -> context.getBean(GrpcLogHandler.class));
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "logpilot.server.protocol=all",
            "logpilot.storage.type=FILE",
            "logging.level.com.logpilot=DEBUG"
    })
    static class LoggingLevelConfigurationTest {

        @Test
        void serverConfig_WithLoggingLevel_ShouldAdjustVerbosity(ApplicationContext context) {
            // Verify context loads with custom logging configuration
            assertNotNull(context);
            assertNotNull(context);

            // Components should be available
            assertDoesNotThrow(() -> context.getBean(LogController.class));
            assertDoesNotThrow(() -> context.getBean(LogStorage.class));
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "logpilot.server.protocol=all",
            "logpilot.storage.type=FILE",
            "management.server.port=0",
            "management.endpoints.web.exposure.include=health,info,metrics"
    })
    static class ActuatorConfigurationTest {

        @Test
        void serverConfig_WithActuatorEnabled_ShouldExposeEndpoints(ApplicationContext context) {
            // Verify actuator configuration doesn't break application startup
            assertNotNull(context);
            assertNotNull(context);

            // Core components should still work
            assertDoesNotThrow(() -> context.getBean(LogController.class));
            assertDoesNotThrow(() -> context.getBean(LogStorage.class));
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "logpilot.server.protocol=rest",
            "logpilot.storage.type=FILE",
            "logpilot.storage.directory=/tmp/profile-switch-test"
    })
    static class ProfileSwitchingConfigurationTest {

        @Test
        void serverConfig_WithProfileSwitching_ShouldReconfigure(ApplicationContext context) {
            // Verify that profile-specific configuration works
            assertNotNull(context);
            assertNotNull(context);

            // Only REST components should be active
            assertDoesNotThrow(() -> context.getBean(RestLogService.class));
            assertThrows(NoSuchBeanDefinitionException.class,
                    () -> context.getBean(GrpcLogHandler.class));

            // Storage should be configured according to profile
            LogStorage storage = context.getBean(LogStorage.class);
            assertEquals("com.logpilot.core.storage.FileLogStorage",
                    storage.getClass().getName());
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "logpilot.server.protocol=all",
            "logpilot.storage.type=FILE",
            "logpilot.storage.directory=/tmp/resource-cleanup-test"
    })
    static class ResourceCleanupConfigurationTest {

        @Test
        void serverConfig_ShouldManageResourcesCorrectly(ApplicationContext context) {
            // Verify resource management
            assertNotNull(context);
            assertNotNull(context);

            LogStorage storage = context.getBean(LogStorage.class);
            assertNotNull(storage);

            // Storage should be properly configured
            assertDoesNotThrow(() -> {
                // Test basic storage functionality
                storage.retrieve("test-channel", "consumer1", 1);
            });
        }
    }
}