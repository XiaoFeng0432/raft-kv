package com.xf.raft.core.entity;

import lombok.Data;

@Data
public class Peer {
    private final String addr;

    public Peer(String addr) {
        this.addr = addr;
    }
}
