package com.xf.raft.node.impl;

import com.xf.raft.core.config.NodeConfig;
import com.xf.raft.core.entity.*;
import com.xf.raft.core.service.*;
import com.xf.raft.node.thread.RaftThreadPool;
import com.xf.raft.node.timer.ElectionTask;
import com.xf.raft.node.timer.HeartbeatTask;
import com.xf.raft.rpc.client.DefaultRpcClient;
import com.xf.raft.rpc.client.RpcClient;
import com.xf.raft.rpc.server.DefaultRpcServer;
import com.xf.raft.rpc.server.RpcServer;
import com.xf.raft.store.DefaultLogModule;
import com.xf.raft.store.DefaultStateMachine;
import jdk.jfr.DataAmount;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class DefaultNode implements Node, ClusterMemberChanges {

    /* 定时器相关 */
    public volatile long electionTime = 15 * 1000; // 选举时间间隔
    public final long heartbeatInterval = 5 * 100; // 心跳间隔
    public volatile long preElectionTime = 0; // 上一次选举时间
    public volatile long preHeartbeatTime = 0; // 上一次心跳时间戳

    /* 节点状态 */
    public volatile int status = NodeStatus.FOLLOWER; // 节点当前状态
    public PeerSet peerSet; // 节点集合
    volatile boolean running = false; // 运行标志

    /* 所有服务器上持久存在的 */
    volatile long currentTerm = 0; // 服务器最后一次知道的任期号 初始化为 0
    volatile String votedFor; // 节点当前任期内投给了哪个候选人 ID
    LogModule logModule; // 日志模块

    /* 所有服务器上经常改变的 */
    volatile long commitIndex; // 已知的最大的已经被提交的日志条目的索引值
    volatile long lastApplied = 0; // 最后被应用到状态机的日志条目索引值

    /* 组件 */
    public NodeConfig nodeConfig; // 配置
    public RpcServer rpcServer; // RPC 服务端
    public RpcClient rpcClient = new DefaultRpcClient(); // RPC 客户端
    public StateMachine stateMachine; // 状态机
    Consensus consensus; // 一致性模块

    /* 定时任务 */
    private ElectionTask electionTask; // 选举定时任务
    private HeartbeatTask heartbeatTask; // 心跳定时任务

    public static DefaultNode getInstance() {
        return DefaultNodeLazyHolder.INSTANCE;
    }

    private static class DefaultNodeLazyHolder {
        private static final DefaultNode INSTANCE = new DefaultNode();
    }


    /**
     * 初始化节点
     */
    @Override
    public void init() throws Throwable {
        if(running){
            return;
        }
        running = true;

        // 初始化 RPC
        rpcClient.init();
        rpcServer.init();

        // 初始化一致性模块
        consensus = new DefaultConsensus(this);

        // 初始化定时任务
        electionTask = new ElectionTask(this);
        heartbeatTask = new HeartbeatTask(this);
//        delegate = new ClusterMembershipChangesImpl(this); // 似乎没用到

        // 启动心跳任务
        RaftThreadPool.scheduleWithFixedDelay(heartbeatTask, 500);
        // 启动选举任务
        RaftThreadPool.scheduleAtFixedRate(electionTask, 6000, 500);
//        RaftThreadPool.execute(replicationFailQueueConsumer);

        // 恢复任期
        LogEntry entry = logModule.getLastEntry();
        if(entry != null){
            currentTerm = entry.getTerm();
        }
        log.info("节点 {} 启动成功", peerSet.getSelf().getAddr());
    }

    /**
     * 销毁节点
     */
    @Override
    public void destroy() throws Throwable {
        running = false;
        rpcClient.destroy();
        rpcServer.destroy();
        logModule.destroy();
        stateMachine.destroy();
        log.info("节点 {} 销毁成功", peerSet.getSelf().getAddr());
    }

    /**
     * 设置节点配置
     */
    @Override
    public void setConfig(NodeConfig config) {
        this.nodeConfig = config;
        logModule = DefaultLogModule.getInstance();
        stateMachine = DefaultStateMachine.getInstance();
        peerSet = PeerSet.getInstance();
        for(String addr : config.getPeerAddrs()){
            Peer peer = new Peer(addr);
            peerSet.addPeer(peer);
            if(addr.equals("localhost:" + config.getPort())){
                peerSet.setSelf(peer);
            }
        }
        rpcServer = new DefaultRpcServer(config.getPort(), this);
    }

    @Override
    public VoteResult handlerRequestVote(VoteParam param) {
        log.debug("收到投票请求: {}", param);
        return consensus.requestVote(param);
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

    @Override
    public ClusterResult addPeer(Peer newPeer) {
        return null;
    }

    @Override
    public ClusterResult removePeer(Peer oldPeer) {
        return null;
    }
}
