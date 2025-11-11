package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求投票结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteResult {
    // 当前节点的任期号
    private long term;
    // 是否同意投票
    private boolean voteGranted;
}
