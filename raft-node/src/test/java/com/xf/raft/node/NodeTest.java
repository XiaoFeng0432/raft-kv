package com.xf.raft.node;

import com.xf.raft.core.entity.ClientKVAck;
import com.xf.raft.core.entity.ClientKVReq;
import com.xf.raft.rpc.client.DefaultRpcClient;
import com.xf.raft.rpc.protocol.Request;
import org.junit.jupiter.api.Test;


public class NodeTest {

    DefaultRpcClient client = new DefaultRpcClient();

    /**
     * 写入一条数据 并且读取
     * 测试 PUT，GET，节点重定向
     */
    @Test
    public void test1() throws Throwable {
        client.init();
        String url = "localhost:8777";

        ClientKVAck res = put("name", "raft-kv", url);
        System.out.println(res);

        // GET 请求
        ClientKVAck res2 = get("name", url);
        System.out.println(res2);

        url = "localhost:8775";
        System.out.println(get("name", url));
        url = "localhost:8776";
        System.out.println(get("name", url));

        client.destroy();
    }

    /**
     * 测试连续写入 99 条数据
     */
    @Test
    public void test2() throws Throwable {
        client.init();

        String url = "localhost:8777";

        for(int i = 11; i <= 20; i++){
            ClientKVAck res = put(String.valueOf(i), String.valueOf(i), url);
            System.out.println(res.getObj());
        }

        client.destroy();
    }


    public ClientKVAck get(String key, String url){
        ClientKVReq getReq = ClientKVReq.builder()
                .operation(ClientKVReq.GET)
                .key(key)
                .build();

        return sendRequest(getReq, url);
    }

    public ClientKVAck put(String key, String value, String url){
        ClientKVReq putReq = ClientKVReq.builder()
                .operation(ClientKVReq.PUT)
                .key(key)
                .value(value)
                .build();
        return sendRequest(putReq, url);
    }

    public ClientKVAck delete(String key, String value, String url){
        ClientKVReq deleteReq = ClientKVReq.builder()
                .operation(ClientKVReq.DELETE)
                .key(key)
                .value(value)
                .build();
        return sendRequest(deleteReq, url);
    }



    public ClientKVAck sendRequest(ClientKVReq req, String url){
        Request request = Request.builder()
                .cmd(Request.CLIENT_REQUEST)
                .obj(req)
                .url(url)
                .build();

        ClientKVAck ack = client.send(request);
        System.out.println(req.getOperation() + " result: " + ack);
        return ack;
    }
}
