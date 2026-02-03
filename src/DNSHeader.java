import java.io.IOException;
import java.io.InputStream;

public class DNSHeader {
    private final int id;
    private final int flags;
    private final int questionCount;
    private final int answerCount;
    private final int authorityCount;
    private final int additionalRecordCount;




    public DNSHeader(
            int id,
            int flags,
            int questionCount,
            int answerCount,
            int authorityCount,
            int additionalRecordCount) {
        this.id = id;
        this.flags = flags;
        this.questionCount = questionCount;
        this.answerCount = answerCount;
        this.authorityCount = authorityCount;
        this.additionalRecordCount = additionalRecordCount;
    }


    public static DNSHeader decodeHeader(InputStream in) throws IOException {
        // declared variables we want to decode (split bytes)
        int id = readFirst16Bits(in);
        int flags = readFirst16Bits(in);
        int questionCount = readFirst16Bits(in);
        int answerCount = readFirst16Bits(in);
        int authorityCount = readFirst16Bits(in);
        int additionalRecordCount = readFirst16Bits(in);

        // return
        return new DNSHeader(
                id,
                flags,
                questionCount,
                answerCount,
                authorityCount,
                additionalRecordCount
                );
    }

    private static int readFirst16Bits(InputStream in) throws IOException {
        int high = in.read();
        int low  = in.read();
        if (high < 0 || low < 0) {
            throw new IOException("Unexpected end of stream while reading 16 bits");
        }
        return (high << 8) | low;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public int getAnswerCount() {
        return answerCount;
    }

    @Override
    public String toString() {
        return "DNSHeader{" +
                "id=" + id +
                ", flags=0x" + Integer.toHexString(flags) +
                ", questionCount=" + questionCount +
                ", answerCount=" + answerCount +
                ", authorityCount=" + authorityCount +
                ", additionalRecordCount=" + additionalRecordCount +
                '}';
    }

    public int getAuthorityCount() {
        return authorityCount;
    }

    public int getAdditionalRecordCount() {
        return additionalRecordCount;
    }
}

/**
 * Sources I used online
 * https://www.geeksforgeeks.org/computer-networks/dns-message-format/
 * */