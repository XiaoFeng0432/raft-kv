# Raft-KV

基于 Raft 一致性算法实现的分布式键值存储系统。

自己参考 [lu-raft-kv](https://github.com/stateIs0/lu-raft-kv) 写的一个Raft算法轮子项目，但是对原项目进行了重构，对代码进行了自己的改进，修复了存在的一些错误等

## 特性

- **Raft 一致性算法**：完整实现 Leader 选举、日志复制和安全性保证
- **持久化存储**：基于 RocksDB 的日志、元数据和状态机存储
- **高性能 RPC**：使用 SOFABolt 框架实现节点间高效通信
- **故障恢复**：自动持久化 `currentTerm` 和 `votedFor` 保证容错
- **强一致性**：所有读写操作提供线性一致性保证

## 架构

```
raft-kv/
├── raft-core/       # Raft 核心接口和实体类
├── raft-node/       # 节点实现
├── raft-rpc/        # 基于 SOFABolt 的 RPC 层
└── raft-store/      # 基于 RocksDB 的存储层
```

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+

### 构建

```bash
mvn clean package "-Dmaven.test.skip=true"
```

### 启动集群

在不同端口启动 3 个节点（在 `raft-kv` 目录中）：

```bash
# 终端 1
java -jar -DserverPort=8777 raft-node/target/raft-node-1.0-SNAPSHOT-jar-with-dependencies.jar

# 终端 2
java -jar -DserverPort=8778 raft-node/target/raft-node-1.0-SNAPSHOT-jar-with-dependencies.jar

# 终端 3
java -jar -DserverPort=8779 raft-node/target/raft-node-1.0-SNAPSHOT-jar-with-dependencies.jar
```

**客户端操作**

这里我为了省事，直接启动三个节点，然后在IDEA中的测试类进行的测试



## 技术细节

### 一致性模块

- **选举超时**：3000ms + 随机抖动（1000ms）
- **心跳间隔**：1000ms
- **投票规则**：仅当候选人日志至少和自己一样新时才投票

### 日志复制

- **写入流程**：Leader → 本地日志 → 复制 → 等待多数派 → 提交 → 应用
- **重试机制**：失败时递减 nextIndex，超时时间 20s
- **一致性检查**：追加前验证 prevLogIndex 和 prevLogTerm

### 持久化

- **元数据**：在 RPC 响应前将 `currentTerm` 和 `votedFor` 存储到 RocksDB
- **日志存储**：顺序写入，原子更新索引
- **状态机**：序列化完整 LogEntry 以支持审计追踪

## 配置

在 `RaftNodeStarter.java` 中编辑集群节点：

```
config.setPeerAddrs(Arrays.asList(
    "localhost:8777",
    "localhost:8778", 
    "localhost:8779"
));
```
