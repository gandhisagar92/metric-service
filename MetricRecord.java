import java.util.Objects;

/**
 * Immutable metric record class optimized for memory efficiency
 */
public final class MetricRecord {
    private final String applicationId;
    private final String messageId;
    private final int version;
    private final String source;
    private final String type;
    private final String alternateId;
    private final String eventStatus;
    private final String processingStatus;
    private final long timestampInMillis;
    private final String line;
    
    // Pre-computed hash for performance
    private final int hashCode;
    
    public MetricRecord(String applicationId, String messageId, int version, 
                       String source, String type, String alternateId,
                       String eventStatus, String processingStatus, 
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
        this.hashCode = computeHashCode();
    }
    
    private int computeHashCode() {
        int result = 17;
        result = 31 * result + (applicationId != null ? applicationId.hashCode() : 0);
        result = 31 * result + (messageId != null ? messageId.hashCode() : 0);
        result = 31 * result + version;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (alternateId != null ? alternateId.hashCode() : 0);
        result = 31 * result + (Long.hashCode(timestampInMillis));
        return result;
    }
    
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
               Objects.equals(alternateId, that.alternateId);
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
    public String toString() {
        return String.format("MetricRecord{app=%s, msgId=%s, version=%d, type=%s, timestamp=%d}", 
                           applicationId, messageId, version, type, timestampInMillis);
    }
}