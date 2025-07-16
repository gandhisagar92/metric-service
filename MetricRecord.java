import java.util.Objects;

/**
 * Compact representation of metric data with minimal memory overhead
 */
public class MetricRecord {
    private final String applicationId;
    private final String messageId;
    private final int version;  // Using int instead of Integer for memory efficiency
    private final String source;
    private final String type;
    private final String alternateId;
    private final String eventStatus;
    private final String processingStatus;
    private final long timestampInMillis;  // Using long instead of Long
    private final String line;
    
    // Pre-computed hash for faster lookups
    private final int hashCode;
    
    public MetricRecord(String applicationId, String messageId, int version, String source, 
                       String type, String alternateId, String eventStatus, String processingStatus,
                       long timestampInMillis, String line) {
        this.applicationId = applicationId;
        this.messageId = messageId;
        this.version = version;
        this.source = source;
        this.type = type;
        this.alternateId = alternateId;
        this.eventStatus = eventStatus;
        this.processingStatus = processingStatus;
        this.timestampInMillis = timestampInMillis;
        this.line = line;
        
        // Pre-compute hash code for better performance
        this.hashCode = Objects.hash(applicationId, messageId, version, source, type, 
                                   alternateId, eventStatus, processingStatus, timestampInMillis);
    }
    
    // Getters
    public String getApplicationId() { return applicationId; }
    public String getMessageId() { return messageId; }
    public int getVersion() { return version; }
    public String getSource() { return source; }
    public String getType() { return type; }
    public String getAlternateId() { return alternateId; }
    public String getEventStatus() { return eventStatus; }
    public String getProcessingStatus() { return processingStatus; }
    public long getTimestampInMillis() { return timestampInMillis; }
    public String getLine() { return line; }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MetricRecord that = (MetricRecord) obj;
        return version == that.version &&
               timestampInMillis == that.timestampInMillis &&
               Objects.equals(applicationId, that.applicationId) &&
               Objects.equals(messageId, that.messageId) &&
               Objects.equals(source, that.source) &&
               Objects.equals(type, that.type) &&
               Objects.equals(alternateId, that.alternateId) &&
               Objects.equals(eventStatus, that.eventStatus) &&
               Objects.equals(processingStatus, that.processingStatus);
    }
}