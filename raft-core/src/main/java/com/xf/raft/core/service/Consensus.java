package com.xf.raft.core.service;

import com.xf.raft.core.entity.AppendEntryParam;
import com.xf.raft.core.entity.AppendEntryResult;
import com.xf.raft.core.entity.VoteParam;
import com.xf.raft.core.entity.VoteResult;

/**
 * 一致性模块接口
 * 负责 Leader选举和日志复制
 */
public interface Consensus {
    /**
     * 请求投票 RPC
     * 候选者发起投票请求时调用
     * @param param 投票参数
     * @return
     */
    VoteResult requestVote(VoteParam param);

    /**
     * 附加日志 RPC
     * Leader 复制日志给 Follower 时调用
     * @param param 日志复制参数
     * @return
     */
    AppendEntryResult appendEntries(AppendEntryParam param);
}
