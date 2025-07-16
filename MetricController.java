import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST Controller for metric queries
 * Provides endpoints for the 4 different query patterns
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricController {
    
    private final MetricStorageService storageService;
    private final MetricParserService parserService;
    
    @Autowired
    public MetricController(MetricStorageService storageService, MetricParserService parserService) {
        this.storageService = storageService;
        this.parserService = parserService;
    }
    
    /**
     * Query 1: Type + MessageId
     * GET /api/metrics/search?type=<type>&messageId=<messageId>
     */
    @GetMapping("/search")
    public ResponseEntity<List<MetricQueryResult>> findByTypeAndMessageId(
            @RequestParam String type,
            @RequestParam String messageId) {
        
        List<MetricQueryResult> results = storageService.findByTypeAndMessageId(type, messageId);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Query 2: Type + MessageId + Version
     * GET /api/metrics/search/version?type=<type>&messageId=<messageId>&version=<version>
     */
    @GetMapping("/search/version")
    public ResponseEntity<List<MetricQueryResult>> findByTypeMessageIdAndVersion(
            @RequestParam String type,
            @RequestParam String messageId,
            @RequestParam int version) {
        
        List<MetricQueryResult> results = storageService.findByTypeMessageIdAndVersion(type, messageId, version);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Query 3: Type + MessageId + Version + Source
     * GET /api/metrics/search/source?type=<type>&messageId=<messageId>&version=<version>&source=<source>
     */
    @GetMapping("/search/source")
    public ResponseEntity<List<MetricQueryResult>> findByTypeMessageIdVersionAndSource(
            @RequestParam String type,
            @RequestParam String messageId,
            @RequestParam int version,
            @RequestParam String source) {
        
        List<MetricQueryResult> results = storageService.findByTypeMessageIdVersionAndSource(type, messageId, version, source);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Query 4: Type + Timestamp (exact match)
     * GET /api/metrics/search/timestamp?type=<type>&timestamp=<timestampInMillis>
     */
    @GetMapping("/search/timestamp")
    public ResponseEntity<List<MetricQueryResult>> findByTypeAndTimestamp(
            @RequestParam String type,
            @RequestParam long timestamp) {
        
        List<MetricQueryResult> results = storageService.findByTypeAndTimestamp(type, timestamp);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Bonus Query: Type + Timestamp Range
     * GET /api/metrics/search/timestamp-range?type=<type>&startTime=<start>&endTime=<end>
     */
    @GetMapping("/search/timestamp-range")
    public ResponseEntity<List<MetricQueryResult>> findByTypeAndTimestampRange(
            @RequestParam String type,
            @RequestParam long startTime,
            @RequestParam long endTime) {
        
        if (startTime > endTime) {
            return ResponseEntity.badRequest().build();
        }
        
        List<MetricQueryResult> results = storageService.findByTypeAndTimestampRange(type, startTime, endTime);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Add a single metric record (for testing purposes)
     * POST /api/metrics/add
     * Body: CSV line format
     */
    @PostMapping("/add")
    public ResponseEntity<String> addMetric(@RequestBody String metricLine) {
        try {
            parserService.processMetricLine(metricLine);
            return ResponseEntity.ok("Metric added successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing metric: " + e.getMessage());
        }
    }
    
    /**
     * Add multiple metric records in batch (for testing purposes)
     * POST /api/metrics/add-batch
     * Body: JSON array of CSV lines
     */
    @PostMapping("/add-batch")
    public ResponseEntity<String> addMetricsBatch(@RequestBody List<String> metricLines) {
        try {
            parserService.processMetricLinesBatch(metricLines);
            return ResponseEntity.ok("Batch processed successfully. Added " + metricLines.size() + " metrics");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing batch: " + e.getMessage());
        }
    }
    
    /**
     * Get storage statistics (for monitoring)
     * GET /api/metrics/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<StorageStats> getStorageStats() {
        StorageStats stats = storageService.getStorageStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Clear all data (for testing purposes)
     * DELETE /api/metrics/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearAllData() {
        storageService.clearAll();
        return ResponseEntity.ok("All data cleared successfully");
    }
    
    /**
     * Health check endpoint
     * GET /api/metrics/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Metric Service is running");
    }
}

/**
 * Error response for API errors
 */
class ErrorResponse {
    private String error;
    private String message;
    private long timestamp;
    
    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    public String getError() { return error; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}

/**
 * Global exception handler for the controller
 */
@ControllerAdvice
class MetricControllerAdvice {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse("INVALID_ARGUMENT", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.internalServerError().body(error);
    }
}