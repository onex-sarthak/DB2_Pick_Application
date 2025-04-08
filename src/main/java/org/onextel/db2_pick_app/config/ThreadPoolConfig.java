package org.onextel.db2_pick_app.config;


import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
@EnableAsync
@Slf4j
public class ThreadPoolConfig {

    
    @Bean(name = "messageProcessorExecutor")
    public ThreadPoolTaskExecutor messageProcessorExecutor(
            @Value("${thread.pool.core.size:2}") int corePoolSize,
            @Value("${thread.pool.max.size:2}") int maxPoolSize,
            @Value("${thread.pool.queue.capacity:100}") int queueCapacity) {
        return createThreadPool(corePoolSize, maxPoolSize, queueCapacity, "MessageProcessor-");
    }

    @Bean(name = "dlrCallbackExecutor")
    public ThreadPoolTaskExecutor dlrCallbackExecutor(
            @Value("${dlr.thread.pool.core.size:10}") int corePoolSize,
            @Value("${dlr.thread.pool.max.size:20}") int maxPoolSize,
            @Value("${dlr.thread.pool.queue.capacity:100}") int queueCapacity) {
        return createThreadPool(corePoolSize, maxPoolSize, queueCapacity, "DLRCallback-");
    }



    private ThreadPoolTaskExecutor createThreadPool(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }



}
