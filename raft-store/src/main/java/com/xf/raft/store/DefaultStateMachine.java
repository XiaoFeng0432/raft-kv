package com.xf.raft.store;

import com.xf.raft.core.entity.LogEntry;
import com.xf.raft.core.service.StateMachine;

public class DefaultStateMachine implements StateMachine {
    @Override
    public void apply(LogEntry entry) {

    }

    @Override
    public LogEntry get(String key) {
        return null;
    }

    @Override
    public String getString(String key) {
        return null;
    }

    @Override
    public void setString(String key, String value) {

    }

    @Override
    public void delString(String... keys) {

    }
}
