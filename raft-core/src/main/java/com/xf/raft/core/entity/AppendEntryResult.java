package com.xf.raft.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 附加日志请求结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppendEntryResult {
    private long term;
    // 是否追加成功
    private boolean success;
}
