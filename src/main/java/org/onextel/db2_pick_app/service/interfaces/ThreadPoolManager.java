package org.onextel.db2_pick_app.service.interfaces;

public interface ThreadPoolManager {
    void submitTask(Runnable task);
    void shutdown();
}