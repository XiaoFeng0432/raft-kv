package com.xf.raft.store;

import com.alibaba.fastjson2.JSON;
import com.xf.raft.core.entity.LogEntry;
import com.xf.raft.core.service.LogModule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 默认日志模块实现 -基于 RocksDB
 * 添加、查询、删除日志，得到最后一条日志及其索引
 */
@Data
@Slf4j
public class DefaultLogModule implements LogModule {

    private String dbDir;
    private String logDir;

    private RocksDB logDb;

    // 最后一条日志索引的 key
    private static final byte[] LAST_INDEX_KEY = "LAST_INDEX_KEY".getBytes();

    private final ReentrantLock lock = new ReentrantLock();

    public DefaultLogModule() {
        if(dbDir == null){
            dbDir = "./RocksDB/" + System.getProperty("serverPort", "default");
        }
        if(logDir == null){
            logDir = dbDir + "/log";
        }
        initRocksDB();
    }

    /**
     * 初始化 RocksDB
     */
    private void initRocksDB(){
        log.info("RocksDB 初始化中......");
        RocksDB.loadLibrary();
        Options options = new Options();
        options.setCreateIfMissing(true);

        File file = new File(logDir);
        if(!file.exists()){
            boolean success = file.mkdirs();
            if(success){
                log.info("创建日志目录成功: {}", logDir);
            }
        }

        try {
            logDb = RocksDB.open(options, logDir);
            log.info("RocksDB 初始化完成, 目录: {}", logDir);
        } catch (RocksDBException e) {
            log.error("RocksDB 初始化失败", e);
            throw new RuntimeException("RocksDB 初始化失败", e);
        }
    }

    /**
     * 单例模式
     */
    public static DefaultLogModule getInstance(){
        return DefaultLogModuleLazyHolder.INSTANCE;
    }
    private static class DefaultLogModuleLazyHolder {
        private static final DefaultLogModule INSTANCE = new DefaultLogModule();
    }


    @Override
    public void init() throws Throwable {

    }

    @Override
    public void destroy() throws Throwable {
        logDb.close();
        log.info("RocksDB 销毁成功");
    }

    /**
     * 写入日志
     * @param entry
     */
    @Override
    public void write(LogEntry entry) {
        boolean success = false;
        try {
            if(!lock.tryLock(3000, TimeUnit.MILLISECONDS)){
                throw new RuntimeException("写入日志失败: 获取锁超时");
            }

            long nextIndex = getLastIndex() + 1;
            entry.setIndex(nextIndex);

            byte[] key = String.valueOf(nextIndex).getBytes();
            byte[] value = JSON.toJSONBytes(entry);

            logDb.put(key, value);

            success = true;
            log.info("写入日志成功: item={}, index={}", entry.getTerm(), entry.getIndex());
        } catch (RocksDBException e) {
            log.error("RocksDB写入日志失败", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log.error("写入日志被中断", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("写入日志被中断", e);
        }finally {
            if(success){
                updateLastIndex(entry.getIndex());
            }
            lock.unlock();
        }
    }

    /**
     * 读取指定索引日志
     * @param index
     * @return
     */
    @Override
    public LogEntry read(Long index) {
        try {
            byte[] key = String.valueOf(index).getBytes();
            byte[] value = logDb.get(key);

            if(value == null){
                return null;
            }

            return JSON.parseObject(value, LogEntry.class);
        } catch (RocksDBException e) {
            log.error("RocksDB读取日志失败", e);
            throw new RuntimeException("RocksDB读取日志失败", e);
        }
    }

    /**
     * 删除指定索引之后的所有日志
     * @param startIndex
     */
    @Override
    public void removeOnStartIndex(Long startIndex) {
        boolean success = false;
        int count = 0;
        try {
            if(!lock.tryLock(3000, TimeUnit.MILLISECONDS)){
                throw new RuntimeException("删除日志失败: 获取锁超时");
            }
            long lastIndex = getLastIndex();
            for(long i = startIndex; i <= lastIndex; i++){
                byte[] key = String.valueOf(i).getBytes();
                logDb.delete(key);
                count++;
            }

            success = true;
            log.info("删除日志成功: startIndex={}, count={}, 原lastIndex={}", startIndex, count, lastIndex);
        } catch (RocksDBException e) {
            log.error("RocksDB删除日志失败", e);
            throw new RuntimeException("RocksDB删除日志失败", e);
        } catch (InterruptedException e) {
            log.error("删除日志被中断", e);
            throw new RuntimeException("删除日志被中断", e);
        }finally {
            if(success){
                updateLastIndex(startIndex - 1);
            }
            lock.unlock();
        }
    }

    /**
     * 获取最后一条日志
     */
    @Override
    public LogEntry getLastEntry() {
        Long lastIndex = getLastIndex();

        if(lastIndex == 0L){
            return null;
        }

        return read(lastIndex);
    }

    /**
     * 获取最后一条日志的索引
     * 返回 -1 表示没有日志
     */
    @Override
    public Long getLastIndex() {
        try {
            byte[] value = logDb.get(LAST_INDEX_KEY);
            if(value == null){
                return 0L;
            }
            return Long.valueOf(new String(value));
        } catch (RocksDBException e) {
            log.error("获取 lastIndex 失败", e);
            throw new RuntimeException("获取 lastIndex 失败", e);
        }
    }

    /**
     * 更新最后日志索引
     * 只有在持有锁的情况下才能调用
     */
    private void updateLastIndex(long index){
        byte[] value = String.valueOf(index).getBytes();
        try {
            logDb.put(LAST_INDEX_KEY, value);
        } catch (RocksDBException e) {
            log.error("更新 lastIndex 失败", e);
            throw new RuntimeException("更新 lastIndex 失败", e);
        }
    }
}
