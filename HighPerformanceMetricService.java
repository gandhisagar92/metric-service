import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * High-performance metric service optimized for millions of records.
 * Uses multiple indexes for O(1) lookups and thread-safe operations.
 */
public class HighPerformanceMetricService {
    
    // Primary storage - all metric records
    private final Set<MetricRecord> allRecords;
    
    // Multiple indexes for different query patterns - O(1) lookups
    private final Map<QueryKeys.TypeMessageKey, List<MetricRecord>> typeMessageIndex;
    private final Map<QueryKeys.TypeMessageVersionKey, List<MetricRecord>> typeMessageVersionIndex;
    private final Map<QueryKeys.TypeMessageVersionSourceKey, List<MetricRecord>> typeMessageVersionSourceIndex;
    private final Map<QueryKeys.TypeTimestampKey, List<MetricRecord>> typeTimestampIndex;
    
    // Additional optimized indexes
    private final Map<String, Set<MetricRecord>> applicationIdIndex;
    private final Map<String, Set<MetricRecord>> typeIndex;
    
    // Read-write lock for thread safety during bulk operations
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    // Configuration
    private final int initialCapacity;
    private final float loadFactor;
    
    public HighPerformanceMetricService() {
        this(1_000_000, 0.75f); // Default capacity for 1M records
    }
    
    public HighPerformanceMetricService(int initialCapacity, float loadFactor) {
        this.initialCapacity = initialCapacity;
        this.loadFactor = loadFactor;
        
        // Initialize all data structures with optimized settings
        this.allRecords = ConcurrentHashMap.newKeySet(initialCapacity);
        
        // Initialize indexes with estimated sizes
        this.typeMessageIndex = new ConcurrentHashMap<>(initialCapacity / 10, loadFactor);
        this.typeMessageVersionIndex = new ConcurrentHashMap<>(initialCapacity / 5, loadFactor);
        this.typeMessageVersionSourceIndex = new ConcurrentHashMap<>(initialCapacity / 2, loadFactor);
        this.typeTimestampIndex = new ConcurrentHashMap<>(initialCapacity / 100, loadFactor);
        
        this.applicationIdIndex = new ConcurrentHashMap<>(6, loadFactor); // 6 applications
        this.typeIndex = new ConcurrentHashMap<>(50, loadFactor); // Estimated types
    }
    
    /**
     * Add a metric record and update all indexes
     */
    public boolean addMetricRecord(MetricRecord record) {
        if (record == null) return false;
        
        rwLock.writeLock().lock();
        try {
            // Add to primary storage
            if (!allRecords.add(record)) {
                return false; // Duplicate record
            }
            
            // Update all indexes
            updateIndexes(record);
            return true;
            
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    /**
     * Bulk add operation for better performance when processing MQ messages
     */
    public int addMetricRecords(Collection<MetricRecord> records) {
        if (records == null || records.isEmpty()) return 0;
        
        rwLock.writeLock().lock();
        try {
            int added = 0;
            for (MetricRecord record : records) {
                if (allRecords.add(record)) {
                    updateIndexes(record);
                    added++;
                }
            }
            return added;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    private void updateIndexes(MetricRecord record) {
        // Update type-message index
        QueryKeys.TypeMessageKey tmKey = new QueryKeys.TypeMessageKey(record.getType(), record.getMessageId());
        typeMessageIndex.computeIfAbsent(tmKey, k -> new ArrayList<>()).add(record);
        
        // Update type-message-version index
        QueryKeys.TypeMessageVersionKey tmvKey = new QueryKeys.TypeMessageVersionKey(
            record.getType(), record.getMessageId(), record.getVersion());
        typeMessageVersionIndex.computeIfAbsent(tmvKey, k -> new ArrayList<>()).add(record);
        
        // Update type-message-version-source index
        QueryKeys.TypeMessageVersionSourceKey tmvsKey = new QueryKeys.TypeMessageVersionSourceKey(
            record.getType(), record.getMessageId(), record.getVersion(), record.getSource());
        typeMessageVersionSourceIndex.computeIfAbsent(tmvsKey, k -> new ArrayList<>()).add(record);
        
        // Update type-timestamp index
        QueryKeys.TypeTimestampKey ttKey = new QueryKeys.TypeTimestampKey(record.getType(), record.getTimestampInMillis());
        typeTimestampIndex.computeIfAbsent(ttKey, k -> new ArrayList<>()).add(record);
        
        // Update auxiliary indexes
        applicationIdIndex.computeIfAbsent(record.getApplicationId(), k -> ConcurrentHashMap.newKeySet()).add(record);
        typeIndex.computeIfAbsent(record.getType(), k -> ConcurrentHashMap.newKeySet()).add(record);
    }
    
    /**
     * Query by type and messageId - O(1) lookup
     */
    public List<MetricQueryResult> queryByTypeAndMessageId(String type, String messageId) {
        rwLock.readLock().lock();
        try {
            QueryKeys.TypeMessageKey key = new QueryKeys.TypeMessageKey(type, messageId);
            List<MetricRecord> records = typeMessageIndex.get(key);
            return records != null ? 
                records.stream().map(r -> new MetricQueryResult(r.getLine(), r.getApplicationId())).collect(Collectors.toList()) :
                Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    /**
     * Query by type, messageId and version - O(1) lookup
     */
    public List<MetricQueryResult> queryByTypeMessageIdAndVersion(String type, String messageId, int version) {
        rwLock.readLock().lock();
        try {
            QueryKeys.TypeMessageVersionKey key = new QueryKeys.TypeMessageVersionKey(type, messageId, version);
            List<MetricRecord> records = typeMessageVersionIndex.get(key);
            return records != null ? 
                records.stream().map(r -> new MetricQueryResult(r.getLine(), r.getApplicationId())).collect(Collectors.toList()) :
                Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    /**
     * Query by type, messageId, version and source - O(1) lookup
     */
    public List<MetricQueryResult> queryByTypeMessageIdVersionAndSource(String type, String messageId, int version, String source) {
        rwLock.readLock().lock();
        try {
            QueryKeys.TypeMessageVersionSourceKey key = new QueryKeys.TypeMessageVersionSourceKey(type, messageId, version, source);
            List<MetricRecord> records = typeMessageVersionSourceIndex.get(key);
            return records != null ? 
                records.stream().map(r -> new MetricQueryResult(r.getLine(), r.getApplicationId())).collect(Collectors.toList()) :
                Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    /**
     * Query by type and timestamp - O(1) lookup
     */
    public List<MetricQueryResult> queryByTypeAndTimestamp(String type, long timestampInMillis) {
        rwLock.readLock().lock();
        try {
            QueryKeys.TypeTimestampKey key = new QueryKeys.TypeTimestampKey(type, timestampInMillis);
            List<MetricRecord> records = typeTimestampIndex.get(key);
            return records != null ? 
                records.stream().map(r -> new MetricQueryResult(r.getLine(), r.getApplicationId())).collect(Collectors.toList()) :
                Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    /**
     * Get metrics by application ID
     */
    public List<MetricQueryResult> queryByApplicationId(String applicationId) {
        rwLock.readLock().lock();
        try {
            Set<MetricRecord> records = applicationIdIndex.get(applicationId);
            return records != null ? 
                records.stream().map(r -> new MetricQueryResult(r.getLine(), r.getApplicationId())).collect(Collectors.toList()) :
                Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    /**
     * Get statistics about the metric service
     */
    public MetricServiceStats getStats() {
        rwLock.readLock().lock();
        try {
            return new MetricServiceStats(
                allRecords.size(),
                typeMessageIndex.size(),
                typeMessageVersionIndex.size(),
                typeMessageVersionSourceIndex.size(),
                typeTimestampIndex.size(),
                applicationIdIndex.size(),
                typeIndex.size()
            );
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    /**
     * Parse metric line from MQ message
     */
    public static MetricRecord parseMetricLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric line cannot be null or empty");
        }
        
        String[] parts = line.split(",", 10); // Limit to 10 parts, last one can contain commas
        if (parts.length != 10) {
            throw new IllegalArgumentException("Invalid metric line format. Expected 10 comma-separated values");
        }
        
        try {
            return new MetricRecord(
                parts[0].trim(),                    // applicationId
                parts[1].trim(),                    // messageId
                Integer.parseInt(parts[2].trim()),  // version
                parts[3].trim(),                    // source
                parts[4].trim(),                    // type
                parts[5].trim(),                    // alternateId
                parts[6].trim(),                    // eventStatus
                parts[7].trim(),                    // processingStatus
                Long.parseLong(parts[8].trim()),    // timestampInMillis
                parts[9]                            // line (can contain commas, no trim)
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in metric line: " + line, e);
        }
    }
    
    /**
     * Clear all data (useful for testing)
     */
    public void clear() {
        rwLock.writeLock().lock();
        try {
            allRecords.clear();
            typeMessageIndex.clear();
            typeMessageVersionIndex.clear();
            typeMessageVersionSourceIndex.clear();
            typeTimestampIndex.clear();
            applicationIdIndex.clear();
            typeIndex.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}

/**
 * Result class for metric queries
 */
class MetricQueryResult {
    private final String line;
    private final String applicationId;
    
    public MetricQueryResult(String line, String applicationId) {
        this.line = line;
        this.applicationId = applicationId;
    }
    
    public String getLine() { return line; }
    public String getApplicationId() { return applicationId; }
    
    @Override
    public String toString() {
        return String.format("MetricQueryResult{line='%s', applicationId='%s'}", line, applicationId);
    }
}

/**
 * Statistics about the metric service
 */
class MetricServiceStats {
    private final int totalRecords;
    private final int typeMessageIndexSize;
    private final int typeMessageVersionIndexSize;
    private final int typeMessageVersionSourceIndexSize;
    private final int typeTimestampIndexSize;
    private final int applicationIdIndexSize;
    private final int typeIndexSize;
    
    public MetricServiceStats(int totalRecords, int typeMessageIndexSize, int typeMessageVersionIndexSize,
                             int typeMessageVersionSourceIndexSize, int typeTimestampIndexSize,
                             int applicationIdIndexSize, int typeIndexSize) {
        this.totalRecords = totalRecords;
        this.typeMessageIndexSize = typeMessageIndexSize;
        this.typeMessageVersionIndexSize = typeMessageVersionIndexSize;
        this.typeMessageVersionSourceIndexSize = typeMessageVersionSourceIndexSize;
        this.typeTimestampIndexSize = typeTimestampIndexSize;
        this.applicationIdIndexSize = applicationIdIndexSize;
        this.typeIndexSize = typeIndexSize;
    }
    
    // Getters
    public int getTotalRecords() { return totalRecords; }
    public int getTypeMessageIndexSize() { return typeMessageIndexSize; }
    public int getTypeMessageVersionIndexSize() { return typeMessageVersionIndexSize; }
    public int getTypeMessageVersionSourceIndexSize() { return typeMessageVersionSourceIndexSize; }
    public int getTypeTimestampIndexSize() { return typeTimestampIndexSize; }
    public int getApplicationIdIndexSize() { return applicationIdIndexSize; }
    public int getTypeIndexSize() { return typeIndexSize; }
    
    @Override
    public String toString() {
        return String.format("MetricServiceStats{totalRecords=%d, indexes=[tm=%d, tmv=%d, tmvs=%d, tt=%d, app=%d, type=%d]}", 
                           totalRecords, typeMessageIndexSize, typeMessageVersionIndexSize, 
                           typeMessageVersionSourceIndexSize, typeTimestampIndexSize, 
                           applicationIdIndexSize, typeIndexSize);
    }
}