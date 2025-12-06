package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeerSet implements Serializable {
    private List<Peer> list = new ArrayList<>();

    private volatile Peer Leader;
    private volatile Peer self;

    public static PeerSet getInstance(){
        return PeerSetLazyHolder.INSTANCE;
    }

    private static class PeerSetLazyHolder {
        private static final PeerSet INSTANCE = new PeerSet();
    }

    public void addPeer(Peer peer){
        list.add(peer);
    }

    public void removePeer(Peer peer){
        list.remove(peer);
    }

    public List<Peer> getPeers(){
        return list;
    }

    public List<Peer> getPeersWithoutSelf(){
        List<Peer> list2 = new ArrayList<>(list);
        list2.remove(self);
        return list2;
    }


}
