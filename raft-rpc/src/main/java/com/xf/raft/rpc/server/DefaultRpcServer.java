package com.xf.raft.rpc.server;

import com.alipay.remoting.BizContext;
import com.xf.raft.core.entity.AppendEntryParam;
import com.xf.raft.core.entity.ClientKVReq;
import com.xf.raft.core.entity.Peer;
import com.xf.raft.core.entity.VoteParam;
import com.xf.raft.core.service.ClusterMemberChanges;
import com.xf.raft.core.service.Node;
import com.xf.raft.rpc.protocol.Request;
import com.xf.raft.rpc.protocol.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认 Rpc 服务端实现（基于 SOFABolt）
 */
@Slf4j
public class DefaultRpcServer implements RpcServer{

    private final Node node;
    private final com.alipay.remoting.rpc.RpcServer SERVER;

    public DefaultRpcServer(int port, Node node) {
        this.node = node;
        SERVER = new com.alipay.remoting.rpc.RpcServer(port, false, false);
        SERVER.registerUserProcessor(new RaftUserProcessor<Request>() {
            @Override
            public Object handleRequest(BizContext bizContext, Request request){
                return handlerRequest(request);
            }
        });
    }

    @Override
    public void init() throws Throwable {
        SERVER.startup();
        log.info("RpcServer 初始化成功");
    }

    @Override
    public void destroy() throws Throwable {
        SERVER.shutdown();
        log.info("RpcServer 销毁成功");
    }

    @Override
    public Response<?> handlerRequest(Request request) {
        log.info("收到 Rpc 请求: cmd={}", request.getCmd());
        switch (request.getCmd()){
            case Request.VOTE:
                return new Response<>(
                        node.handlerRequestVote((VoteParam) request.getObj())
                );
            case Request.APPEND_ENTRIES:
                return new Response<>(
                        node.handlerAppendEntries((AppendEntryParam) request.getObj())
                );
            case Request.CLIENT_REQUEST:
                return new Response<>(
                        node.handlerClientRequest((ClientKVReq) request.getObj())
                );
            case Request.CHANGE_CONFIG_ADD:
                return new Response<>(
                        ((ClusterMemberChanges) node).addPeer((Peer) request.getObj())
                );
            case Request.CHANGE_CONFIG_REMOVE:
                return new Response<>(
                        ((ClusterMemberChanges) node).removePeer((Peer) request.getObj())
                );
            default:
                log.error("未知的请求类型: " + request.getCmd());
                return Response.fail();
        }
    }
}
