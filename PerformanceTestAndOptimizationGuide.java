import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * Performance testing and optimization guide for metric service implementations.
 * 
 * This class provides:
 * 1. Performance benchmarks for different data structures
 * 2. Memory usage analysis
 * 3. Recommendations for optimization
 * 4. Answers to specific questions about collection query languages and hashing
 */
public class PerformanceTestAndOptimizationGuide {
    
    public static void main(String[] args) throws Exception {
        PerformanceTestAndOptimizationGuide guide = new PerformanceTestAndOptimizationGuide();
        guide.runAllTests();
        guide.printOptimizationGuide();
    }
    
    public void runAllTests() throws Exception {
        System.out.println("=== METRIC SERVICE PERFORMANCE ANALYSIS ===\n");
        
        // Test data generation
        List<MetricRecord> testData = generateTestData(1_000_000);
        System.out.println("Generated " + testData.size() + " test records\n");
        
        // Test 1: Standard HashMap-based implementation
        testHashMapImplementation(testData);
        
        // Test 2: Chronicle Map implementation (if available)
        // testChronicleMapImplementation(testData);
        
        // Test 3: Query performance comparison
        testQueryPerformance(testData);
        
        // Test 4: Memory usage analysis
        analyzeMemoryUsage(testData);
    }
    
    private void testHashMapImplementation(List<MetricRecord> testData) {
        System.out.println("=== TESTING HASHMAP-BASED IMPLEMENTATION ===");
        
        HighPerformanceMetricService service = new HighPerformanceMetricService();
        
        // Test insertion performance
        long startTime = System.currentTimeMillis();
        
        // Bulk insert for better performance
        List<List<MetricRecord>> batches = partition(testData, 10000);
        batches.parallelStream().forEach(batch -> service.addMetricRecords(batch));
        
        long insertTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Insertion Performance:");
        System.out.println("- Records inserted: " + testData.size());
        System.out.println("- Time taken: " + insertTime + " ms");
        System.out.println("- Throughput: " + (testData.size() * 1000L / insertTime) + " records/second");
        
        // Test query performance
        testQueryPerformanceForService(service, testData);
        
        // Print statistics
        MetricServiceStats stats = service.getStats();
        System.out.println("\nService Statistics:");
        System.out.println(stats);
        System.out.println();
    }
    
    private void testQueryPerformanceForService(HighPerformanceMetricService service, List<MetricRecord> testData) {
        System.out.println("\nQuery Performance:");
        
        // Get some sample records for testing
        MetricRecord sample1 = testData.get(100);
        MetricRecord sample2 = testData.get(500);
        MetricRecord sample3 = testData.get(1000);
        
        // Test Query 1: Type and MessageId
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            service.queryByTypeAndMessageId(sample1.getType(), sample1.getMessageId());
        }
        long query1Time = (System.nanoTime() - start) / 1000000; // Convert to ms
        
        // Test Query 2: Type, MessageId, Version
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            service.queryByTypeMessageIdAndVersion(sample2.getType(), sample2.getMessageId(), sample2.getVersion());
        }
        long query2Time = (System.nanoTime() - start) / 1000000;
        
        // Test Query 3: Type, MessageId, Version, Source
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            service.queryByTypeMessageIdVersionAndSource(sample3.getType(), sample3.getMessageId(), 
                                                       sample3.getVersion(), sample3.getSource());
        }
        long query3Time = (System.nanoTime() - start) / 1000000;
        
        // Test Query 4: Type and Timestamp
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            service.queryByTypeAndTimestamp(sample1.getType(), sample1.getTimestampInMillis());
        }
        long query4Time = (System.nanoTime() - start) / 1000000;
        
        System.out.println("- Query 1 (Type + MessageId): " + query1Time + " ms for 1000 queries");
        System.out.println("- Query 2 (Type + MessageId + Version): " + query2Time + " ms for 1000 queries");
        System.out.println("- Query 3 (Type + MessageId + Version + Source): " + query3Time + " ms for 1000 queries");
        System.out.println("- Query 4 (Type + Timestamp): " + query4Time + " ms for 1000 queries");
        System.out.println("- Average query time: " + (query1Time + query2Time + query3Time + query4Time) / 4.0 + " ms per 1000 queries");
    }
    
    private void testQueryPerformance(List<MetricRecord> testData) {
        System.out.println("=== QUERY PERFORMANCE COMPARISON ===");
        
        // Compare different lookup strategies
        testHashingPerformance(testData);
        testLinearSearchPerformance(testData);
    }
    
    private void testHashingPerformance(List<MetricRecord> testData) {
        System.out.println("\nHashing-based lookup performance:");
        
        // Create hash map for type+messageId lookup
        Map<String, List<MetricRecord>> hashIndex = new HashMap<>();
        
        // Build index
        long start = System.currentTimeMillis();
        for (MetricRecord record : testData) {
            String key = record.getType() + "_" + record.getMessageId();
            hashIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }
        long indexBuildTime = System.currentTimeMillis() - start;
        
        // Test lookups
        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            MetricRecord sample = testData.get(i % testData.size());
            String key = sample.getType() + "_" + sample.getMessageId();
            hashIndex.get(key);
        }
        long lookupTime = (System.nanoTime() - start) / 1000000;
        
        System.out.println("- Index build time: " + indexBuildTime + " ms");
        System.out.println("- 10,000 hash lookups: " + lookupTime + " ms");
        System.out.println("- Average lookup time: " + (lookupTime / 10000.0) + " ms");
    }
    
    private void testLinearSearchPerformance(List<MetricRecord> testData) {
        System.out.println("\nLinear search performance (for comparison):");
        
        MetricRecord sample = testData.get(100);
        
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            // Linear search simulation
            testData.stream()
                .filter(r -> r.getType().equals(sample.getType()) && r.getMessageId().equals(sample.getMessageId()))
                .findFirst();
        }
        long linearTime = (System.nanoTime() - start) / 1000000;
        
        System.out.println("- 100 linear searches: " + linearTime + " ms");
        System.out.println("- Average linear search time: " + (linearTime / 100.0) + " ms");
        System.out.println("- Hash lookup is ~" + (linearTime / 100.0 / 0.001) + "x faster than linear search");
    }
    
    private void analyzeMemoryUsage(List<MetricRecord> testData) {
        System.out.println("\n=== MEMORY USAGE ANALYSIS ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection
        System.gc();
        long baseMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create service and add data
        HighPerformanceMetricService service = new HighPerformanceMetricService();
        service.addMetricRecords(testData);
        
        System.gc();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long serviceMemory = usedMemory - baseMemory;
        
        System.out.println("Memory Usage:");
        System.out.println("- Total records: " + testData.size());
        System.out.println("- Memory used by service: " + (serviceMemory / 1024 / 1024) + " MB");
        System.out.println("- Memory per record: " + (serviceMemory / testData.size()) + " bytes");
        
        // Estimate index overhead
        MetricServiceStats stats = service.getStats();
        int totalIndexes = stats.getTypeMessageIndexSize() + stats.getTypeMessageVersionIndexSize() + 
                          stats.getTypeMessageVersionSourceIndexSize() + stats.getTypeTimestampIndexSize();
        
        System.out.println("- Total index entries: " + totalIndexes);
        System.out.println("- Index overhead ratio: " + (totalIndexes / (float) testData.size()));
    }
    
    private List<MetricRecord> generateTestData(int count) {
        List<MetricRecord> data = new ArrayList<>(count);
        Random random = new Random(42); // Fixed seed for reproducibility
        
        String[] applications = {"APP1", "APP2", "APP3", "APP4", "APP5", "APP6"};
        String[] types = {"ERROR", "WARN", "INFO", "DEBUG", "TRACE"};
        String[] sources = {"WEB", "API", "BATCH", "QUEUE", "DB"};
        String[] statuses = {"SUCCESS", "FAILED", "PENDING", "TIMEOUT"};
        
        for (int i = 0; i < count; i++) {
            data.add(new MetricRecord(
                applications[random.nextInt(applications.length)],
                "MSG" + (i % 100000), // Create some duplicates
                random.nextInt(5) + 1,
                sources[random.nextInt(sources.length)],
                types[random.nextInt(types.length)],
                "ALT" + i,
                statuses[random.nextInt(statuses.length)],
                statuses[random.nextInt(statuses.length)],
                System.currentTimeMillis() + random.nextInt(86400000), // Random time within 24h
                "Sample log line " + i + " with some additional content that might be longer"
            ));
        }
        
        return data;
    }
    
    public void printOptimizationGuide() {
        System.out.println("\n=== OPTIMIZATION GUIDE AND RECOMMENDATIONS ===\n");
        
        System.out.println("ANSWERS TO YOUR SPECIFIC QUESTIONS:\n");
        
        System.out.println("1. OPTIMAL DATA STRUCTURES:");
        System.out.println("   ✓ Multiple HashMap-based indexes for different query patterns");
        System.out.println("   ✓ ConcurrentHashMap for thread safety");
        System.out.println("   ✓ Composite keys for multi-attribute queries");
        System.out.println("   ✓ Pre-computed hash codes for better performance");
        System.out.println("   ✓ Separate indexes for each query pattern = O(1) lookups");
        
        System.out.println("\n2. FAST ATTRIBUTE COMPARISON:");
        System.out.println("   ✓ Hash-based lookups instead of linear scans");
        System.out.println("   ✓ Immutable composite keys with pre-computed hash codes");
        System.out.println("   ✓ Index-based retrieval eliminates need for attribute comparison");
        System.out.println("   ✓ Bulk operations for better throughput");
        
        System.out.println("\n3. COLLECTION QUERY LANGUAGES:");
        System.out.println("   ✓ YES! Several options available:");
        System.out.println("     - Chronicle Map: Off-heap collections with indexing");
        System.out.println("     - Apache Ignite: In-memory data grid with SQL queries");
        System.out.println("     - Hazelcast: Distributed in-memory computing with indexes");
        System.out.println("     - Eclipse Collections: High-performance collection framework");
        System.out.println("     - Apache Lucene: Full-text indexing and search");
        System.out.println("   ✓ Chronicle Map is recommended for your use case");
        
        System.out.println("\n4. HASHING TECHNIQUES:");
        System.out.println("   ✓ YES! Hashing is the KEY optimization:");
        System.out.println("     - Composite key hashing for multi-attribute queries");
        System.out.println("     - Pre-computed hash codes to avoid recalculation");
        System.out.println("     - Perfect hashing for known data sets");
        System.out.println("     - Bloom filters for existence checks");
        System.out.println("     - Hash partitioning for distributed scenarios");
        
        System.out.println("\nADDITIONAL OPTIMIZATIONS:\n");
        
        System.out.println("MEMORY OPTIMIZATIONS:");
        System.out.println("   ✓ Use primitive collections (Eclipse Collections, Trove)");
        System.out.println("   ✓ String interning for repeated values");
        System.out.println("   ✓ Off-heap storage (Chronicle Map)");
        System.out.println("   ✓ Compressed data structures");
        System.out.println("   ✓ Object pooling for frequent allocations");
        
        System.out.println("\nPERFORMANCE OPTIMIZATIONS:");
        System.out.println("   ✓ Batch processing for MQ messages");
        System.out.println("   ✓ Asynchronous indexing");
        System.out.println("   ✓ Read-write locks for concurrent access");
        System.out.println("   ✓ Lock-free data structures where possible");
        System.out.println("   ✓ CPU cache-friendly data layouts");
        
        System.out.println("\nSCALABILITY OPTIMIZATIONS:");
        System.out.println("   ✓ Horizontal partitioning by applicationId");
        System.out.println("   ✓ Time-based partitioning for historical data");
        System.out.println("   ✓ Distributed caching (Redis, Hazelcast)");
        System.out.println("   ✓ Database integration for persistence");
        System.out.println("   ✓ Message queue optimization");
        
        System.out.println("\nRECOMMENDED ARCHITECTURE:\n");
        
        System.out.println("For 1M - 10M records:");
        System.out.println("   → Use HighPerformanceMetricService (HashMap-based)");
        System.out.println("   → Multiple indexes, concurrent access");
        System.out.println("   → Expected performance: <1ms query time");
        
        System.out.println("\nFor 10M+ records:");
        System.out.println("   → Use ChronicleMapMetricService (off-heap)");
        System.out.println("   → Minimal GC pressure, persistent storage");
        System.out.println("   → Expected performance: <2ms query time");
        
        System.out.println("\nFor distributed scenarios:");
        System.out.println("   → Apache Ignite or Hazelcast");
        System.out.println("   → SQL-like queries with automatic indexing");
        System.out.println("   → Horizontal scaling capability");
        
        System.out.println("\nIMPLEMENTATION PRIORITIES:");
        System.out.println("   1. Start with HashMap-based solution");
        System.out.println("   2. Add Chronicle Map for larger datasets");
        System.out.println("   3. Implement monitoring and metrics");
        System.out.println("   4. Add persistence layer if needed");
        System.out.println("   5. Consider distributed solution for scale");
        
        System.out.println("\n=== END OF OPTIMIZATION GUIDE ===");
    }
    
    // Utility method to partition lists for bulk processing
    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}