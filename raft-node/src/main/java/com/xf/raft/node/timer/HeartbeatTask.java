package com.xf.raft.node.timer;

import com.xf.raft.node.impl.DefaultNode;

public class HeartbeatTask implements Runnable{

    private final DefaultNode node;

    public HeartbeatTask(DefaultNode node) {
        this.node = node;
    }

    @Override
    public void run() {

    }
}
