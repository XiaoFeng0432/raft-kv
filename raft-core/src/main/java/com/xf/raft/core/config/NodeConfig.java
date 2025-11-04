package com.xf.raft.core.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Raft 节点的配置类
 * 包括节点信息、集群信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeConfig {
    // 节点端口
    private int port;
    // 集群中其他节点的地址
    private String[] cluster;
}
