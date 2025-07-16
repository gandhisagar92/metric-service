import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for the Metric Service
 * Tests performance, concurrency, and correctness
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.jms.ibm.mq.host=localhost",
    "metric.queue.name=TEST.QUEUE"
})
public class MetricServiceTest {
    
    private MetricStorageService storageService;
    private MetricParserService parserService;
    
    @BeforeEach
    void setUp() {
        storageService = new MetricStorageService();
        parserService = new MetricParserService(storageService);
        
        // Clear any existing data
        storageService.clearAll();
    }
    
    @Test
    void testBasicMetricParsing() {
        String metricLine = "APP1,MSG123,1,SOURCE1,ERROR,ALT456,ACTIVE,PROCESSED,1640995200000,Error processing request";
        
        MetricRecord record = parserService.parseMetricLine(metricLine);
        
        assertEquals("APP1", record.getApplicationId());
        assertEquals("MSG123", record.getMessageId());
        assertEquals(1, record.getVersion());
        assertEquals("SOURCE1", record.getSource());
        assertEquals("ERROR", record.getType());
        assertEquals("ALT456", record.getAlternateId());
        assertEquals("ACTIVE", record.getEventStatus());
        assertEquals("PROCESSED", record.getProcessingStatus());
        assertEquals(1640995200000L, record.getTimestampInMillis());
        assertEquals("Error processing request", record.getLine());
    }
    
    @Test
    void testQuery1_TypeAndMessageId() {
        // Add test data
        addTestMetrics();
        
        // Query by type and messageId
        List<MetricQueryResult> results = storageService.findByTypeAndMessageId("ERROR", "MSG123");
        
        assertFalse(results.isEmpty());
        assertEquals("APP1", results.get(0).getApplicationId());
        assertTrue(results.get(0).getLine().contains("Error"));
    }
    
    @Test
    void testQuery2_TypeMessageIdAndVersion() {
        // Add test data
        addTestMetrics();
        
        // Query by type, messageId, and version
        List<MetricQueryResult> results = storageService.findByTypeMessageIdAndVersion("ERROR", "MSG123", 1);
        
        assertFalse(results.isEmpty());
        assertEquals("APP1", results.get(0).getApplicationId());
    }
    
    @Test
    void testQuery3_TypeMessageIdVersionAndSource() {
        // Add test data
        addTestMetrics();
        
        // Query by type, messageId, version, and source
        List<MetricQueryResult> results = storageService.findByTypeMessageIdVersionAndSource("ERROR", "MSG123", 1, "SOURCE1");
        
        assertFalse(results.isEmpty());
        assertEquals("APP1", results.get(0).getApplicationId());
    }
    
    @Test
    void testQuery4_TypeAndTimestamp() {
        // Add test data
        addTestMetrics();
        
        // Query by type and timestamp
        List<MetricQueryResult> results = storageService.findByTypeAndTimestamp("ERROR", 1640995200000L);
        
        assertFalse(results.isEmpty());
        assertEquals("APP1", results.get(0).getApplicationId());
    }
    
    @Test
    void testTimestampRangeQuery() {
        // Add test data with different timestamps
        addTestMetricsWithTimestamps();
        
        // Query by type and timestamp range
        List<MetricQueryResult> results = storageService.findByTypeAndTimestampRange("ERROR", 1640995200000L, 1640995300000L);
        
        assertFalse(results.isEmpty());
        assertTrue(results.size() >= 2); // Should find multiple records in range
    }
    
    @Test
    void testBatchProcessing() {
        List<String> metricLines = new ArrayList<>();
        
        // Create 1000 test metrics
        for (int i = 0; i < 1000; i++) {
            metricLines.add(String.format("APP%d,MSG%d,%d,SOURCE%d,ERROR,ALT%d,ACTIVE,PROCESSED,%d,Test message %d",
                i % 6 + 1, i, i % 10 + 1, i % 3 + 1, i, 1640995200000L + i, i));
        }
        
        // Process batch
        long startTime = System.currentTimeMillis();
        parserService.processMetricLinesBatch(metricLines);
        long endTime = System.currentTimeMillis();
        
        // Verify all records were added
        StorageStats stats = storageService.getStorageStats();
        assertEquals(1000, stats.getTotalRecords());
        
        System.out.println("Batch processing time for 1000 records: " + (endTime - startTime) + " ms");
    }
    
    @Test
    void testPerformanceBenchmark() {
        int numRecords = 10000;
        List<String> metricLines = new ArrayList<>();
        
        // Generate test data
        for (int i = 0; i < numRecords; i++) {
            metricLines.add(String.format("APP%d,MSG%d,%d,SOURCE%d,ERROR,ALT%d,ACTIVE,PROCESSED,%d,Performance test record %d",
                i % 6 + 1, i, i % 10 + 1, i % 3 + 1, i, 1640995200000L + i, i));
        }
        
        // Measure insert performance
        long insertStart = System.currentTimeMillis();
        parserService.processMetricLinesBatch(metricLines);
        long insertEnd = System.currentTimeMillis();
        
        // Measure query performance
        long queryStart = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            storageService.findByTypeAndMessageId("ERROR", "MSG" + (i % numRecords));
        }
        long queryEnd = System.currentTimeMillis();
        
        System.out.println("Insert performance: " + numRecords + " records in " + (insertEnd - insertStart) + " ms");
        System.out.println("Query performance: 1000 queries in " + (queryEnd - queryStart) + " ms");
        System.out.println("Average query time: " + ((queryEnd - queryStart) / 1000.0) + " ms per query");
        
        // Performance assertions
        assertTrue((insertEnd - insertStart) < 5000, "Insert should complete within 5 seconds");
        assertTrue((queryEnd - queryStart) < 1000, "1000 queries should complete within 1 second");
    }
    
    @Test
    void testConcurrentAccess() throws ExecutionException, InterruptedException {
        int numThreads = 10;
        int recordsPerThread = 100;
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Create multiple threads adding data concurrently
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int i = 0; i < recordsPerThread; i++) {
                    String metricLine = String.format("APP%d,MSG%d_%d,%d,SOURCE%d,ERROR,ALT%d,ACTIVE,PROCESSED,%d,Concurrent test %d-%d",
                        threadId, threadId, i, i % 10 + 1, threadId % 3 + 1, threadId * 1000 + i, 
                        1640995200000L + threadId * 1000 + i, threadId, i);
                    parserService.processMetricLine(metricLine);
                }
            });
            futures.add(future);
        }
        
        // Wait for all threads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        
        // Verify all records were added correctly
        StorageStats stats = storageService.getStorageStats();
        assertEquals(numThreads * recordsPerThread, stats.getTotalRecords());
        
        // Test concurrent queries
        List<CompletableFuture<List<MetricQueryResult>>> queryFutures = new ArrayList<>();
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            CompletableFuture<List<MetricQueryResult>> queryFuture = CompletableFuture.supplyAsync(() -> 
                storageService.findByTypeAndMessageId("ERROR", "MSG" + threadId + "_0")
            );
            queryFutures.add(queryFuture);
        }
        
        // Verify all queries complete successfully
        for (CompletableFuture<List<MetricQueryResult>> future : queryFutures) {
            List<MetricQueryResult> results = future.get();
            assertFalse(results.isEmpty());
        }
    }
    
    @Test
    void testMemoryEfficiency() {
        // Test with a large number of records to verify memory efficiency
        int numRecords = 50000;
        
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Add records
        for (int i = 0; i < numRecords; i++) {
            String metricLine = String.format("APP%d,MSG%d,%d,SOURCE%d,ERROR,ALT%d,ACTIVE,PROCESSED,%d,Memory test record %d",
                i % 6 + 1, i, i % 10 + 1, i % 3 + 1, i, 1640995200000L + i, i);
            parserService.processMetricLine(metricLine);
        }
        
        System.gc(); // Suggest garbage collection
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        System.out.println("Memory used for " + numRecords + " records: " + (memoryUsed / 1024 / 1024) + " MB");
        System.out.println("Average memory per record: " + (memoryUsed / numRecords) + " bytes");
        
        // Verify reasonable memory usage (should be less than 500 bytes per record including indexes)
        assertTrue((memoryUsed / numRecords) < 500, "Memory usage per record should be reasonable");
    }
    
    @Test
    void testInvalidDataHandling() {
        // Test with invalid metric lines
        assertThrows(IllegalArgumentException.class, () -> {
            parserService.parseMetricLine("INVALID,DATA"); // Not enough fields
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            parserService.parseMetricLine("APP1,MSG123,INVALID_VERSION,SOURCE1,ERROR,ALT456,ACTIVE,PROCESSED,1640995200000,Error"); // Invalid version
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            parserService.parseMetricLine("APP1,MSG123,1,SOURCE1,ERROR,ALT456,ACTIVE,PROCESSED,INVALID_TIMESTAMP,Error"); // Invalid timestamp
        });
    }
    
    private void addTestMetrics() {
        String[] testMetrics = {
            "APP1,MSG123,1,SOURCE1,ERROR,ALT456,ACTIVE,PROCESSED,1640995200000,Error processing request",
            "APP2,MSG456,2,SOURCE2,INFO,ALT789,ACTIVE,PROCESSED,1640995210000,Info message",
            "APP3,MSG789,1,SOURCE1,WARNING,ALT123,ACTIVE,PROCESSED,1640995220000,Warning occurred",
            "APP1,MSG123,2,SOURCE1,ERROR,ALT456,ACTIVE,PROCESSED,1640995230000,Another error",
            "APP4,MSG321,1,SOURCE3,ERROR,ALT654,ACTIVE,PROCESSED,1640995240000,Critical error"
        };
        
        for (String metric : testMetrics) {
            parserService.processMetricLine(metric);
        }
    }
    
    private void addTestMetricsWithTimestamps() {
        String[] testMetrics = {
            "APP1,MSG123,1,SOURCE1,ERROR,ALT456,ACTIVE,PROCESSED,1640995200000,Error at time 1",
            "APP1,MSG456,1,SOURCE1,ERROR,ALT789,ACTIVE,PROCESSED,1640995250000,Error at time 2",
            "APP1,MSG789,1,SOURCE1,ERROR,ALT123,ACTIVE,PROCESSED,1640995300000,Error at time 3",
            "APP1,MSG321,1,SOURCE1,ERROR,ALT654,ACTIVE,PROCESSED,1640995350000,Error at time 4"
        };
        
        for (String metric : testMetrics) {
            parserService.processMetricLine(metric);
        }
    }
}