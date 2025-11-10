package com.xf.raft.core.service;

import com.xf.raft.core.entity.ClusterResult;
import com.xf.raft.core.entity.Peer;

public interface ClusterMemberChanges {

    /**
     * 添加节点
     * @param newPeer
     * @return
     */
    ClusterResult addPeer(Peer newPeer);

    /**
     * 删除节点
     * @param oldPeer
     * @return
     */
    ClusterResult removePeer(Peer oldPeer);

}
