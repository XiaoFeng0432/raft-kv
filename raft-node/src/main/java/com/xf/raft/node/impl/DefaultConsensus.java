package com.xf.raft.node.impl;

import com.xf.raft.core.entity.AppendEntryParam;
import com.xf.raft.core.entity.AppendEntryResult;
import com.xf.raft.core.entity.VoteParam;
import com.xf.raft.core.entity.VoteResult;
import com.xf.raft.core.service.Consensus;

public class DefaultConsensus implements Consensus {
    @Override
    public VoteResult requestVote(VoteParam param) {
        return null;
    }

    @Override
    public AppendEntryResult appendEntries(AppendEntryParam param) {
        return null;
    }
}
