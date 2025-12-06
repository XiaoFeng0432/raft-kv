package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 附加日志请求结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppendEntryResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private long term;
    // 是否追加成功
    private boolean success;
}
