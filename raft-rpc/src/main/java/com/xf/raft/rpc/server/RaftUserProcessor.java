package com.xf.raft.rpc.server;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;
import com.xf.raft.rpc.protocol.Request;

/**
 * Raft 服务器的请求处理器基类
 * @param <T>
 */
public abstract class RaftUserProcessor<T> extends AbstractUserProcessor<T> {
    @Override
    public void handleRequest(BizContext bizContext, AsyncContext asyncContext, T t) {
        throw new UnsupportedOperationException("Raft Server 不支持异步处理请求");
    }

    @Override
    public abstract Object handleRequest(BizContext bizContext, T t);

    @Override
    public String interest() {
        return Request.class.getName();
    }
}
