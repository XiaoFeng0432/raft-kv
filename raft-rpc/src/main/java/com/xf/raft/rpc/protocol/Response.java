package com.xf.raft.rpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {
    private T result;

    public static Response<String> ok(){
        return new Response<>("ok");
    }

    public static Response<String> fail(){
        return new Response<>("fail");
    }
}
