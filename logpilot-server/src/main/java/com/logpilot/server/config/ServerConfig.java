package com.logpilot.server.config;

import com.logpilot.core.config.LogPilotProperties;
import com.logpilot.core.config.LogStorageFactory;
import com.logpilot.core.storage.LogStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LogPilotProperties.class)
public class ServerConfig {

    @Bean(destroyMethod = "close")
    public LogStorage logStorage(LogPilotProperties properties) {
        return LogStorageFactory.createLogStorage(properties);
    }
}