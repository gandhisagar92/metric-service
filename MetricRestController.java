import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST controller for metric queries
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricRestController {
    
    @Autowired
    private HighPerformanceMetricService metricService;
    
    /**
     * Query by type and messageId
     * GET /api/metrics/query?type=error&messageId=12345
     */
    @GetMapping("/query")
    public ResponseEntity<List<MetricQueryResult>> queryByTypeAndMessageId(
            @RequestParam String type,
            @RequestParam String messageId) {
        
        List<MetricQueryResult> results = metricService.queryByTypeAndMessageId(type, messageId);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Query by type, messageId and version
     * GET /api/metrics/query/version?type=error&messageId=12345&version=2
     */
    @GetMapping("/query/version")
    public ResponseEntity<List<MetricQueryResult>> queryByTypeMessageIdAndVersion(
            @RequestParam String type,
            @RequestParam String messageId,
            @RequestParam int version) {
        
        List<MetricQueryResult> results = metricService.queryByTypeMessageIdAndVersion(type, messageId, version);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Query by type, messageId, version and source
     * GET /api/metrics/query/source?type=error&messageId=12345&version=2&source=web
     */
    @GetMapping("/query/source")
    public ResponseEntity<List<MetricQueryResult>> queryByTypeMessageIdVersionAndSource(
            @RequestParam String type,
            @RequestParam String messageId,
            @RequestParam int version,
            @RequestParam String source) {
        
        List<MetricQueryResult> results = metricService.queryByTypeMessageIdVersionAndSource(type, messageId, version, source);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Query by type and timestamp
     * GET /api/metrics/query/timestamp?type=error&timestamp=1640995200000
     */
    @GetMapping("/query/timestamp")
    public ResponseEntity<List<MetricQueryResult>> queryByTypeAndTimestamp(
            @RequestParam String type,
            @RequestParam long timestamp) {
        
        List<MetricQueryResult> results = metricService.queryByTypeAndTimestamp(type, timestamp);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Query by application ID
     * GET /api/metrics/application/APP1
     */
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<MetricQueryResult>> queryByApplicationId(@PathVariable String applicationId) {
        List<MetricQueryResult> results = metricService.queryByApplicationId(applicationId);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Get service statistics
     * GET /api/metrics/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<MetricServiceStats> getStats() {
        MetricServiceStats stats = metricService.getStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Add a single metric record (for testing)
     * POST /api/metrics
     */
    @PostMapping
    public ResponseEntity<String> addMetricRecord(@RequestBody String metricLine) {
        try {
            MetricRecord record = HighPerformanceMetricService.parseMetricLine(metricLine);
            boolean added = metricService.addMetricRecord(record);
            return ResponseEntity.ok(added ? "Record added successfully" : "Duplicate record");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid metric line: " + e.getMessage());
        }
    }
}