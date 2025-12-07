package com.xf.raft.core.exception;

public class RaftRemotingException extends RuntimeException{

    public RaftRemotingException() {
    }

    public RaftRemotingException(String message, Throwable cause) {
        super(message, cause);
    }
}
