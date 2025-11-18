package com.xf.raft.node.timer;

import com.xf.raft.core.entity.*;
import com.xf.raft.node.impl.DefaultNode;
import com.xf.raft.node.thread.RaftThreadPool;
import com.xf.raft.rpc.protocol.Request;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 选举定时任务
 */
@Slf4j
public class ElectionTask implements Runnable{

    private final DefaultNode node;

    public ElectionTask(DefaultNode node) {
        this.node = node;
    }

    @Override
    public void run() {
        // 如果是 Leader 不需要选举
        if(node.getStatus() == NodeStatus.LEADER){
            return;
        }

        // 基于 Raft 的随机时间 解决冲突
        node.setElectionTime(node.getElectionTime() + ThreadLocalRandom.current().nextInt(50));
        long current = System.currentTimeMillis();

        // 选举超时检查
        if(current - node.getPreElectionTime() < node.getElectionTime()){
            return;
        }

        // 开始选举
        startElection();
    }

    /**
     * 1. 转变为候选人以后立刻开始选举
     *  增加任期号 -> 给自己投票 -> 发送投票请求给其余节点
     * 2. 接收到大多数投票就变为 Leader
     * 3. 接收到其余节点的附加日志 RPC 就变为 FOLLOWER
     * 4. 超时的话重新开始一次选举
     */
    private void startElection() {
        // 变为候选人
        node.setStatus(NodeStatus.CANDIDATE);
        // 更新选举时间
        node.setPreElectionTime(System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(200) + 150);
        // 增加任期号
        node.setCurrentTerm(node.getCurrentTerm() + 1);

        log.debug("节点 {} 成为候选人, 开始选举Leader, 任期号: {}, 最后日志: {}",
                node.getPeerSet().getSelf().getAddr(),
                node.getCurrentTerm(),
                node.getLogModule().getLastEntry()
        );

        // 给自己投票
        node.setVotedFor(node.getPeerSet().getSelf().getAddr());

        // 发送投票请求
        List<Peer> peers = node.getPeerSet().getPeersWithoutSelf();
        log.debug("节点 {} 发送投票请求给 {} 个节点", node.getPeerSet().getSelf().getAddr(), peers.size());

        List<Future<VoteResult>> futureList = new ArrayList<>();

        for(Peer peer : peers){
            futureList.add(
                    RaftThreadPool.submit(() -> {
                        LogEntry entry = node.getLogModule().getLastEntry();
                        long lastLogTerm = entry == null ? 0 : entry.getTerm();

                        // 请求参数
                        VoteParam param = VoteParam.builder()
                                .term(node.getCurrentTerm())
                                .serverId(peer.getAddr())
                                .candidateId(node.getPeerSet().getSelf().getAddr())
                                .lastLogTerm(lastLogTerm)
                                .lastLogIndex(node.getLogModule().getLastIndex())
                                .build();

                        // 请求体
                        Request request = Request.builder()
                                .cmd(Request.VOTE)
                                .obj(param)
                                .url(peer.getAddr())
                                .build();

                        try{
                            return node.getRpcClient().send(request);
                        }catch (Exception e){
                            log.error("请求投票失败, url: {}", peer.getAddr(), e);
                            return null;
                        }
                    })
            );
        }

        // 等待投票结果
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(futureList.size());

        for(Future<VoteResult> future : futureList){
            // TODO 是否可以换成execute?
            RaftThreadPool.submit(() -> {
                try{
                    VoteResult result = future.get(3000, TimeUnit.MILLISECONDS);
                    if(result == null){
                        return -1;
                    }

                    boolean isVoteGranted = result.isVoteGranted();
                    if(isVoteGranted){
                        successCount.incrementAndGet();
                    }
                    else{
                        // 更新自己的任期
                        long resultTerm = result.getTerm();
                        if(resultTerm > node.getCurrentTerm()){
                            node.setCurrentTerm(resultTerm);
                        }
                    }
                    return 0;
                }catch (Exception e){
                    log.error("投票异常", e);
                    return -1;
                }finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(3500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn("选举任务被中断");
        }

        int voteCount = successCount.get();
        log.info("节点 {} 获得 {} 票，共需要 {} 票",
                node.getPeerSet().getSelf().getAddr(),
                voteCount,
                peers.size() / 2
        );

        // 如果在投票期间，有其他服务器发送 appendEntry，就可能变成 follower
        if (node.getStatus() == NodeStatus.FOLLOWER) {
            return;
        }

        if(voteCount >= peers.size() / 2){
            // 成为 Leader
            log.info("节点 {} 成为 Leader", node.getPeerSet().getSelf().getAddr());
            node.setStatus(NodeStatus.LEADER);
            node.getPeerSet().setLeader(node.getPeerSet().getSelf());
            node.setVotedFor(null);

            // TODO 成为 Leader 的初始化工作
            node.becomeLeaderToDoThing();
        }
        else{
            node.setVotedFor(null);
        }

    }
}
