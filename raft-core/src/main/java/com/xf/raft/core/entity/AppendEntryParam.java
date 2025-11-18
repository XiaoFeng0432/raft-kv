package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 附加日志 RPC 的参数
 * Leader 用来复制日志给其他节点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppendEntryParam {

    private long term;
    // 被请求者id
    private String serverId;
    // Leader ID
    private String leaderId;
    // 新日志之前一条日志的索引
    private long prevLogIndex;
    // 新日志之前一条日志的任期号
    private long prevLogTerm;
    // 要复制的日志条目（为空表示心跳）
    private List<LogEntry> entries;
    // Leader已经提交的最高日志索引
    private long leaderCommit;
}
