package com.xf.raft.node.impl;

import com.xf.raft.core.entity.*;
import com.xf.raft.core.service.Consensus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 默认一致性模块实现
 * 负责投票和日志复制的核心实现
 */
@Data
@Slf4j
public class DefaultConsensus implements Consensus {

    public final DefaultNode node;

    public final ReentrantLock voteLock = new ReentrantLock();
    public final ReentrantLock appendLock = new ReentrantLock();

    /**
     * 请求投票
     * 接收者实现：
     *   如果 term < currentTerm 就返回 false
     *   如果 votedFor 为空或者就是 candidateId，并且候选人的日志至少和自己一样新，那么就投票给他
     */
    @Override
    public VoteResult requestVote(VoteParam param) {
        try {
            // 尝试获取锁
            if (!voteLock.tryLock()) {
                return new VoteResult(node.getCurrentTerm(), false);
            }

            // 如果对方的任期不如自己新，不投票
            if(param.getTerm() < node.getCurrentTerm()){
                return new VoteResult(node.getCurrentTerm(), false);
            }

            log.info("节点 {} 收到投票请求: 候选人: {}, 候选人任期号: {}, 节点当前任期: {}, 已投票给: {}",
                    node.getPeerSet().getSelf().getAddr(),
                    param.getCandidateId(),
                    param.getTerm(),
                    node.getCurrentTerm(),
                    node.getVotedFor()
            );

            // 发现对方任期更高，更新自己任期
            if(param.getTerm() > node.getCurrentTerm()){
                node.setCurrentTerm(param.getTerm());
                // 状态切换为 FOLLOWER
                node.setStatus(NodeStatus.FOLLOWER);
                node.setVotedFor(null);
            }

            // 检查是否已经投票
            // 没有投票 或者 正好投票给对方 -> 比较日志大小
            if(node.getVotedFor() == null || node.getVotedFor().equals(param.getCandidateId())){
                // 对方的日志至少和自己一样新 才会投票给对方
                LogEntry lastEntry = node.getLogModule().getLastEntry();
                if(lastEntry != null){
                    // 对方日志任期号不如自己新
                    if(lastEntry.getTerm() > param.getLastLogTerm()){
                        return new VoteResult(node.getCurrentTerm(), false);
                    }
                    // 对方日志索引比自己小
                    if(lastEntry.getIndex() > param.getLastLogIndex()){
                        return new VoteResult(node.getCurrentTerm(), false);
                    }
                }
            }

            // 可以给对方投票，节点变为 FOLLOWER
            node.setStatus(NodeStatus.FOLLOWER);
            node.setVotedFor(param.getCandidateId());
            node.getPeerSet().setLeader(new Peer(param.getCandidateId()));
            node.setCurrentTerm(param.getTerm());

            return new VoteResult(node.getCurrentTerm(), false);
        } finally {
            voteLock.unlock();
        }
    }

    @Override
    public AppendEntryResult appendEntries(AppendEntryParam param) {
        // TODO 日志复制
        return null;
    }
}
