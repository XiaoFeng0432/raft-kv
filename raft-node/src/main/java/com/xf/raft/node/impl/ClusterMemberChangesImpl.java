package com.xf.raft.node.impl;

import com.xf.raft.core.entity.ClusterResult;
import com.xf.raft.core.entity.Peer;
import com.xf.raft.core.service.ClusterMemberChanges;

/**
 * 集群配置变更接口实现
 */
public class ClusterMemberChangesImpl implements ClusterMemberChanges {
    @Override
    public ClusterResult addPeer(Peer newPeer) {
        return null;
    }

    @Override
    public ClusterResult removePeer(Peer oldPeer) {
        return null;
    }
}
