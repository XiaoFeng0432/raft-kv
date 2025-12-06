package com.xf.raft.rpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request implements Serializable {
    // 请求投票
    public static final String VOTE = "VOTE";
    // 附加日志
    public static final String APPEND_ENTRIES = "APPEND_ENTRIES";
    // 客户端请求
    public static final String CLIENT_REQUEST = "CLIENT_REQUEST";
    // 配置变更 -- 添加节点
    public static final String CHANGE_CONFIG_ADD = "CHANGE_CONFIG_ADD";
    // 配置变更 -- 删除节点
    public static final String CHANGE_CONFIG_REMOVE = "CHANGE_CONFIG_REMOVE";

    // 请求类型
    private String cmd;
    // 请求参数
    private Object obj;
    // 请求地址
    private String url;
}
