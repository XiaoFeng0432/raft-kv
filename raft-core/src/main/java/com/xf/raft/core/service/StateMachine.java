package com.xf.raft.core.service;

import com.xf.raft.core.entity.LogEntry;

/**
 * 状态机接口
 * 将提交的日志应用到系统状态
 */
public interface StateMachine {
    // 将日志应用到状态机
    void apply(LogEntry entry);

    // 获取日志中的值
    LogEntry get(String key);

    // 简化的 KV 操作
    String getString(String key);
    void setString(String key, String value);
    void delString(String... keys);
}
