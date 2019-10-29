package com.chineseall.epubparser.lib.downloader;

import com.chineseall.epubparser.lib.util.LogUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 可扩容的线程池
 */
public class AutoAdjustThreadPool {
    private static volatile AutoAdjustThreadPool instance;
    private static final int MAX_QUEUE_SIZE = 3;
    private static final int PER_ADD_THREAD = 9;
    private static final Long MONITOR_DELAY_TIME = 1L;

    private ScheduledExecutorService scheduledExecutorService = null;
    private ThreadPoolExecutor executor = null;

    private boolean isInit = false;

    private AutoAdjustThreadPool() {

    }

    public static AutoAdjustThreadPool get() {
        if (instance == null) {
            synchronized (AutoAdjustThreadPool.class) {
                if (instance == null) {
                    instance = new AutoAdjustThreadPool();
                }
            }
        }
        return instance;
    }

    private void start() {
        executor = new ThreadPoolExecutor(
                3,
                Integer.MAX_VALUE,
                60L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue()
        );
        scheduledExecutorService = new ScheduledThreadPoolExecutor(3, Executors.defaultThreadFactory());
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                //当队列大小超过限制，且jvm内存使用率小于80%时扩容，防止无限制扩容
                if (executor.getQueue().size() >= MAX_QUEUE_SIZE && executor.getPoolSize() < executor.getMaximumPoolSize() && getMemoryUsage() < 0.8f) {
                    executor.setCorePoolSize(executor.getPoolSize() + PER_ADD_THREAD);
                }
                //当队列大小小于限制的80%，线程池缩容
                if (executor.getPoolSize() > 0 && executor.getQueue().size() < MAX_QUEUE_SIZE * 0.8) {
                    executor.setCorePoolSize(executor.getPoolSize() - PER_ADD_THREAD);
                }
            }
        }, MONITOR_DELAY_TIME, MONITOR_DELAY_TIME, TimeUnit.SECONDS);
        isInit = true;
    }

    /**
     * 获取jvm内存使用率
     *
     * @return
     */
    private float getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) * 1.0f / runtime.maxMemory();
    }

    public void stop() {
        if (isInit) {
            executor.shutdown();
            try {
                while (!(executor.awaitTermination(1, TimeUnit.SECONDS))) {
                }
            } catch (Exception e) {

            }
            scheduledExecutorService.shutdown();
            executor = null;
            scheduledExecutorService = null;
            isInit = false;
        }
    }

    public Future<?> execute(Runnable task) {
        if (!isInit) {
            start();
        }
        if (remove(task)) {
            LogUtil.d("task has been removed");
        }
        return executor.submit(task);
    }

    public boolean remove(Runnable task) {
        boolean res = false;
        if (executor != null) {
            res = executor.remove(task);
        }
        if (res) {
            LogUtil.d("task has been removed");
        }
        return false;
    }
}
