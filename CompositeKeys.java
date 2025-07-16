import java.util.Objects;

/**
 * Composite key classes for efficient indexing based on query patterns
 */
public class CompositeKeys {
    
    /**
     * Key for queries: Type + MessageId
     */
    public static class TypeMessageKey {
        private final String type;
        private final String messageId;
        private final int hashCode;
        
        public TypeMessageKey(String type, String messageId) {
            this.type = type;
            this.messageId = messageId;
            this.hashCode = Objects.hash(type, messageId);
        }
        
        public String getType() { return type; }
        public String getMessageId() { return messageId; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TypeMessageKey that = (TypeMessageKey) obj;
            return Objects.equals(type, that.type) &&
                   Objects.equals(messageId, that.messageId);
        }
        
        @Override
        public int hashCode() { return hashCode; }
    }
    
    /**
     * Key for queries: Type + MessageId + Version
     */
    public static class TypeMessageVersionKey {
        private final String type;
        private final String messageId;
        private final int version;
        private final int hashCode;
        
        public TypeMessageVersionKey(String type, String messageId, int version) {
            this.type = type;
            this.messageId = messageId;
            this.version = version;
            this.hashCode = Objects.hash(type, messageId, version);
        }
        
        public String getType() { return type; }
        public String getMessageId() { return messageId; }
        public int getVersion() { return version; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TypeMessageVersionKey that = (TypeMessageVersionKey) obj;
            return version == that.version &&
                   Objects.equals(type, that.type) &&
                   Objects.equals(messageId, that.messageId);
        }
        
        @Override
        public int hashCode() { return hashCode; }
    }
    
    /**
     * Key for queries: Type + MessageId + Version + Source
     */
    public static class TypeMessageVersionSourceKey {
        private final String type;
        private final String messageId;
        private final int version;
        private final String source;
        private final int hashCode;
        
        public TypeMessageVersionSourceKey(String type, String messageId, int version, String source) {
            this.type = type;
            this.messageId = messageId;
            this.version = version;
            this.source = source;
            this.hashCode = Objects.hash(type, messageId, version, source);
        }
        
        public String getType() { return type; }
        public String getMessageId() { return messageId; }
        public int getVersion() { return version; }
        public String getSource() { return source; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TypeMessageVersionSourceKey that = (TypeMessageVersionSourceKey) obj;
            return version == that.version &&
                   Objects.equals(type, that.type) &&
                   Objects.equals(messageId, that.messageId) &&
                   Objects.equals(source, that.source);
        }
        
        @Override
        public int hashCode() { return hashCode; }
    }
    
    /**
     * Key for queries: Type + Timestamp (with time range support)
     */
    public static class TypeTimestampKey {
        private final String type;
        private final long timestampInMillis;
        private final int hashCode;
        
        public TypeTimestampKey(String type, long timestampInMillis) {
            this.type = type;
            this.timestampInMillis = timestampInMillis;
            this.hashCode = Objects.hash(type, timestampInMillis);
        }
        
        public String getType() { return type; }
        public long getTimestampInMillis() { return timestampInMillis; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TypeTimestampKey that = (TypeTimestampKey) obj;
            return timestampInMillis == that.timestampInMillis &&
                   Objects.equals(type, that.type);
        }
        
        @Override
        public int hashCode() { return hashCode; }
    }
}