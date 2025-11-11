package com.xf.raft.core.entity;

/**
 * 节点的状态
 */
public class NodeStatus {
    public static final int FOLLOWER = 0;
    public static final int CANDIDATE = 1;
    public static final int LEADER = 2;

    enum Enum{
        FOLLOWER(0),
        CANDIDATE(1),
        LEADER(2);

        Enum(int code){
            this.code = code;
        }

        final int code;

        public static Enum value(int i){
            for(Enum value : values()){
                if(value.code == i){
                    return value;
                }
            }
            return null;
        }
    }
}
