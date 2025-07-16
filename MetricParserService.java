import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service for parsing metric data from CSV format to MetricRecord objects
 * Optimized for high-throughput processing
 */
@Service
public class MetricParserService {
    
    private final MetricStorageService storageService;
    
    // Thread pool for async processing
    private final Executor executor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );
    
    @Autowired
    public MetricParserService(MetricStorageService storageService) {
        this.storageService = storageService;
    }
    
    /**
     * Parse a single metric line synchronously
     * Format: "applicationId,messageId,version,source,type,alternateId,eventStatus,processingStatus,timestampInMillis,line"
     */
    public MetricRecord parseMetricLine(String metricLine) {
        if (metricLine == null || metricLine.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric line cannot be null or empty");
        }
        
        String[] parts = metricLine.split(",", 10); // Limit to 10 parts to handle commas in 'line' field
        
        if (parts.length != 10) {
            throw new IllegalArgumentException("Invalid metric format. Expected 10 fields, got " + parts.length);
        }
        
        try {
            String applicationId = parts[0].trim();
            String messageId = parts[1].trim();
            int version = Integer.parseInt(parts[2].trim());
            String source = parts[3].trim();
            String type = parts[4].trim();
            String alternateId = parts[5].trim();
            String eventStatus = parts[6].trim();
            String processingStatus = parts[7].trim();
            long timestampInMillis = Long.parseLong(parts[8].trim());
            String line = parts[9]; // Don't trim this as it might contain meaningful whitespace
            
            return new MetricRecord(applicationId, messageId, version, source, type, 
                                  alternateId, eventStatus, processingStatus, timestampInMillis, line);
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in metric line: " + metricLine, e);
        }
    }
    
    /**
     * Parse and store a metric line synchronously
     */
    public void processMetricLine(String metricLine) {
        MetricRecord record = parseMetricLine(metricLine);
        storageService.addMetricRecord(record);
    }
    
    /**
     * Parse and store a metric line asynchronously for better throughput
     */
    public CompletableFuture<Void> processMetricLineAsync(String metricLine) {
        return CompletableFuture.runAsync(() -> {
            try {
                processMetricLine(metricLine);
            } catch (Exception e) {
                // Log error but don't fail the entire batch
                System.err.println("Failed to process metric line: " + metricLine + ", Error: " + e.getMessage());
            }
        }, executor);
    }
    
    /**
     * Parse and store multiple metric lines in batch for optimal performance
     */
    public void processMetricLinesBatch(java.util.List<String> metricLines) {
        java.util.List<MetricRecord> records = new java.util.ArrayList<>();
        
        for (String line : metricLines) {
            try {
                records.add(parseMetricLine(line));
            } catch (Exception e) {
                // Log error but continue processing other lines
                System.err.println("Failed to parse metric line: " + line + ", Error: " + e.getMessage());
            }
        }
        
        if (!records.isEmpty()) {
            storageService.addMetricRecords(records);
        }
    }
    
    /**
     * Parse and store multiple metric lines asynchronously in batch
     */
    public CompletableFuture<Void> processMetricLinesBatchAsync(java.util.List<String> metricLines) {
        return CompletableFuture.runAsync(() -> processMetricLinesBatch(metricLines), executor);
    }
}

/**
 * Exception for metric parsing errors
 */
class MetricParsingException extends RuntimeException {
    public MetricParsingException(String message) {
        super(message);
    }
    
    public MetricParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}