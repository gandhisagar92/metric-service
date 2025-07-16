import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * High-performance metric storage service using multiple hash-based indexes
 * Optimized for millions of records with O(1) lookup performance
 */
@Service
public class MetricStorageService {
    
    // Primary storage - all records
    private final Set<MetricRecord> primaryStorage = ConcurrentHashMap.newKeySet();
    
    // Multiple indexes for different query patterns - O(1) lookups
    private final Map<CompositeKeys.TypeMessageKey, Set<MetricRecord>> typeMessageIndex = new ConcurrentHashMap<>();
    private final Map<CompositeKeys.TypeMessageVersionKey, Set<MetricRecord>> typeMessageVersionIndex = new ConcurrentHashMap<>();
    private final Map<CompositeKeys.TypeMessageVersionSourceKey, Set<MetricRecord>> typeMessageVersionSourceIndex = new ConcurrentHashMap<>();
    
    // For timestamp queries - using NavigableMap for range queries
    private final Map<String, NavigableMap<Long, Set<MetricRecord>>> typeTimestampIndex = new ConcurrentHashMap<>();
    
    // Read-Write lock for consistent updates across all indexes
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * Add a metric record to all indexes
     */
    public void addMetricRecord(MetricRecord record) {
        lock.writeLock().lock();
        try {
            // Add to primary storage
            primaryStorage.add(record);
            
            // Update all indexes
            updateIndexes(record);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Add multiple records efficiently in batch
     */
    public void addMetricRecords(Collection<MetricRecord> records) {
        lock.writeLock().lock();
        try {
            primaryStorage.addAll(records);
            
            for (MetricRecord record : records) {
                updateIndexes(record);
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void updateIndexes(MetricRecord record) {
        // Index 1: Type + MessageId
        CompositeKeys.TypeMessageKey key1 = new CompositeKeys.TypeMessageKey(record.getType(), record.getMessageId());
        typeMessageIndex.computeIfAbsent(key1, k -> ConcurrentHashMap.newKeySet()).add(record);
        
        // Index 2: Type + MessageId + Version
        CompositeKeys.TypeMessageVersionKey key2 = new CompositeKeys.TypeMessageVersionKey(
            record.getType(), record.getMessageId(), record.getVersion());
        typeMessageVersionIndex.computeIfAbsent(key2, k -> ConcurrentHashMap.newKeySet()).add(record);
        
        // Index 3: Type + MessageId + Version + Source
        CompositeKeys.TypeMessageVersionSourceKey key3 = new CompositeKeys.TypeMessageVersionSourceKey(
            record.getType(), record.getMessageId(), record.getVersion(), record.getSource());
        typeMessageVersionSourceIndex.computeIfAbsent(key3, k -> ConcurrentHashMap.newKeySet()).add(record);
        
        // Index 4: Type + Timestamp (for range queries)
        typeTimestampIndex.computeIfAbsent(record.getType(), k -> new ConcurrentSkipListMap<>())
                         .computeIfAbsent(record.getTimestampInMillis(), k -> ConcurrentHashMap.newKeySet())
                         .add(record);
    }
    
    /**
     * Query 1: Type + MessageId - O(1) lookup
     */
    public List<MetricQueryResult> findByTypeAndMessageId(String type, String messageId) {
        lock.readLock().lock();
        try {
            CompositeKeys.TypeMessageKey key = new CompositeKeys.TypeMessageKey(type, messageId);
            Set<MetricRecord> records = typeMessageIndex.get(key);
            
            return records != null ? 
                records.stream().map(this::toQueryResult).collect(Collectors.toList()) : 
                Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Query 2: Type + MessageId + Version - O(1) lookup
     */
    public List<MetricQueryResult> findByTypeMessageIdAndVersion(String type, String messageId, int version) {
        lock.readLock().lock();
        try {
            CompositeKeys.TypeMessageVersionKey key = new CompositeKeys.TypeMessageVersionKey(type, messageId, version);
            Set<MetricRecord> records = typeMessageVersionIndex.get(key);
            
            return records != null ? 
                records.stream().map(this::toQueryResult).collect(Collectors.toList()) : 
                Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Query 3: Type + MessageId + Version + Source - O(1) lookup
     */
    public List<MetricQueryResult> findByTypeMessageIdVersionAndSource(String type, String messageId, int version, String source) {
        lock.readLock().lock();
        try {
            CompositeKeys.TypeMessageVersionSourceKey key = new CompositeKeys.TypeMessageVersionSourceKey(type, messageId, version, source);
            Set<MetricRecord> records = typeMessageVersionSourceIndex.get(key);
            
            return records != null ? 
                records.stream().map(this::toQueryResult).collect(Collectors.toList()) : 
                Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Query 4: Type + Timestamp - O(log n) for exact match, supports range queries
     */
    public List<MetricQueryResult> findByTypeAndTimestamp(String type, long timestampInMillis) {
        lock.readLock().lock();
        try {
            NavigableMap<Long, Set<MetricRecord>> timestampMap = typeTimestampIndex.get(type);
            if (timestampMap == null) {
                return Collections.emptyList();
            }
            
            Set<MetricRecord> records = timestampMap.get(timestampInMillis);
            return records != null ? 
                records.stream().map(this::toQueryResult).collect(Collectors.toList()) : 
                Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Range query: Type + Timestamp range - O(log n + k) where k is result size
     */
    public List<MetricQueryResult> findByTypeAndTimestampRange(String type, long startTime, long endTime) {
        lock.readLock().lock();
        try {
            NavigableMap<Long, Set<MetricRecord>> timestampMap = typeTimestampIndex.get(type);
            if (timestampMap == null) {
                return Collections.emptyList();
            }
            
            return timestampMap.subMap(startTime, true, endTime, true)
                              .values()
                              .stream()
                              .flatMap(Set::stream)
                              .map(this::toQueryResult)
                              .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private MetricQueryResult toQueryResult(MetricRecord record) {
        return new MetricQueryResult(record.getApplicationId(), record.getLine());
    }
    
    /**
     * Get storage statistics for monitoring
     */
    public StorageStats getStorageStats() {
        lock.readLock().lock();
        try {
            return new StorageStats(
                primaryStorage.size(),
                typeMessageIndex.size(),
                typeMessageVersionIndex.size(),
                typeMessageVersionSourceIndex.size(),
                typeTimestampIndex.size()
            );
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Clear all data (useful for testing or maintenance)
     */
    public void clearAll() {
        lock.writeLock().lock();
        try {
            primaryStorage.clear();
            typeMessageIndex.clear();
            typeMessageVersionIndex.clear();
            typeMessageVersionSourceIndex.clear();
            typeTimestampIndex.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}

/**
 * Query result containing applicationId and metric line
 */
class MetricQueryResult {
    private final String applicationId;
    private final String line;
    
    public MetricQueryResult(String applicationId, String line) {
        this.applicationId = applicationId;
        this.line = line;
    }
    
    public String getApplicationId() { return applicationId; }
    public String getLine() { return line; }
}

/**
 * Storage statistics for monitoring
 */
class StorageStats {
    private final int totalRecords;
    private final int typeMessageIndexSize;
    private final int typeMessageVersionIndexSize;
    private final int typeMessageVersionSourceIndexSize;
    private final int typeTimestampIndexSize;
    
    public StorageStats(int totalRecords, int typeMessageIndexSize, int typeMessageVersionIndexSize,
                       int typeMessageVersionSourceIndexSize, int typeTimestampIndexSize) {
        this.totalRecords = totalRecords;
        this.typeMessageIndexSize = typeMessageIndexSize;
        this.typeMessageVersionIndexSize = typeMessageVersionIndexSize;
        this.typeMessageVersionSourceIndexSize = typeMessageVersionSourceIndexSize;
        this.typeTimestampIndexSize = typeTimestampIndexSize;
    }
    
    // Getters
    public int getTotalRecords() { return totalRecords; }
    public int getTypeMessageIndexSize() { return typeMessageIndexSize; }
    public int getTypeMessageVersionIndexSize() { return typeMessageVersionIndexSize; }
    public int getTypeMessageVersionSourceIndexSize() { return typeMessageVersionSourceIndexSize; }
    public int getTypeTimestampIndexSize() { return typeTimestampIndexSize; }
}