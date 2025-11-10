package com.xf.raft.rpc.client;

import com.xf.raft.core.service.LifeCycle;
import com.xf.raft.rpc.protocol.Request;

public interface RpcClient extends LifeCycle {

    /**
     * 发送请求，并同步等待返回值，默认5s
     * @param request 请求参数
     * @param <R> 返回值泛型
     * @return 返回结果
     */
    <R> R send(Request request);

    <R> R send(Request request, int timeout);
}
