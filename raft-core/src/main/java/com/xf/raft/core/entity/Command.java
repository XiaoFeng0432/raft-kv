package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 执行的命令
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Command implements Serializable {
    private String key;
    private String value;
}
