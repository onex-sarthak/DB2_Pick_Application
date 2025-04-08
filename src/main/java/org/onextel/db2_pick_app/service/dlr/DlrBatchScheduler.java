package org.onextel.db2_pick_app.service.dlr;

import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class DlrBatchScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> future;

    public void schedule(Runnable task, long timeoutMilliSeconds) {
        cancel();
        future = scheduler.schedule(task, timeoutMilliSeconds, TimeUnit.MILLISECONDS);
    }

    public void cancel() {
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
        }
    }
}
