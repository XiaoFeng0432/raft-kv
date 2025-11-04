package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户端请求的返回结果
 * Raft 节点返回给客户端的结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientKVAck {
    // 客户端请求的键
    private String key;
    // 请求的结果
    private boolean success;
    // 错误消息，如果有的话
    private String message;
}
