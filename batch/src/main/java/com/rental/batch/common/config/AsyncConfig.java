package com.rental.batch.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 배치 트리거 비동기 실행 — fire-and-forget (ADR-014 §통신).
 *
 * <p>Controller 가 즉시 200 반환하고, 실제 작업은 별도 스레드에서 진행.
 * 진행 상황은 {@code BL_BATCH_LOG} 로 영속화 → backoffice 가 폴링 조회.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "batchTaskExecutor")
    public TaskExecutor batchTaskExecutor() {
        var ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("batch-runner-");
        ex.initialize();
        return ex;
    }
}
