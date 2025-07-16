import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.values.Values;
import net.openhft.chronicle.values.Copyable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced metric service using Chronicle Map for off-heap storage.
 * This version can handle 10M+ records with minimal heap usage.
 * 
 * Chronicle Map advantages:
 * - Off-heap storage (minimal GC pressure)
 * - Persistent storage option
 * - Very fast access times
 * - Memory-mapped files
 * - Better for very large datasets (10M+ records)
 */
public class ChronicleMapMetricService {
    
    // Off-heap indexes using Chronicle Map
    private final ChronicleMap<String, List<Long>> typeMessageIndex;
    private final ChronicleMap<String, List<Long>> typeMessageVersionIndex;
    private final ChronicleMap<String, List<Long>> typeMessageVersionSourceIndex;
    private final ChronicleMap<String, List<Long>> typeTimestampIndex;
    
    // Primary storage with IDs for reference
    private final ChronicleMap<Long, CompactMetricRecord> recordStorage;
    private volatile long nextId = 1;
    
    // In-memory index for fast application lookup (small dataset)
    private final Map<String, Set<Long>> applicationIdIndex;
    
    public ChronicleMapMetricService() throws IOException {
        this(10_000_000); // Default 10M capacity
    }
    
    public ChronicleMapMetricService(long expectedRecords) throws IOException {
        // Initialize Chronicle Maps with estimated sizes
        this.recordStorage = ChronicleMap
            .of(Long.class, CompactMetricRecord.class)
            .entries(expectedRecords)
            .create();
        
        this.typeMessageIndex = ChronicleMap
            .of(String.class, (Class<List<Long>>) (Class) ArrayList.class)
            .entries(expectedRecords / 10)
            .averageKey("TYPE_MSGID_12345678901234567890") // Average key length
            .averageValue(Arrays.asList(1L, 2L, 3L, 4L, 5L)) // Average list size
            .create();
        
        this.typeMessageVersionIndex = ChronicleMap
            .of(String.class, (Class<List<Long>>) (Class) ArrayList.class)
            .entries(expectedRecords / 5)
            .averageKey("TYPE_MSGID_V1_1234567890123456")
            .averageValue(Arrays.asList(1L, 2L, 3L))
            .create();
        
        this.typeMessageVersionSourceIndex = ChronicleMap
            .of(String.class, (Class<List<Long>>) (Class) ArrayList.class)
            .entries(expectedRecords / 2)
            .averageKey("TYPE_MSGID_V1_SOURCE_12345678901")
            .averageValue(Arrays.asList(1L, 2L))
            .create();
        
        this.typeTimestampIndex = ChronicleMap
            .of(String.class, (Class<List<Long>>) (Class) ArrayList.class)
            .entries(expectedRecords / 100)
            .averageKey("TYPE_1640995200000")
            .averageValue(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L))
            .create();
        
        // Keep application index in heap (only 6 applications)
        this.applicationIdIndex = new ConcurrentHashMap<>();
    }
    
    /**
     * Add metric record with optimized storage
     */
    public boolean addMetricRecord(MetricRecord record) {
        long id = nextId++;
        
        // Create compact record for off-heap storage
        CompactMetricRecord compact = CompactMetricRecord.of(record);
        recordStorage.put(id, compact);
        
        // Update indexes
        updateIndexes(record, id);
        
        return true;
    }
    
    private void updateIndexes(MetricRecord record, long id) {
        // Type-Message index
        String tmKey = record.getType() + "_" + record.getMessageId();
        typeMessageIndex.compute(tmKey, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(id);
            return v;
        });
        
        // Type-Message-Version index
        String tmvKey = record.getType() + "_" + record.getMessageId() + "_V" + record.getVersion();
        typeMessageVersionIndex.compute(tmvKey, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(id);
            return v;
        });
        
        // Type-Message-Version-Source index
        String tmvsKey = record.getType() + "_" + record.getMessageId() + "_V" + record.getVersion() + "_" + record.getSource();
        typeMessageVersionSourceIndex.compute(tmvsKey, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(id);
            return v;
        });
        
        // Type-Timestamp index
        String ttKey = record.getType() + "_" + record.getTimestampInMillis();
        typeTimestampIndex.compute(ttKey, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(id);
            return v;
        });
        
        // Application ID index (in-memory)
        applicationIdIndex.computeIfAbsent(record.getApplicationId(), k -> ConcurrentHashMap.newKeySet()).add(id);
    }
    
    /**
     * Query operations
     */
    public List<MetricQueryResult> queryByTypeAndMessageId(String type, String messageId) {
        String key = type + "_" + messageId;
        List<Long> ids = typeMessageIndex.get(key);
        return convertToResults(ids);
    }
    
    public List<MetricQueryResult> queryByTypeMessageIdAndVersion(String type, String messageId, int version) {
        String key = type + "_" + messageId + "_V" + version;
        List<Long> ids = typeMessageVersionIndex.get(key);
        return convertToResults(ids);
    }
    
    public List<MetricQueryResult> queryByTypeMessageIdVersionAndSource(String type, String messageId, int version, String source) {
        String key = type + "_" + messageId + "_V" + version + "_" + source;
        List<Long> ids = typeMessageVersionSourceIndex.get(key);
        return convertToResults(ids);
    }
    
    public List<MetricQueryResult> queryByTypeAndTimestamp(String type, long timestamp) {
        String key = type + "_" + timestamp;
        List<Long> ids = typeTimestampIndex.get(key);
        return convertToResults(ids);
    }
    
    public List<MetricQueryResult> queryByApplicationId(String applicationId) {
        Set<Long> ids = applicationIdIndex.get(applicationId);
        return convertToResults(ids != null ? new ArrayList<>(ids) : null);
    }
    
    private List<MetricQueryResult> convertToResults(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<MetricQueryResult> results = new ArrayList<>(ids.size());
        for (Long id : ids) {
            CompactMetricRecord compact = recordStorage.get(id);
            if (compact != null) {
                results.add(new MetricQueryResult(compact.getLine(), compact.getApplicationId()));
            }
        }
        return results;
    }
    
    /**
     * Get memory usage statistics
     */
    public ChronicleMapStats getStats() {
        return new ChronicleMapStats(
            recordStorage.size(),
            typeMessageIndex.size(),
            typeMessageVersionIndex.size(),
            typeMessageVersionSourceIndex.size(),
            typeTimestampIndex.size(),
            getOffHeapMemoryUsage()
        );
    }
    
    private long getOffHeapMemoryUsage() {
        // Estimate off-heap usage (this is approximate)
        return recordStorage.offHeapMemoryUsed() +
               typeMessageIndex.offHeapMemoryUsed() +
               typeMessageVersionIndex.offHeapMemoryUsed() +
               typeMessageVersionSourceIndex.offHeapMemoryUsed() +
               typeTimestampIndex.offHeapMemoryUsed();
    }
    
    /**
     * Close and cleanup resources
     */
    public void close() {
        recordStorage.close();
        typeMessageIndex.close();
        typeMessageVersionIndex.close();
        typeMessageVersionSourceIndex.close();
        typeTimestampIndex.close();
    }
}

/**
 * Compact metric record for efficient off-heap storage
 * Uses Chronicle Map's value interface for optimal serialization
 */
interface CompactMetricRecord extends Copyable<CompactMetricRecord> {
    
    // Required fields for queries
    String getApplicationId();
    void setApplicationId(String applicationId);
    
    String getMessageId();
    void setMessageId(String messageId);
    
    int getVersion();
    void setVersion(int version);
    
    String getSource();
    void setSource(String source);
    
    String getType();
    void setType(String type);
    
    String getAlternateId();
    void setAlternateId(String alternateId);
    
    String getEventStatus();
    void setEventStatus(String eventStatus);
    
    String getProcessingStatus();
    void setProcessingStatus(String processingStatus);
    
    long getTimestampInMillis();
    void setTimestampInMillis(long timestampInMillis);
    
    String getLine();
    void setLine(String line);
    
    // Factory method
    static CompactMetricRecord of(MetricRecord record) {
        CompactMetricRecord compact = Values.newHeapInstance(CompactMetricRecord.class);
        compact.setApplicationId(record.getApplicationId());
        compact.setMessageId(record.getMessageId());
        compact.setVersion(record.getVersion());
        compact.setSource(record.getSource());
        compact.setType(record.getType());
        compact.setAlternateId(record.getAlternateId());
        compact.setEventStatus(record.getEventStatus());
        compact.setProcessingStatus(record.getProcessingStatus());
        compact.setTimestampInMillis(record.getTimestampInMillis());
        compact.setLine(record.getLine());
        return compact;
    }
}

/**
 * Statistics for Chronicle Map implementation
 */
class ChronicleMapStats {
    private final long totalRecords;
    private final long typeMessageIndexSize;
    private final long typeMessageVersionIndexSize;
    private final long typeMessageVersionSourceIndexSize;
    private final long typeTimestampIndexSize;
    private final long offHeapMemoryUsageBytes;
    
    public ChronicleMapStats(long totalRecords, long typeMessageIndexSize, 
                           long typeMessageVersionIndexSize, long typeMessageVersionSourceIndexSize,
                           long typeTimestampIndexSize, long offHeapMemoryUsageBytes) {
        this.totalRecords = totalRecords;
        this.typeMessageIndexSize = typeMessageIndexSize;
        this.typeMessageVersionIndexSize = typeMessageVersionIndexSize;
        this.typeMessageVersionSourceIndexSize = typeMessageVersionSourceIndexSize;
        this.typeTimestampIndexSize = typeTimestampIndexSize;
        this.offHeapMemoryUsageBytes = offHeapMemoryUsageBytes;
    }
    
    // Getters
    public long getTotalRecords() { return totalRecords; }
    public long getTypeMessageIndexSize() { return typeMessageIndexSize; }
    public long getTypeMessageVersionIndexSize() { return typeMessageVersionIndexSize; }
    public long getTypeMessageVersionSourceIndexSize() { return typeMessageVersionSourceIndexSize; }
    public long getTypeTimestampIndexSize() { return typeTimestampIndexSize; }
    public long getOffHeapMemoryUsageBytes() { return offHeapMemoryUsageBytes; }
    
    public String getOffHeapMemoryUsageMB() {
        return String.format("%.2f MB", offHeapMemoryUsageBytes / 1024.0 / 1024.0);
    }
    
    @Override
    public String toString() {
        return String.format("ChronicleMapStats{totalRecords=%d, offHeapUsage=%s, indexes=[tm=%d, tmv=%d, tmvs=%d, tt=%d]}", 
                           totalRecords, getOffHeapMemoryUsageMB(), typeMessageIndexSize, 
                           typeMessageVersionIndexSize, typeMessageVersionSourceIndexSize, typeTimestampIndexSize);
    }
}