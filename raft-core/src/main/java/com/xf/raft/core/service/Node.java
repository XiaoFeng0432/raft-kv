package com.xf.raft.core.service;

import com.xf.raft.core.config.NodeConfig;
import com.xf.raft.core.entity.*;

/**
 * Raft 节点接口
 * 聚合了一致性模块、日志模块、状态机模块
 */
public interface Node {
    // 配置节点信息（端口、集群等）
    void setConfig(NodeConfig config);

    // 处理投票请求
    VoteResult handlerRequestVote(VoteParam param);

    // 处理日志复制请求
    AppendEntryResult handlerAppendEntries(AppendEntryParam param);

    // 处理客户端写请求
    ClientKVAck handlerClientRequest(ClientKVReq request);

    // 如果请求到了 Follower，重定向到 Leader
    ClientKVAck redirect(ClientKVReq request);
}
