package com.xf.raft.core.service;


/**
 * 负责持久化 currentTerm 和 votedFor
 */
public interface StatusStore {
    void setCurrentTerm(long term);

    long getCurrentTerm();

    void setVotedFor(String votedFor);

    String getVotedFor();

    void updateTermAndVotedFor(long term, String votedFor);

    void destroy() throws Exception;
}
