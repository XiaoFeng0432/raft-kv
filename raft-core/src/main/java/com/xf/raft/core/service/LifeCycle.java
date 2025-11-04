package com.xf.raft.core.service;

/**
 * 生命周期管理接口
 * 负责启动、销毁 Raft 组件
 */
public interface LifeCycle {
    void init() throws Throwable;
    void destroy() throws Throwable;
}
