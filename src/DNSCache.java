import java.util.HashMap;

// DNSCache stores DNS answers and handels TTL expiration
public class DNSCache {


    // store values as key value, ket DNSQuestion, Value DNSRecord
    private final HashMap<DNSQuestion, CacheEntry> cache = new HashMap<>();


    // now I look for cached record if it was found or not
    public CacheEntry get(DNSQuestion question) {
        CacheEntry record = cache.get(question);

        // check if null
        if (record == null) {
            return null;
        }

        // check if expired
        if (record.isExpired()) {
            cache.remove(question);
            return null;
        }

        return record;
    }

    // store record in chase
    public void put(DNSQuestion question, byte[] responseBytes, long ttlSeconds) {
        cache.put(question, new CacheEntry(responseBytes, ttlSeconds));
    }

    public static class CacheEntry {
        public final byte[] responseBytes;
        public final long createdAtMillis;
        public final long ttlSeconds;

        public CacheEntry(byte[] responseBytes, long ttlSeconds) {
            this.responseBytes = responseBytes;
            this.ttlSeconds = ttlSeconds;
            this.createdAtMillis = System.currentTimeMillis();
        }

        public boolean isExpired() {
            long expiresAt = createdAtMillis + ttlSeconds * 1000L;
            return System.currentTimeMillis() > expiresAt;
        }
    }
}