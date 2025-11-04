package com.xf.raft.core;

import com.xf.raft.core.config.NodeConfig;
import com.xf.raft.core.service.Consensus;
import com.xf.raft.core.service.LifeCycle;
import com.xf.raft.core.service.LogModule;
import com.xf.raft.core.service.StateMachine;

import java.util.Timer;

public class RaftNode implements LifeCycle {

    private NodeConfig nodeConfig; // 节点配置信息
    private String id; // 节点唯一标识
    private Consensus consensus; // 一致性模块接口
    private LogModule logModule; // 日志模块接口
    private StateMachine stateMachine; // 状态机接口

    private Timer timer; // 定时器

    private boolean isLeader = false;
    private long electionTimeout = 5000;
    private long heartbeatInterval = 2000;

    public RaftNode(NodeConfig config){
        this.nodeConfig = config;
        this.id = "Node-" + config.getPort();
    }

    @Override
    public void init() throws Throwable {
        System.out.println("RaftNode:" + id + " init...");

//        this.consensus = new DefaultConsensus();

    }

    @Override
    public void destroy() throws Throwable {

    }
}
