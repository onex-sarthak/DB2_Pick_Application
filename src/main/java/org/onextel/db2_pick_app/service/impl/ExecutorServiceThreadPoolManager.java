package org.onextel.db2_pick_app.service.impl;


import org.onextel.db2_pick_app.service.interfaces.ThreadPoolManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
public class ExecutorServiceThreadPoolManager implements ThreadPoolManager {
    private final ThreadPoolExecutor executor;

    @Autowired
    public ExecutorServiceThreadPoolManager(
            @Value("${thread.pool.core.size:5}") int corePoolSize,
            @Value("${thread.pool.max.size:10}") int maxPoolSize,
            @Value("${thread.pool.keep.alive.seconds:60}") int keepAliveSeconds,
            @Value("${thread.pool.queue.capacity:100}") int queueCapacity) {
        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                r -> new Thread(r, "message-processor-" + Thread.currentThread().getId()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Override
    public void submitTask(Runnable task) {
        executor.submit(task);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}