package com.xf.raft.node;

import com.xf.raft.core.config.NodeConfig;
import com.xf.raft.node.impl.DefaultNode;

import java.util.Arrays;

public class RaftNodeStarter {

    /*
    mvn clean package "-Dmaven.test.skip=true"
    java -jar -DserverPort=8777 raft-node/target/raft-node-1.0-SNAPSHOT-jar-with-dependencies.jar
    */

    private static final String logo =
            " ███████       ██     ████████ ██████████       ██   ██ ██      ██\n" +
            " ██    ██     ████    ██           ██           ██  ██  ██      ██\n" +
            " ██    ██    ██  ██   ██           ██           ██ ██   ██      ██\n" +
            " ███████    ██    ██  ███████      ██     █████ ████     ██    ██ \n" +
            " ██   ██   ██████████ ██           ██           ██ ██     ██  ██  \n" +
            " ██    ██  ██      ██ ██           ██           ██  ██     ████   \n" +
            " ██     ██ ██      ██ ██           ██           ██   ██     ██    \n" +
            "                                                                  \n";

    public static void main(String[] args) throws Throwable {
        // 从系统属性获取端口
        String portStr = System.getProperty("serverPort", "8779");
        int port = Integer.parseInt(portStr);

        // 配置节点
        NodeConfig config = new NodeConfig();
        config.setPort(port);
        config.setPeerAddrs(Arrays.asList(
                "localhost:8775",
                "localhost:8776",
                "localhost:8777",
                "localhost:8778",
                "localhost:8779"
        ));

        // 创建并启动节点
        DefaultNode node = DefaultNode.getInstance();
        node.setConfig(config);
        node.init();

        System.out.println(logo);

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                node.destroy();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }));

        // 保持运行
        synchronized (node) {
            node.wait();
        }
    }
}
