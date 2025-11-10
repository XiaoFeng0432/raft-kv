package com.xf.raft.node.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * Raft 线程池
 */
@Slf4j
public class RaftThreadPool {
    private static final int CPU = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = CPU * 2;
    private static final int QUEUE_SIZE = 1024;
    private static final long KEEP_TIME = 1000 * 60;
    private static final TimeUnit KEEP_TIME_UNIT = TimeUnit.MILLISECONDS;

    // 定时任务线程池
    private static final ScheduledExecutorService scheduledExecutor = getScheduled();
    // 普通任务线程池
    private static final ThreadPoolExecutor threadPoolExecutor = getThreadPool();

    /**
     * 获取定时任务线程池
     */
    private static ScheduledExecutorService getScheduled(){
        return new ScheduledThreadPoolExecutor(CPU, new NameThreadFactory());
    }

    /**
     * 获取普通任务线程池
     */
    private static ThreadPoolExecutor getThreadPool(){
        return new RaftThreadPoolExecutor(
                CPU,
                MAX_POOL_SIZE,
                KEEP_TIME,
                KEEP_TIME_UNIT,
                new LinkedBlockingQueue<>(QUEUE_SIZE),
                new NameThreadFactory()
        );
    }

    /**
     * 固定频率执行定时任务
     * 从开始时间计时 - 比如固定一分钟执行n次
     * @param r 任务
     * @param initialDelay 初始延迟
     * @param delay 执行间隔
     */
    public static void scheduleAtFixedRate(Runnable r, long initialDelay, long delay){
        scheduledExecutor.scheduleAtFixedRate(r, initialDelay, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 固定延迟执行定时任务
     * 上一个任务结束后开始计时
     * @param r 任务
     * @param delay 延迟时间
     */
    public static void scheduleWithFixedDelay(Runnable r, long delay){
        scheduledExecutor.scheduleWithFixedDelay(r, 0, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 提交有返回值的任务
     */
    public static <T> Future<T> submit(Callable<T> t){
        return threadPoolExecutor.submit(t);
    }

    /**
     * 执行任务
     */
    public static void execute(Runnable r){
        threadPoolExecutor.execute(r);
    }

    /**
     * 执行任务（可选同步或异步）
     */
    public static void execute(Runnable r, boolean sync){
        if(sync){
            r.run();
        }
        else{
            threadPoolExecutor.execute(r);
        }
    }

    /**
     * 线程工厂
     */
    static class NameThreadFactory implements ThreadFactory{
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new RaftThread("Raft-Thread", r);
            t.setDaemon(true);
            t.setPriority(5);
            return t;
        }
    }

}
