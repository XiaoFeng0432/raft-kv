package com.xf.raft.node.timer;

import com.xf.raft.core.entity.AppendEntryParam;
import com.xf.raft.core.entity.AppendEntryResult;
import com.xf.raft.core.entity.NodeStatus;
import com.xf.raft.core.entity.Peer;
import com.xf.raft.node.impl.DefaultNode;
import com.xf.raft.node.thread.RaftThreadPool;
import com.xf.raft.rpc.protocol.Request;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class HeartbeatTask implements Runnable{

    private final DefaultNode node;

    public HeartbeatTask(DefaultNode node) {
        this.node = node;
    }

    @Override
    public void run() {
        if(node.getStatus() != NodeStatus.LEADER){
            return;
        }

        // 检查是否超出心跳间隔
        long current = System.currentTimeMillis();
        if(current - node.getPreHeartbeatTime() < node.getHeartbeatInterval()){
            return;
        }

        // 发送心跳
        log.debug("==================== Leader 发送心跳 ====================");
        List<Peer> peers = node.getPeerSet().getPeersWithoutSelf();
        for(Peer peer : peers){
            log.debug("Peer {} nextIndex={}", peer.getAddr(), node.getNextIndexes().get(peer));
        }

        node.setPreHeartbeatTime(System.currentTimeMillis());

        for(Peer peer : peers){
            // 包装请求参数
            AppendEntryParam param = AppendEntryParam.builder()
                    .term(node.getCurrentTerm())
                    .serverId(peer.getAddr())
                    .leaderId(node.getPeerSet().getSelf().getAddr())
                    .entries(null)
                    .leaderCommit(node.getCommitIndex())
                    .build();
            Request request = Request.builder()
                    .cmd(Request.APPEND_ENTRIES)
                    .obj(param)
                    .url(peer.getAddr())
                    .build();

            // 异步发送请求
            RaftThreadPool.execute(() -> {
                try{
                    AppendEntryResult result = node.getRpcClient().send(request);

                    if(result == null){
                        return;
                    }

                    long resultTerm = result.getTerm();
                    // 如果发现更高的任期, 转化为 FOLLOWER
                    if(resultTerm > node.getCurrentTerm()){
                        log.debug("节点 {} 当前任期: {}, 发现更高任期: {}, 转化为 FOLLOWER",
                                node.getPeerSet().getSelf().getAddr(), node.getCurrentTerm(), resultTerm);
                        synchronized (node){
                            node.setCurrentTerm(resultTerm);
                            node.setStatus(NodeStatus.FOLLOWER);
                            node.setVotedFor(null);
                        }
                    }
                }catch (Exception e){
                    log.error("发送心跳异常", e);
                }
            }, false);
        }

    }
}
