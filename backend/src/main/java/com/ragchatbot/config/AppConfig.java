package com.ragchatbot.config;

import java.time.Duration;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

@Configuration
@EnableAsync
public class AppConfig implements AsyncConfigurer {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean(name = "applicationTaskExecutor")
    public TaskExecutor taskExecutor() {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);

        executor.setThreadNamePrefix("rag-async-");

        executor.initialize();

        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }
}