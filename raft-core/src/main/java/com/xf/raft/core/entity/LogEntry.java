package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Raft 日志条目
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry implements Serializable {
    // 当前日志的任期号
    private long term;
    // 日志在整个日志序列的下标
    private long index;
    // 实际执行的命令
    private String command;
}
