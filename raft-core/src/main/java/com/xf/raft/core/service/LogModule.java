package com.xf.raft.core.service;

import com.xf.raft.core.entity.LogEntry;

/**
 * 日志模块
 * 负责日志的写入、读取和删除
 */
public interface LogModule extends LifeCycle{
    // 写入日志
    void write(LogEntry entry);

    // 根据索引读取日志
    LogEntry read(Long index);

    // 删除从 startIndex 开始的所有日志
    void removeOnStartIndex(Long startIndex);

    // 得到最后一条日志
    LogEntry getLastEntry();


    // 获取最后一条日志的索引
    Long getLastIndex();
}

