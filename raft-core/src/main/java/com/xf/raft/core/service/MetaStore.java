package com.xf.raft.core.service;


/**
 * 负责持久化 currentTerm 和 votedFor
 */
public interface MetaStore {
    void setCurrentTerm(long term);

    long getCurrentTerm();

    void setVotedFor(String votedFor);

    String getVotedFor();

    void destroy() throws Exception;
}
