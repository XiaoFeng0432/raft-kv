package com.xf.raft.rpc.client;

import com.alipay.remoting.exception.RemotingException;
import com.xf.raft.rpc.protocol.Request;
import com.xf.raft.rpc.protocol.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultRpcClient implements RpcClient {

    private static final com.alipay.remoting.rpc.RpcClient CLIENT = new com.alipay.remoting.rpc.RpcClient();

    @Override
    public void init() throws Throwable {
        CLIENT.startup();
        log.info("RpcClient 初始化成功");
    }

    @Override
    public void destroy() throws Throwable {
        CLIENT.shutdown();
        log.info("RpcClient 销毁成功");
    }

    @Override
    public <R> R send(Request request) {
        return send(request, (int) TimeUnit.SECONDS.toMillis(5));
    }

    @Override
    public <R> R send(Request request, int timeout) {
        Response<R> response;
        try {
            response = (Response<R>) CLIENT.invokeSync(request.getUrl(), request, timeout);
            return response.getResult();
        } catch (RemotingException e) {
            log.error("Rpc 调用失败, url:{}, error:{}",request.getUrl(), e.getMessage());
            throw new RuntimeException("Rpc远程调用异常", e);
        } catch (InterruptedException e) {
            log.error("Rpc 调用被中断, url:{}, error:{}",request.getUrl(), e.getMessage());
            Thread.currentThread().interrupt();
        }
        return null;
    }
}
