package com.xf.raft.store;

import com.alibaba.fastjson2.JSON;
import com.xf.raft.core.entity.Command;
import com.xf.raft.core.entity.LogEntry;
import com.xf.raft.core.service.StateMachine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;


/**
 * 状态机
 */
@Data
@Slf4j
public class DefaultStateMachine implements StateMachine {

    public String dbDir;
    public String stateMachineDir;

    public RocksDB machineDB;

    public DefaultStateMachine() {
        dbDir = "./RocksDB/" + System.getProperty("serverPort", "default");
        stateMachineDir = dbDir + "/stateMachine";
        initStateMachineDB();
    }

    /**
     * 初始化状态机
     */
    private void initStateMachineDB() {
        log.info("stateMachine 初始化中......");
        RocksDB.loadLibrary();
        Options options = new Options();
        options.setCreateIfMissing(true);

        File file = new File(stateMachineDir);
        if(!file.exists()){
            boolean success = file.mkdirs();
            if(success){
                log.info("创建状态机目录成功: {}", stateMachineDir);
            }
        }

        try {
            machineDB = RocksDB.open(options, stateMachineDir);
            log.info("stateMachine 初始化完成, 目录: {}", stateMachineDir);
        } catch (RocksDBException e) {
            log.error("stateMachine 初始化失败");
            throw new RuntimeException("stateMachine 初始化失败");
        }
    }

    /**
     * 单例模式
     */
    public static DefaultStateMachine getInstance(){
        return DefaultStateMachineLazyHolder.INSTANCE;
    }
    public static class DefaultStateMachineLazyHolder {
        private static final DefaultStateMachine INSTANCE = new DefaultStateMachine();
    }

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void destroy() throws Throwable {
        machineDB.close();
        log.info("stateMachine 销毁成功");
    }

    /**
     * 将日志应用到状态机
     * <p>
     * 核心逻辑：
     * 1. 提取日志中的 Command (key-value)
     * 2. 将整个 LogEntry 序列化后存储 (包含 term、index 等元信息)
     * 3. 这样可以追溯每个 key 的历史版本
     * <p>
     * 注意：
     * - 此方法必须是幂等的 (多次应用同一条日志，结果相同)
     * - 此方法必须是线程安全的
     */
    @Override
    public synchronized void apply(LogEntry entry) {
        try{
            Command command = entry.getCommand();
            if(command == null){
                log.info("应用空日志, item={}, index={}", entry.getTerm(), entry.getIndex());
                return;
            }

            byte[] key = command.getKey().getBytes();
            byte[] value = JSON.toJSONBytes(entry);
            machineDB.put(key, value);
            log.info("应用日志到状态机成功, key={}, value={}, index={}",key, command.getValue(), entry.getIndex());
        } catch (RocksDBException e) {
            log.error("应用日志到状态机失败", e);
            throw new RuntimeException("应用日志到状态机失败", e);
        }
    }

    /* 下面的函数是为了方便获取数据，并不影响实现，但是方便 */
    /**
     * 根据 key 获取 entry 信息
     */
    @Override
    public LogEntry get(String key) {
        try {
            byte[] keyBytes = key.getBytes();
            byte[] value = machineDB.get(keyBytes);

            if(value == null){
                return null;
            }
            return JSON.parseObject(value, LogEntry.class);
        } catch (RocksDBException e) {
            log.error("从状态机读取数据失败: key={}", key, e);
            throw new RuntimeException("从状态机读取数据失败", e);
        }
    }

    /**
     * 根据 key 获取 value(简化版)
     */
    @Override
    public String getString(String key) {
        LogEntry entry = get(key);
        if(entry == null || entry.getCommand() == null){
            return null;
        }
        return entry.getCommand().getValue();
    }

    /**
     * 设置 key-value
     */
    @Override
    public void setString(String key, String value) {
        byte[] keyBytes = key.getBytes();
        byte[] valueBytes = value.getBytes();
        try {
            machineDB.put(keyBytes, valueBytes);
        } catch (RocksDBException e) {
            log.error("写入状态机失败: key={}", key, e);
            throw new RuntimeException("写入状态机失败", e);
        }
    }

    /**
     * 删除指定 key
     */
    @Override
    public void delString(String... keys) {
        try {
            for(String key : keys){
                machineDB.delete(key.getBytes());
            }
        } catch (RocksDBException e) {
            log.error("删除状态机数据失败", e);
            throw new RuntimeException("删除状态机数据失败", e);
        }
    }

}
