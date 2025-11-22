package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户端的 KV 写请求
 * 客户端向 Raft 节点发送写请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientKVReq {

    public static final String GET = "GET";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";

    // 请求的操作类型
    private String operation;
    // 请求的键
    private String key;
    // 请求的值
    private String value;
}
