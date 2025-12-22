package com.logpilot.spring;

import com.logpilot.client.LogPilotClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(LogPilotClient.class)
@EnableConfigurationProperties(LogPilotClientProperties.class)
public class LogPilotClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogPilotClient logPilotClient(LogPilotClientProperties properties) {
        return LogPilotClient.builder()
                .serverUrl(properties.getServerUrl())
                .enableBatching(properties.isEnableBatching())
                .batchSize(properties.getBatchSize())
                .flushIntervalMillis(properties.getFlushIntervalMillis())
                .build();
    }
}
