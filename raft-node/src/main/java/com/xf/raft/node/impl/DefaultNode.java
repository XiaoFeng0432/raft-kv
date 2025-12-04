package com.xf.raft.node.impl;

import com.xf.raft.core.config.NodeConfig;
import com.xf.raft.core.entity.*;
import com.xf.raft.core.service.*;
import com.xf.raft.node.thread.RaftThreadPool;
import com.xf.raft.node.timer.ElectionTask;
import com.xf.raft.node.timer.HeartbeatTask;
import com.xf.raft.rpc.client.DefaultRpcClient;
import com.xf.raft.rpc.client.RpcClient;
import com.xf.raft.rpc.protocol.Request;
import com.xf.raft.rpc.server.DefaultRpcServer;
import com.xf.raft.rpc.server.RpcServer;
import com.xf.raft.store.DefaultLogModule;
import com.xf.raft.store.DefaultStateMachine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    /* Leader 上经常改变的 */
    Map<Peer, Long> nextIndexes; // 对于每一个服务器，需要发送给他的下一个日志条目的索引值
    Map<Peer, Long> matchIndexes; // 对于每一个服务器，已经复制给他的日志的最高索引值

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
        if (running) {
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
        if (entry != null) {
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
        for (String addr : config.getPeerAddrs()) {
            Peer peer = new Peer(addr);
            peerSet.addPeer(peer);
            if (addr.equals("localhost:" + config.getPort())) {
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
        if (param.getEntries() != null && !param.getEntries().isEmpty()) {
            log.debug("收到来自 {} 的附加日志请求: size={}", param.getLeaderId(), param.getEntries().size());
        }
        return consensus.appendEntries(param);
    }

    /**
     * 处理客户端的键值请求
     * 该方法根据当前节点的角色以及请求类型执行不同的处理逻辑：
     * 如果当前节点不是 Leader，则将请求重定向给 Leader 节点
     * 如果是 GET 请求，则直接从状态机中读取数据并返回
     * 如果是写操作 PUT/DELETE，则先写入本地日志，并异步复制到其他节点，在多数节点确认后提交日志并应用到状态机
     */
    @Override
    public ClientKVAck handlerClientRequest(ClientKVReq request) {
        log.debug("收到客户端请求: {} {} {}",
                request.getOperation(), request.getKey(), request.getValue());

        // 如果不是 Leader 则重定向
        if (status != NodeStatus.LEADER) {
            log.debug("当前节点 {} 不是 Leader, 重定向请求", peerSet.getSelf().getAddr());
            return redirect(request);
        }

        // 处理 GET 请求
        if (request.getOperation().equals(ClientKVReq.GET)) {
            LogEntry entry = stateMachine.get(request.getKey());
            if (entry != null) {
                return new ClientKVAck(entry);
            }
            return new ClientKVAck(null);
        }

        // 处理 PUT/DELETE 请求
        LogEntry logEntry = LogEntry.builder()
                .command(new Command(request.getKey(), request.getValue()))
                .term(currentTerm)
                .build();

        // 写入本地日志
        logModule.write(logEntry);
        log.debug("Leader 写入本地日志: term={}, index={}", logEntry.getTerm(), logEntry.getIndex());

        // 复制到其他节点
        AtomicInteger successCount = new AtomicInteger(0);
        List<Future<Boolean>> futureList = new ArrayList<>();

        for (Peer peer : peerSet.getPeersWithoutSelf()) {
            futureList.add(replication(peer, logEntry));
        }

        // 等待复制结果
        CountDownLatch latch = new CountDownLatch(futureList.size());

        for (Future<Boolean> future : futureList) {
            RaftThreadPool.execute(() -> {
                try {
                    Boolean result = future.get(4000, TimeUnit.MILLISECONDS);
                    if (result) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("复制日志失败: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(4000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("等待复制结果失败: {}", e.getMessage());
        }

        int peerCount = peerSet.getPeersWithoutSelf().size();
        log.info("日志复制结果: success={}, total={}, need={}",
                successCount.get(), peerCount, peerCount / 2);

        // Leader 决定哪些日志可以提交
        List<Long> matchIndexList = new ArrayList<>(matchIndexes.values());
        matchIndexList.add(logModule.getLastIndex());

        int majority = peerSet.getPeers().size() / 2 + 1;
        Collections.sort(matchIndexList, Collections.reverseOrder());

        // 找到满足多数派且属于当前任期的最大 N
        for (Long N : matchIndexList) {
            if (N <= commitIndex) break;

            // 统计有多少服务器的 matchIndex >= N
            int count = 1; // 节点本身
            for (Long peerMatch : matchIndexes.values()) {
                if (peerMatch >= N) count++;
            }

            if (count >= majority) {
                LogEntry entry = logModule.read(N);
                // 关键：只能提交当前任期的日志
                if (entry != null && entry.getTerm() == currentTerm) {
                    commitIndex = N;
                    break;
                }
            }
        }

        // 3. 将日志应用到状态机的判断改为基于 commitIndex
        if (logEntry.getIndex() <= commitIndex) {
            // 仅应用已提交的日志
            stateMachine.apply(logEntry);
            lastApplied = logEntry.getIndex();
            log.info("日志应用到状态机成功: logEntry={}", logEntry);
            return ClientKVAck.ok();
        } else {
            // 未提交则回滚
            logModule.removeOnStartIndex(logEntry.getIndex());
            log.info("日志复制失败, 回滚: index={}", logEntry.getIndex());
            return ClientKVAck.fail();
        }

    }

    /**
     * 复制日志到指定节点
     */
    private Future<Boolean> replication(Peer peer, LogEntry entry) {
        return RaftThreadPool.submit(() -> {
            long begin = System.currentTimeMillis();
            long end = begin;

            while (end - begin <= 20 * 1000L) {
                // 得到需要复制的日志
                List<LogEntry> entries = new ArrayList<>();
                Long nextIndex = nextIndexes.get(peer);

                if(nextIndex == null){
                    nextIndex = logModule.getLastIndex() + 1;
                    nextIndexes.put(peer, nextIndex);
                }

                if (nextIndex <= entry.getIndex()) {
                    for (long i = nextIndex; i <= entry.getIndex(); i++) {
                        LogEntry e = logModule.read(i);
                        if (e != null) {
                            entries.add(e);
                        }
                    }
                } else {
                    entries.add(entry);
                }

                LogEntry prev = getPreLog(entries.get(0));

                AppendEntryParam param = AppendEntryParam.builder()
                        .term(currentTerm)
                        .serverId(peer.getAddr())
                        .leaderId(peerSet.getSelf().getAddr())
                        .prevLogTerm(prev.getTerm())
                        .prevLogIndex(prev.getIndex())
                        .entries(entries)
                        .leaderCommit(commitIndex)
                        .build();

                Request request = Request.builder()
                        .cmd(Request.APPEND_ENTRIES)
                        .obj(param)
                        .url(peer.getAddr())
                        .build();

                try {
                    AppendEntryResult result = rpcClient.send(request);
                    if (result == null) {
                        return false;
                    }

                    if (result.isSuccess()) {
                        // 复制成功
                        nextIndexes.put(peer, entry.getIndex() + 1);
                        matchIndexes.put(peer, entry.getIndex());
                        return true;
                    } else {
                        // 复制失败
                        if (result.getTerm() > currentTerm) {
                            // 发现更高的任期, 转成 FOLLOWER
                            log.debug("节点 {} 附加日志过程中发现 {} 具有更高任期 term={}, 转成 Follower",
                                    peerSet.getSelf().getAddr(), peer.getAddr(), result.getTerm());
                            status = NodeStatus.FOLLOWER;
                            currentTerm = result.getTerm();
                            return false;
                        } else {
                            // 减小 nextIndex 重试
                            if (nextIndex == 0L) {
                                nextIndex = 1L;
                            }
                            nextIndexes.put(peer, nextIndex - 1);
                            log.warn("日志不匹配，减小 nextIndex 重试");
                        }
                    }

                    end = System.currentTimeMillis();

                } catch (Exception e) {
                    log.warn("日志复制请求失败: {}", e.getMessage());
                }
            }
            // 超时
            return false;
        });
    }

    private LogEntry getPreLog(LogEntry entry) {
        LogEntry read = logModule.read(entry.getIndex() - 1);
        if (read == null) {
            log.debug("前一条日志不存在: logEntry={}", entry);
            return LogEntry.builder()
                    .term(0)
                    .index(0)
                    .command(null)
                    .build();
        }
        return read;
    }

    /**
     * 重定向
     * 如果请求到 follower 节点, 将请求转发给 Leader
     */
    @Override
    public ClientKVAck redirect(ClientKVReq request) {
        Request r = Request.builder()
                .cmd(Request.CLIENT_REQUEST)
                .obj(request)
                .url(peerSet.getLeader().getAddr())
                .build();
        return rpcClient.send(r);
    }

    @Override
    public ClusterResult addPeer(Peer newPeer) {
        return null;
    }

    @Override
    public ClusterResult removePeer(Peer oldPeer) {
        return null;
    }

    /**
     * 节点变成 Leader 以后需要做的事情
     * TODO 是不是需要添加更多操作？
     */
    public void becomeLeaderToDoThing() {
        log.info("初始化 Leader 状态");

        nextIndexes = new ConcurrentHashMap<>();
        matchIndexes = new ConcurrentHashMap<>();

        for (Peer peer : peerSet.getPeersWithoutSelf()) {
            nextIndexes.put(peer, logModule.getLastIndex() + 1);
            matchIndexes.put(peer, 0L);
        }
    }
}
