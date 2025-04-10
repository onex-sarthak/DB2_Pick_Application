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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableScheduling
@EnableAsync
@Slf4j
public class ThreadPoolConfig {


    @Value("${thread.pool.core.size:3}")
    private int corePoolSize;

    @Value("${thread.pool.max.size:5}")
    private int maxPoolSize;

    @Value("${thread.pool.keep.alive.seconds:60}")
    private int keepAliveSeconds;

    @Value("${thread.pool.queue.capacity:100}")
    private int queueCapacity;

    private final AtomicInteger threadCounter = new AtomicInteger(0);

    @Bean(name = "messageProcessorExecutor")
    public ThreadPoolExecutor messageProcessorExecutor() {
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                r -> new Thread(r, "message-processor-" + threadCounter.getAndIncrement()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "messagePollingExecutor")
    public ExecutorService messagePollingExecutor() {
        return Executors.newSingleThreadExecutor(r -> new Thread(r, "db-poller"));
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
