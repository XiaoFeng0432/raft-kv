package com.xf.raft.store;

import com.xf.raft.core.service.MetaStore;
import org.junit.jupiter.api.Test;

public class MetaStoreTest {
    MetaStore metaStore = new DefaultMetaStore();


    // 测试 metaStore 能否成功存储状态
    @Test
    public void set() throws Exception {
        metaStore.setCurrentTerm(1);
        metaStore.setVotedFor("1");
        metaStore.destroy();
    }

    @Test
    public void get() throws Exception {
        System.out.println(metaStore.getCurrentTerm());
        System.out.println(metaStore.getVotedFor());
        metaStore.destroy();
    }
}
