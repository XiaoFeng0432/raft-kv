package com.xf.raft.store;

import com.xf.raft.core.service.MetaStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 持久化存储 Raft 元数据 (currentTerm, votedFor)
 * 论文要求：Updated on stable storage before responding to RPCs
 */
@Data
@Slf4j
public class DefaultMetaStore implements MetaStore {

    private String dbDir;
    private String metaDir;
    private RocksDB metaDB;

    private static final byte[] CURRENT_TERM_KEY = "CURRENT_TERM".getBytes();
    private static final byte[] VOTED_FOR_KEY = "VOTED_FOR".getBytes();

    private final ReentrantLock lock = new ReentrantLock();

    public DefaultMetaStore() {
        if(dbDir == null){
            dbDir = "../RocksDB/" + System.getProperty("serverPort", "default");
        }
        if(metaDir == null){
            metaDir = dbDir + "/meta";
        }
        initRocksDB();
    }

    private void initRocksDB(){
        log.info("MetaStore 初始化中......");
        RocksDB.loadLibrary();
        Options options = new Options();
        options.setCreateIfMissing(true);

        File file = new File(metaDir);
        if(!file.exists()){
            boolean success = file.mkdirs();
            if(success){
                log.info("创建元数据目录成功: {}", metaDir);
            }
        }

        try {
            metaDB = RocksDB.open(options, metaDir);
            log.info("MetaStore 初始化完成, 目录: {}", metaDir);
        } catch (RocksDBException e) {
            log.error("MetaStore 初始化失败", e);
            throw new RuntimeException("MetaStore 初始化失败", e);
        }
    }

    public static DefaultMetaStore getInstance(){
        return DefaultMetaStoreLazyHolder.INSTANCE;
    }

    private static class DefaultMetaStoreLazyHolder {
        private static final DefaultMetaStore INSTANCE = new DefaultMetaStore();
    }

    /**
     * 持久化 currentTerm
     */
    public void setCurrentTerm(long term) {
        lock.lock();
        try {
            byte[] value = String.valueOf(term).getBytes();
            metaDB.put(CURRENT_TERM_KEY, value);
            log.debug("持久化 currentTerm: {}", term);
        } catch (RocksDBException e) {
            log.error("持久化 currentTerm 失败", e);
            throw new RuntimeException("持久化 currentTerm 失败", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 读取 currentTerm
     */
    public long getCurrentTerm() {
        lock.lock();
        try {
            byte[] value = metaDB.get(CURRENT_TERM_KEY);
            if(value == null){
                return 0;
            }
            return Long.parseLong(new String(value));
        } catch (RocksDBException e) {
            log.error("读取 currentTerm 失败", e);
            throw new RuntimeException("读取 currentTerm 失败", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 持久化 votedFor
     */
    public void setVotedFor(String candidateId) {
        lock.lock();
        try {
            if(candidateId == null){
                metaDB.delete(VOTED_FOR_KEY);
            } else {
                metaDB.put(VOTED_FOR_KEY, candidateId.getBytes());
            }
            log.debug("持久化 votedFor: {}", candidateId);
        } catch (RocksDBException e) {
            log.error("持久化 votedFor 失败", e);
            throw new RuntimeException("持久化 votedFor 失败", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 读取 votedFor
     */
    public String getVotedFor() {
        lock.lock();
        try {
            byte[] value = metaDB.get(VOTED_FOR_KEY);
            if(value == null){
                return null;
            }
            return new String(value);
        } catch (RocksDBException e) {
            log.error("读取 votedFor 失败", e);
            throw new RuntimeException("读取 votedFor 失败", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 原子性更新 term 和 votedFor
     */
    public void updateTermAndVotedFor(long term, String votedFor) {
        lock.lock();
        try {
            setCurrentTerm(term);
            setVotedFor(votedFor);
        } finally {
            lock.unlock();
        }
    }

    public void destroy() throws Exception {
        metaDB.close();
        log.info("MetaStore 销毁成功");
    }
}