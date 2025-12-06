package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 客户端请求的返回结果
 * Raft 节点返回给客户端的结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientKVAck implements Serializable {
    private static final long serialVersionUID = 1L;

    private Object obj;

    public static ClientKVAck ok(){
        return new ClientKVAck("ok");
    }

    public static ClientKVAck fail(){
        return new ClientKVAck("fail");
    }
}
