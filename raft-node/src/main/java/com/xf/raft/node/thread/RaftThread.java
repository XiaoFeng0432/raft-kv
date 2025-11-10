package com.xf.raft.node.thread;

import lombok.extern.slf4j.Slf4j;

/**
 * Raft 专用线程
 * 统一处理未捕获异常
 */
@Slf4j
public class RaftThread extends Thread{

    private static final UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER =
            (t, e) -> log.warn("线程: " + t.getName() + " 发生未捕获异常: " + e.getMessage());

    public RaftThread(String threadName, Runnable r){
        super(r, threadName);
        setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
    }
}
