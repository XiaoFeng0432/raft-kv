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
            // 没有投票/正好投票给对方 -> 比较日志大小
            if(node.getVotedFor() == null || node.getVotedFor().equals(param.getCandidateId())){
                // 对方的日志至少和自己一样新 才会投票给对方
                if(!isLogUpToDate(param.getLastLogTerm(), param.getLastLogIndex())){
                    log.info("拒绝投票给 {}: 候选人日志不够新", param.getCandidateId());
                    return new VoteResult(node.getCurrentTerm(), false);
                }

                // 可以给对方投票，节点变为 FOLLOWER
                log.info("节点 {} 投票给 {}", node.getPeerSet().getSelf().getAddr(), param.getCandidateId());
                node.setStatus(NodeStatus.FOLLOWER);
                node.setVotedFor(param.getCandidateId());

                // TODO setLeader 什么时候设置？
//                node.getPeerSet().setLeader(new Peer(param.getCandidateId()));
//                node.setCurrentTerm(param.getTerm());

                return new VoteResult(node.getCurrentTerm(), true);
            }

            return new VoteResult(node.getCurrentTerm(), false);
        } finally {
            voteLock.unlock();
        }
    }

    /**
     * 判断候选人的日志是否至少和本节点一样新
     * @param candidateLastLogTerm 候选人最后日志的任期
     * @param candidateLastLogIndex 候选人最后日志的索引
     * @return true 表示候选人日志至少和本节点一样新
     */
    private boolean isLogUpToDate(long candidateLastLogTerm, long candidateLastLogIndex) {
        LogEntry lastEntry = node.getLogModule().getLastEntry();

        // 本节点没有日志，候选人的日志一定够新
        if(lastEntry == null){
            return true;
        }

        long myLastTerm = lastEntry.getTerm();
        long myLastIndex = lastEntry.getIndex();

        // 1. 先比较任期号：任期号大的更新
        if(candidateLastLogTerm != myLastTerm){
            return candidateLastLogTerm > myLastTerm;
        }

        // 2. 任期号相同，比较索引：索引大的更新
        return candidateLastLogIndex >= myLastIndex;
    }

    /**
     * 附加日志 RPC
     * 如果日志为空就是心跳信息，反之为日志信息
     *
     * 接收者实现：
     * 1. 如果 term < currentTerm 就返回 false
     * 2. 如果日志在 prevLogIndex 位置处的日志条目的任期号和 prevLogTerm 不匹配，则返回 false
     * 3. 如果已经存在的日志条目和新的产生冲突（索引值相同但是任期号不同），删除这一条和之后所有的
     * 4. 附加任何在已有的日志中不存在的条目
     * 5. 如果 leaderCommit > commitIndex，令 commitIndex 等于 leaderCommit 和 新日志条目索引值 中较小的一个
     */
    @Override
    public AppendEntryResult appendEntries(AppendEntryParam param) {
        AppendEntryResult result = new AppendEntryResult();
        result.setSuccess(false);

        try{
            if(!appendLock.tryLock()){
                return result;
            }

            // 无论成功失败先设置返回值，也就是将自己的 term 返回给 Leader
            result.setTerm(node.getCurrentTerm());

            // 如果 term < currentTerm 就返回 false
            if(param.getTerm() < node.getCurrentTerm()){
                return result;
            }

            // 更新心跳和选举时间
            node.setPreHeartbeatTime(System.currentTimeMillis());
            node.setPreElectionTime(System.currentTimeMillis());
            node.getPeerSet().setLeader(new Peer(param.getLeaderId()));

            // 如果对方任期更大，则更新自己任期并变成 FOLLOWER
            if(param.getTerm() >= node.getCurrentTerm()){
                log.debug("节点 {} 转化为 {} 的 FOLLOWER", node.getPeerSet().getSelf().getAddr(), param.getLeaderId());
                node.setCurrentTerm(param.getTerm());
                node.setStatus(NodeStatus.FOLLOWER);
            }

            // 如果是心跳信息
            if(param.getEntries() == null || param.getEntries().isEmpty()){
                log.debug("节点 {} 接收到 {} 的心跳信息", node.getPeerSet().getSelf().getAddr(), param.getLeaderId());

                // 处理 Leader 已提交但是没有应用到状态机的日志

                // 如果 leaderCommit > commitIndex, 令 commitIndex 等于 leaderCommit 和新日志条目索引值中较小的一个
                if(param.getLeaderCommit() > node.getCommitIndex()){
                    // leader 中的日志信息比本地 node 的日志新
                    int commitIndex = (int) Math.min(node.getLogModule().getLastIndex(), param.getLeaderCommit());
                    node.setCommitIndex(commitIndex);
                    node.setLastApplied(commitIndex);
                }

                // 下一个需要提交的日志的索引（如有）
                long nextCommit = node.getCommitIndex() + 1;

                // 应用已提交但是未应用的日志
                while(nextCommit <= node.getCommitIndex()){
                    node.getStateMachine().apply(node.getLogModule().read(nextCommit));
                    nextCommit++;
                }

                result.setSuccess(true);
                result.setTerm(node.getCurrentTerm());
                return result;
            }

            // 如果是日志信息, 进行日志复制
            // 检查日志一致性
            if(node.getLogModule().getLastIndex() != 0 && param.getPrevLogIndex() != 0){
                LogEntry entry = node.getLogModule().read(param.getPrevLogIndex());

                if(entry != null){
                    // 如果日志在 prevLogIndex 处日志的任期号和 prevLogTerm 不匹配，则返回 false
                    if(entry.getTerm() != param.getPrevLogTerm()){
                        log.warn("日志不一致: prevLogIndex={}, 本地term={}, Leader处term={}",
                                param.getPrevLogIndex(), entry.getTerm(), param.getPrevLogTerm());
                        return result;
                    }
                }
                else{
                    // index不对, 需要减小 nextIndex 重试
                    log.warn("日志不一致: prevLogIndex={} 处没有日志", param.getPrevLogIndex());
                    return result;
                }
            }

            // 到这里 prevLogIndex 之前的日志保持一致，但是本地 node 可能有多的日志, 可能
            // 如果已存在的日志条目 和 新的 产生冲突, 删除这一条和之后所有的
            LogEntry existLog = node.getLogModule().read(param.getPrevLogIndex() + 1);
            if(existLog != null && existLog.getTerm() != param.getEntries().get(0).getTerm()){
                log.warn("删除冲突日志: startIndex={}", param.getPrevLogIndex() + 1);
                node.getLogModule().removeOnStartIndex(param.getPrevLogIndex() + 1);
            }
            else if(existLog != null){
                // 日志一致，已存在
                result.setSuccess(true);
                return result;
            }

            // 附加新日志
            for(LogEntry entry : param.getEntries()){
                node.getLogModule().write(entry);
                log.debug("写入日志成功: term={}, index={}", entry.getTerm(), entry.getIndex());
            }
            result.setSuccess(true);

            // 更新 commitIndex 并应用日志
            if(param.getLeaderCommit() > node.getCommitIndex()){
                int commitIndex = (int) Math.min(node.getLogModule().getLastIndex(), param.getLeaderCommit());
                node.setCommitIndex(commitIndex);
                node.setLastApplied(commitIndex);
            }

            // 应用已提交但是未应用的日志
            long nextCommit = node.getCommitIndex() + 1;
            while(nextCommit <= node.getLogModule().getLastIndex()){
                node.getStateMachine().apply(node.getLogModule().read(nextCommit));
                nextCommit++;
            }

            result.setTerm(node.getCurrentTerm());
            result.setSuccess(true);

            return result;
        }finally {
            appendLock.unlock();
        }
    }
}
