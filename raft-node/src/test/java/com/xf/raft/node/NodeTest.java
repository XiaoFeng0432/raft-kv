package com.xf.raft.node;

import com.xf.raft.core.config.NodeConfig;
import com.xf.raft.node.impl.DefaultNode;

import java.util.Arrays;

public class NodeTest {

    // 测试 node 创建是否成功
    public static void main(String[] args) throws Throwable {
        // 配置节点
        NodeConfig config = new NodeConfig();
        config.setPort(8777);
        config.setPeerAddrs(Arrays.asList(
                "localhost:8777",
                "localhost:8778",
                "localhost:8779"
        ));

        // 创建节点
        DefaultNode node = DefaultNode.getInstance();
        node.setConfig(config);
        node.init();

        // 保持运行
        synchronized (node) {
            node.wait();
        }
    }
}
