package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 请求投票RPC的参数
 * 如果节点发起选举会广播这个请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteParam implements Serializable {
    private long term;
    // 候选者id
    private String candidateId;
    // 候选者最后一次日志的索引
    private String lastLogIndex;
    // 候选者最后一次日志的任期号
    private String lastLogTerm;
}
