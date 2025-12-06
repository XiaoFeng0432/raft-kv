package com.xf.raft.node;

import com.xf.raft.core.config.NodeConfig;
import com.xf.raft.core.entity.ClientKVAck;
import com.xf.raft.core.entity.ClientKVReq;
import com.xf.raft.node.impl.DefaultNode;
import com.xf.raft.rpc.client.DefaultRpcClient;
import com.xf.raft.rpc.protocol.Request;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class NodeTest {

    DefaultRpcClient client = new DefaultRpcClient();

    // 测试 GET PUT DELETE 操作
    @Test
    public void UserClient() throws Throwable {
//        DefaultRpcClient client = new DefaultRpcClient();
        client.init();

        ClientKVAck res = put("name", "raft-kv");
        System.out.println(res);

        // GET 请求
        ClientKVAck res2 = get("name");
        System.out.println(res2);

        client.destroy();
    }


    public ClientKVAck get(String key){
        ClientKVReq getReq = ClientKVReq.builder()
                .operation(ClientKVReq.GET)
                .key(key)
                .build();

        return sendRequest(getReq);
    }


    public ClientKVAck put(String key, String value){
        ClientKVReq putReq = ClientKVReq.builder()
                .operation(ClientKVReq.PUT)
                .key(key)
                .value(value)
                .build();
        return sendRequest(putReq);
    }

    public ClientKVAck delete(String key, String value){
        ClientKVReq deleteReq = ClientKVReq.builder()
                .operation(ClientKVReq.DELETE)
                .key(key)
                .value(value)
                .build();
        return sendRequest(deleteReq);
    }



    public ClientKVAck sendRequest(ClientKVReq req){
        Request request = Request.builder()
                .cmd(Request.CLIENT_REQUEST)
                .obj(req)
                .url("localhost:8777")
                .build();

        ClientKVAck ack = client.send(request);
        System.out.println(req.getOperation() + " result: " + ack);
        return ack;
    }
}
