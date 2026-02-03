import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class DNSQuestion {

    //After Header we got Question format and filed are QName, QType, QClass
    // decode example.come
    private final String QName; // domain name www.example.com
    private final int QType; // what kind of record is being requested A-AAAA
    private final int QClass; // IN (Internet)

    private DNSQuestion(String QName, int QType, int QClass) {
        this.QName = QName;
        this.QType = QType;
        this.QClass = QClass;
    }

    public String getQName() { return QName; }
    public int getQType() { return QType; }
    public int getQClass() { return QClass; }

    // decode that supports compression pointers using the full message bytes
    public static DNSQuestion decodeQuestion(InputStream in, byte[] fullMessage) throws IOException {
        String qname = readName(in, fullMessage);
        int qType = readU16(in);
        int qClass = readU16(in);
        return new DNSQuestion(qname, qType, qClass);
    }

    // Keep your old name if you want; but make it call the new one safely
    public static DNSQuestion questionDecode(InputStream in) throws IOException {
        throw new IOException("Use decodeQuestion(in, fullMessage) so compression works.");
    }

    // we want to jump 12 bytes and read from there.
    private static String readName(InputStream in, byte[] fullMessage) throws IOException {
        StringBuilder sb = new StringBuilder();
        int jumps = 0;

        while (true) {
            int b = readU8(in);

            if (b == 0) break;

            // pointer: 11xxxxxx
            // compare with 11 if yes compression pointer not bale length
            if ((b & 0xC0) == 0xC0) {
                int b2 = readU8(in);
                // read the rest of name from offset
                int offset = ((b & 0x3F) << 8) | b2;

                // check of loop jump
                if (++jumps > 20) throw new IOException("Too many compression jumps (possible loop)");

                String suffix = readNameFromOffset(offset, fullMessage);

                // add logic for . in www.example.com
                if (sb.length() > 0 && suffix.length() > 0) sb.append('.');
                sb.append(suffix);
                break; // pointer ends the name
            }

            // ready full characters
            int len = b;
            byte[] label = new byte[len];
            readFully(in, label);

            if (sb.length() > 0) sb.append('.');
            sb.append(new String(label));
        }

        return sb.toString();
    }

    private static String readNameFromOffset(int offset, byte[] fullMessage) throws IOException {
        ByteArrayInputStream bin =
                new ByteArrayInputStream(fullMessage, offset, fullMessage.length - offset);
        return readName(bin, fullMessage);
    }

    private static int readU8(InputStream in) throws IOException {
        int b = in.read();
        if (b < 0) throw new IOException("Unexpected EOF");
        return b;
    }

    private static int readU16(InputStream in) throws IOException {
        int hi = readU8(in);
        int lo = readU8(in);
        return (hi << 8) | lo;
    }

    private static void readFully(InputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) throw new IOException("Unexpected EOF");
            off += n;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DNSQuestion)) return false;
        DNSQuestion that = (DNSQuestion) o;
        return QType == that.QType &&
                QClass == that.QClass &&
                Objects.equals(QName, that.QName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(QName, QType, QClass);
    }

    @Override
    public String toString() {
        return "DNSQuestion{" +
                "QName='" + QName + '\'' +
                ", QType=" + QType +
                ", QClass=" + QClass +
                '}';
    }
}



/**
 * Sources I used
 * https://www.geeksforgeeks.org/computer-networks/details-on-dns/
 * */