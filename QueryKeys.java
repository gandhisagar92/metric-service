import java.util.Objects;

/**
 * Composite key classes for different query patterns.
 * These keys are used for efficient HashMap-based indexing.
 */
public class QueryKeys {
    
    /**
     * Key for queries by type and messageId
     */
    public static final class TypeMessageKey {
        private final String type;
        private final String messageId;
        private final int hashCode;
        
        public TypeMessageKey(String type, String messageId) {
            this.type = type;
            this.messageId = messageId;
            this.hashCode = Objects.hash(type, messageId);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TypeMessageKey that = (TypeMessageKey) obj;
            return Objects.equals(type, that.type) && Objects.equals(messageId, that.messageId);
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
        
        @Override
        public String toString() {
            return String.format("TypeMessageKey{type='%s', messageId='%s'}", type, messageId);
        }
    }
    
    /**
     * Key for queries by type, messageId and version
     */
    public static final class TypeMessageVersionKey {
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
        public int hashCode() {
            return hashCode;
        }
        
        @Override
        public String toString() {
            return String.format("TypeMessageVersionKey{type='%s', messageId='%s', version=%d}", 
                               type, messageId, version);
        }
    }
    
    /**
     * Key for queries by type, messageId, version and source
     */
    public static final class TypeMessageVersionSourceKey {
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
        public int hashCode() {
            return hashCode;
        }
        
        @Override
        public String toString() {
            return String.format("TypeMessageVersionSourceKey{type='%s', messageId='%s', version=%d, source='%s'}", 
                               type, messageId, version, source);
        }
    }
    
    /**
     * Key for queries by type and timestamp
     */
    public static final class TypeTimestampKey {
        private final String type;
        private final long timestamp;
        private final int hashCode;
        
        public TypeTimestampKey(String type, long timestamp) {
            this.type = type;
            this.timestamp = timestamp;
            this.hashCode = Objects.hash(type, timestamp);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TypeTimestampKey that = (TypeTimestampKey) obj;
            return timestamp == that.timestamp && Objects.equals(type, that.type);
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
        
        @Override
        public String toString() {
            return String.format("TypeTimestampKey{type='%s', timestamp=%d}", type, timestamp);
        }
    }
}