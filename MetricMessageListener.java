import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Message listener for IBM MQ to receive metric data
 * Processes incoming metric messages asynchronously for optimal throughput
 */
@Component
public class MetricMessageListener {
    
    private final MetricParserService parserService;
    
    @Autowired
    public MetricMessageListener(MetricParserService parserService) {
        this.parserService = parserService;
    }
    
    /**
     * Listen to metric messages from IBM MQ
     * Queue name should be configured in application.properties
     */
    @JmsListener(destination = "${metric.queue.name:METRIC.QUEUE}")
    public void receiveMetricMessage(String message) {
        if (!StringUtils.hasText(message)) {
            System.err.println("Received empty message, skipping...");
            return;
        }
        
        try {
            // Process the metric message asynchronously for better throughput
            parserService.processMetricLineAsync(message.trim())
                         .exceptionally(throwable -> {
                             System.err.println("Failed to process metric message: " + message + 
                                              ", Error: " + throwable.getMessage());
                             return null;
                         });
            
        } catch (Exception e) {
            System.err.println("Error processing metric message: " + message + ", Error: " + e.getMessage());
        }
    }
    
    /**
     * Listen to batch metric messages (if messages are sent in batches)
     * This queue would contain multiple metric lines separated by newlines
     */
    @JmsListener(destination = "${metric.batch.queue.name:METRIC.BATCH.QUEUE}")
    public void receiveMetricBatchMessage(String batchMessage) {
        if (!StringUtils.hasText(batchMessage)) {
            System.err.println("Received empty batch message, skipping...");
            return;
        }
        
        try {
            // Split the batch message into individual lines
            String[] lines = batchMessage.split("\\r?\\n");
            java.util.List<String> metricLines = java.util.Arrays.asList(lines);
            
            // Process the batch asynchronously
            parserService.processMetricLinesBatchAsync(metricLines)
                         .exceptionally(throwable -> {
                             System.err.println("Failed to process metric batch, Error: " + throwable.getMessage());
                             return null;
                         });
            
        } catch (Exception e) {
            System.err.println("Error processing metric batch message, Error: " + e.getMessage());
        }
    }
}