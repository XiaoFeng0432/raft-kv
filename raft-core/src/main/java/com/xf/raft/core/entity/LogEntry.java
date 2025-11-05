package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Raft 日志条目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry implements Serializable, Comparable<LogEntry>{
    // 当前日志的任期号
    private long term;
    // 日志在整个日志序列的下标
    private long index;
    // 实际执行的命令
    private Command command;

    @Override
    public int compareTo(LogEntry o) {
        if(o == null) return -1;
        if(this.getIndex() > o.getIndex()) return 1;
        return -1;
    }
}
