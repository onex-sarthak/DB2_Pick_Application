package org.onextel.db2_pick_app.service.rocksdb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RocksDBDlrHandler {

    private RocksDB rocksDB;

    @PostConstruct
    public void init() {
        RocksDB.loadLibrary();
        try(Options options = new Options().setCreateIfMissing(true)) {
            File lockFile = new File("rocksdb-dlr-data/LOCK");
            if (lockFile.exists()) {
                log.warn("Deleting orphaned LOCK file.");
                lockFile.delete();
            }
            rocksDB = RocksDB.open(options, "rocksdb-dlr-data");
            log.info("RocksDB initialized successfully.");
        } catch (RocksDBException e) {
            log.error("Error initializing RocksDB", e);
            throw new RuntimeException(e);
        }
    }
    @PreDestroy
    public void close() {
        if (rocksDB != null) {
            rocksDB.close();
            log.info("RocksDB closed successfully.");
        }
    }

    public void put(String key, String value) {
        try {
            rocksDB.put(key.getBytes(), value.getBytes());
            log.info("Written to RocksDB: key={}, value={}", key, value);
        } catch (RocksDBException e) {
            log.error("Error writing to RocksDB", e);
        }
    }

    public String get(String key) {
        try {
            byte[] value = rocksDB.get(key.getBytes());
            return value != null ? new String(value) : null;
        } catch (RocksDBException e) {
            log.error("Error reading from RocksDB", e);
            return null;
        }
    }

    public void delete(String key) {
        try {
            rocksDB.delete(key.getBytes());
            log.info("Deleted from RocksDB: key={}", key);
        } catch (RocksDBException e) {
            log.error("Error deleting from RocksDB", e);
        }
    }

    public List<String> iterate() {
        List<String> keys = new ArrayList<>();
        try (var iterator = rocksDB.newIterator()) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                keys.add(new String(iterator.key()));
            }
        } catch (Exception e) {
            log.error("Error iterating over RocksDB", e);
        }
        return keys;
    }

}
