package com.xf.raft.core.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClusterResult {

    public static final int FAIL = 0;
    public static final int SUCCESS = 1;

    int status;
    String leaderHint;

    public enum Status{
        FAIL(0), SUCCESS(1);
        int code;

        Status(int code){
            this.code = code;
        }

        public static Status value(int v){
            for(Status status : values()){
                if(status.code == v){
                    return status;
                }
            }
            return null;
        }
    }

}
