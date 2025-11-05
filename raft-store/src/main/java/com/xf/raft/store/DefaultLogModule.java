package com.xf.raft.store;

import com.xf.raft.core.entity.LogEntry;
import com.xf.raft.core.service.LogModule;

public class DefaultLogModule implements LogModule {
    @Override
    public void write(LogEntry entry) {

    }

    @Override
    public LogEntry read(Long index) {
        return null;
    }

    @Override
    public void removeOnStartIndex(Long startIndex) {

    }

    @Override
    public LogEntry getLastEntry() {
        return null;
    }

    @Override
    public Long getLastIndex() {
        return null;
    }
}
