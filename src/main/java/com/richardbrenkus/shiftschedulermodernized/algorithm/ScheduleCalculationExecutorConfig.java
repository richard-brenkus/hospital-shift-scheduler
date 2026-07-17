package com.richardbrenkus.shiftschedulermodernized.algorithm;

import com.richardbrenkus.shiftschedulermodernized.config.ScheduleCalculationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ScheduleCalculationExecutorConfig {

    @Bean(name = "scheduleCalculationExecutor")
    public Executor scheduleCalculationExecutor(ScheduleCalculationProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(properties.numberOfThreads());
        executor.setMaxPoolSize(properties.numberOfThreads());
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("schedule-calc-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }
}