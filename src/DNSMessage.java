import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

public class DNSMessage {

    public final DNSHeader header;
    public final DNSQuestion[] questions;
    public final DNSRecord[] answers;
    public final DNSRecord[] authorities;
    public final DNSRecord[] additionals;

    // original bytes to use compression
    public final byte[] originalBytes;

    private DNSMessage(
            DNSHeader header,
            DNSQuestion[] questions,
            DNSRecord[] answers,
            DNSRecord[] authorities,
            DNSRecord[] additionals,
            byte[] originalBytes
            ) {
        this.header = header;
        this.questions = questions;
        this.answers = answers;
        this.authorities = authorities;
        this.additionals = additionals;
        this.originalBytes = originalBytes;
    }

    // Decode full DNS Message from raw data
    public static DNSMessage decodeMessage(byte[] bytes) throws IOException {

        ByteArrayInputStream in = new ByteArrayInputStream(bytes);

        // header has 12 byttes
        DNSHeader header = DNSHeader.decodeHeader(in);

        // Questions (Variable count)
        DNSQuestion[] question = new DNSQuestion[header.getQuestionCount()];
        for (int i = 0; i < question.length; i++) {
            question[i] = DNSQuestion.decodeQuestion(in, bytes);
        }

        // Answers (record)
        DNSRecord[] answers = new DNSRecord[header.getAnswerCount()];
        for (int i = 0; i < answers.length; i++) {
            answers[i] = DNSRecord.decodeRecord(in, bytes);
        }

        // Authorities
        DNSRecord[] authorities = new DNSRecord[header.getAuthorityCount()];
        for (int i = 0; i < authorities.length; i++) {
            authorities[i] = DNSRecord.decodeRecord(in, bytes);
        }

        // Additional
        DNSRecord[] additionals = new DNSRecord[header.getAdditionalRecordCount()];
        for (int i = 0; i < additionals.length; i++) {
            additionals[i] = DNSRecord.decodeRecord(in, bytes);
        }

        return new DNSMessage(header, question, answers, authorities, additionals, bytes);
    }

    @Override
    public String toString() {
        return "DNSMessage{\n" +
                "  header=" + header + ",\n" +
                "  questions=" + Arrays.toString(questions) + ",\n" +
                "  answers=" + Arrays.toString(answers) + ",\n" +
                "  authorities=" + Arrays.toString(authorities) + ",\n" +
                "  additionals=" + Arrays.toString(additionals) + "\n" +
                "}";
    }
}