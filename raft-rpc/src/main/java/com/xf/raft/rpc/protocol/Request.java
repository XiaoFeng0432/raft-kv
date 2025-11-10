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
    public static final int VOTE = 0;
    // 附加日志
    public static final int APPEND_ENTRIES = 1;
    // 客户端请求
    public static final int CLIENT_REQUEST = 2;
    // 配置变更 -- 添加节点
    public static final int CHANGE_CONFIG_ADD = 3;
    // 配置变更 -- 删除节点
    public static final int CHANGE_CONFIG_REMOVE = 4;

    // 请求类型
    private int cmd = -1;
    // 请求参数
    private Object obj;
    // 请求地址
    private String url;
}
