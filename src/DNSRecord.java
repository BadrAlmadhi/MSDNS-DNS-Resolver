import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


public class DNSRecord {

    // The owner name for this record ("example.com")
    public final String name;

    // Record type: 1=A, 28=AAAA, 5=CNAME, etc.
    public final int type;

    // Record class: usually 1=IN (Internet)
    public final int rclass;

    // TTL in seconds (how long we can cache this record)
    public final long ttl;

    // Length of RDATA field (bytes)
    public final int rdLength;

    // Raw RDATA bytes (for A record: 4 bytes = IPv4)
    public final byte[] rdata;

    // when program received the answer from Google
    public final long createdAtMillis;

    private DNSRecord(String name, int type, int rclass, long ttl, int rdLength, byte[] rdata, long createdAtMillis) {
        this.name = name;
        this.type = type;
        this.rclass = rclass;
        this.ttl = ttl;
        this.rdLength = rdLength;
        this.rdata = rdata;
        this.createdAtMillis = createdAtMillis;
    }

    /**
     * Decode ONE DNS record from the stream.
     *
     * @param in          stream positioned at the start of a record
     * @param fullMessage the entire DNS message bytes (needed for compression pointers)
     */
    public static DNSRecord decodeRecord(InputStream in, byte[] fullMessage) throws IOException {

        // 1) NAME (variable length, may be a pointer that jumps elsewhere)
        String name = readName(in, fullMessage);

        // 2) TYPE (2 bytes)
        int type = readU16(in);

        // 3) CLASS (2 bytes)
        int rclass = readU16(in);

        // 4) TTL (4 bytes)  <-- IMPORTANT: TTL is U32 not U16
        long ttl = readU32(in);

        // 5) RDLENGTH (2 bytes)
        int rdLength = readU16(in);

        // 6) RDATA (rdLength bytes)
        byte[] rdata = new byte[rdLength];
        readFully(in, rdata);

        return new DNSRecord(name, type, rclass, ttl, rdLength, rdata, System.currentTimeMillis());
    }

    /**
     * Read a DNS NAME at the current position in the stream.
     *
     * DNS names are encoded as:
     *   [len][label bytes][len][label bytes]...[0]
     *
     * But in responses, DNS often compresses the name using a POINTER:
     *   If the first byte has top two bits = 11 (0xC0),
     *   then NAME is actually 2 bytes: 0xC0 XX
     *   and the lower 14 bits are the offset into the message.
     */
    private static String readName(InputStream in, byte[] fullMessage) throws IOException {
        StringBuilder sb = new StringBuilder();

        // Read the first byte of NAME
        int first = in.read();
        if (first < 0) throw new IOException("EOF while reading NAME");

        // Case A: NAME is a COMPRESSION POINTER (0xC0xx)
        if ((first & 0xC0) == 0xC0) {
            // Need second byte to complete the pointer
            int second = in.read();
            if (second < 0) throw new IOException("EOF while reading NAME pointer");

            // pointer offset = 14 bits:
            // - top two bits are 11 (ignored)
            // - remaining 6 bits of first byte + all 8 bits of second byte
            int offset = ((first & 0x3F) << 8) | second;

            // Jump to that offset in the message and read the name from there
            return readNameFromOffset(offset, fullMessage);
        }

        // Case B: NAME is NOT a pointer; then 'first' is the first label length
        int len = first;

        while (true) {
            // len == 0 means end of name
            if (len == 0) break;

            // Read the label bytes
            byte[] label = new byte[len];
            readFully(in, label);

            // Add dot between labels: "example" + "." + "com"
            if (sb.length() > 0) sb.append('.');
            sb.append(new String(label));

            // Read next length byte (could be 0, could be another label, could be a pointer)
            len = in.read();
            if (len < 0) throw new IOException("EOF while reading NAME");

            // DNS spec allows a pointer to appear here too (name ends with pointer suffix)
            if ((len & 0xC0) == 0xC0) {
                int second = in.read();
                if (second < 0) throw new IOException("EOF while reading NAME pointer");

                int offset = ((len & 0x3F) << 8) | second;
                String suffix = readNameFromOffset(offset, fullMessage);

                // Append the suffix (avoid double dots)
                if (sb.length() > 0 && suffix.length() > 0) sb.append('.');
                sb.append(suffix);
                break; // pointer ends the name
            }

            // Otherwise loop continues with next normal label length
        }

        return sb.toString();
    }

    private static void readFully(InputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) throw new IOException("EOF while reading bytes");
            off += n;
        }
    }


    /**
     * Read a name from an earlier position in the DNS message (compression pointer target).
     *
     * We create a new ByteArrayInputStream that starts at 'offset', then reuse readName.
     */
    private static String readNameFromOffset(int offset, byte[] fullMessage) throws IOException {
        ByteArrayInputStream bin =
                new ByteArrayInputStream(fullMessage, offset, fullMessage.length - offset);

        // This may encounter pointers again, so it’s recursive (safe in real packets)
        return readName(bin, fullMessage);
    }

    /**
     * Read an unsigned 16-bit int from the stream (big-endian).
     * DNS uses network byte order (big-endian).
     */
    private static int readU16(InputStream in) throws IOException {
        int hi = in.read();
        int lo = in.read();
        if (hi < 0 || lo < 0) throw new IOException("EOF while reading U16");
        return (hi << 8) | lo;
    }

    /**
     * Read an unsigned 32-bit value from the stream (big-endian).
     * TTL uses 4 bytes.
     */
    private static long readU32(InputStream in) throws IOException {
        long b1 = in.read();
        long b2 = in.read();
        long b3 = in.read();
        long b4 = in.read();
        if (b1 < 0 || b2 < 0 || b3 < 0 || b4 < 0) throw new IOException("EOF while reading U32");

        // Combine 4 bytes into one 32-bit number
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }

    /**
     * If this record is an A record, convert the 4 RDATA bytes to "a.b.c.d".
     * Used for debugging / printing.
     */
    public String ipv4StringIfA() {
        if (type != 1 || rdLength != 4) return null;

        // & 0xFF converts signed Java byte to unsigned 0..255
        int a = rdata[0] & 0xFF;
        int b = rdata[1] & 0xFF;
        int c = rdata[2] & 0xFF;
        int d = rdata[3] & 0xFF;

        return a + "." + b + "." + c + "." + d;
    }

    public boolean isExpired() {
        long expiresAt = createdAtMillis + ttl *1000L;
        return System.currentTimeMillis() > expiresAt;
    }

    @Override
    public String toString() {
        String ip = ipv4StringIfA();
        return "DNSRecord{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", class=" + rclass +
                ", ttl=" + ttl +
                ", rdLength=" + rdLength +
                (ip != null ? ", A=" + ip : "") +
                '}';
    }
}
