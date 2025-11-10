package com.xf.raft.node.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * Raft 线程池执行器
 * 扩展 threadPoolExecutor，添加监控和日志
 */
@Slf4j
public class RaftThreadPoolExecutor extends ThreadPoolExecutor {

    // 用来监控耗时
    private static final ThreadLocal<Long> COST_TIME_WATCH = ThreadLocal.withInitial(System::currentTimeMillis);

    public RaftThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            RaftThreadPool.NameThreadFactory nameThreadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, nameThreadFactory);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        COST_TIME_WATCH.get();
        log.debug("Raft 线程池开始执行任务");
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        log.debug("Raft 线程池执行完成，耗时: {}ms", System.currentTimeMillis() - COST_TIME_WATCH.get());
        COST_TIME_WATCH.remove();
    }

    // 确定线程池彻底关闭
    @Override
    protected void terminated() {
        log.info("Raft 线程池关闭 - activeCount: {}, queueSize: {}, poolSize:{}",
                getActiveCount(), getQueue().size(), getPoolSize());
    }
}
