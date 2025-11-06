package com.xf.raft.node.impl;

import com.xf.raft.core.config.NodeConfig;
import com.xf.raft.core.entity.*;
import com.xf.raft.core.service.Node;
import jdk.jfr.DataAmount;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class DefaultNode implements Node {

    /* 定时器相关 */
    public volatile long electionTime = 15 * 1000; // 选举时间间隔基数
    public final long heartbeatInterval = 5 * 100; // 心跳间隔基数
    public volatile long preElectionTime = 0; // 上一次选举时间
    public volatile long preHeartbeatTime = 0; // 上一次心跳时间戳

    /* 节点当前状态 */
    public volatile int status = NodeStatus.FOLLOWER;
    public PeerSet peerSet;
    volatile boolean running = false;

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void destroy() throws Throwable {

    }

    @Override
    public void setConfig(NodeConfig config) {

    }

    @Override
    public VoteResult handlerRequestVote(VoteParam param) {
        return null;
    }

    @Override
    public AppendEntryResult handlerAppendEntries(AppendEntryParam param) {
        return null;
    }

    @Override
    public ClientKVAck handlerClientRequest(ClientKVReq request) {
        return null;
    }

    @Override
    public ClientKVAck redirect(ClientKVReq request) {
        return null;
    }
}
