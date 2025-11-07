package com.xf.raft.store;

import com.xf.raft.core.entity.Command;
import com.xf.raft.core.entity.LogEntry;
import org.junit.jupiter.api.Test;

public class DefaultLogModuleTest {

    static {
        System.setProperty("serverPort", "test-8080");
    }
    static DefaultLogModule logModule = DefaultLogModule.getInstance();

    @Test
    public void writeAndReadTest(){
        // 写入日志
        LogEntry entry = LogEntry.builder()
                .term(1L)
                .command(new Command("test-key", "test-value"))
                .build();
        logModule.write(entry);

        // 读取日志
        LogEntry readEntry = logModule.read(entry.getIndex());
        System.out.println("读取到的日志为: " + readEntry);
    }

    @Test
    public void removeOnStartIndexTest(){
        // 删除日志
        logModule.removeOnStartIndex(1L);
    }

}
