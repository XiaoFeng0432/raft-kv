package com.xf.raft.rpc.server;

import com.xf.raft.core.service.LifeCycle;
import com.xf.raft.rpc.protocol.Request;
import com.xf.raft.rpc.protocol.Response;


public interface RpcServer extends LifeCycle {
    /**
     * 处理请求
     * @param request 请求参数
     * @return
     */
    Response<?> handlerRequest(Request request);
}
