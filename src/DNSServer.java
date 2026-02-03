import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
// read a bytearray as an input stream
import java.io.ByteArrayInputStream;
import java.net.SocketTimeoutException;



public class DNSServer {
    private final int port;
    // add cache
    private final DNSCache cache = new DNSCache();

    public DNSServer(int port) {
        this.port = port;
    }

    public void run() throws Exception{
        try (DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("127.0.0.1"))) {
            System.out.println("MSDNS listing oh UDP port " + port);

            // message size
            byte[] buf = new byte[512];

            while (true) {
                System.out.println("waiting for paket...");
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                System.out.println("Got One");

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();



                // old line
                // ByteArrayInputStream in = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());

                // copy requestBytes
                byte[] requestBytes = new byte[packet.getLength()];
                // faster loop I learned from server tutorial
                System.arraycopy(packet.getData(), packet.getOffset(), requestBytes, 0, packet.getLength());

                // decode request With DNSMessage
               DNSMessage requestMsg = DNSMessage.decodeMessage(requestBytes);
                System.out.println("Parsed request message: " + requestMsg);

                // still need question for cache
                DNSQuestion question = requestMsg.questions[0];
                System.out.println("Parsed question: " + question);

                DNSCache.CacheEntry cached = cache.get(question);
                if (cached != null) {
                    System.out.println("CACHE HIT for " + question.getQName());

                    byte[] response = cached.responseBytes.clone();
                    response[0] = requestBytes[0];
                    response[1] = requestBytes[1];

                    DatagramPacket back = new DatagramPacket(
                            response,
                            response.length,
                            clientAddress,
                            clientPort
                    );
                    socket.send(back);
                    System.out.println("Replied from cache");
                    continue; // skip google not we get data straight from memory

                    // used for check to assign cache
                } else {
                    System.out.println("CACHE MISS for " + question.getQName());
                }

                int packetLength = requestBytes.length;

                // relay on Google DNS (8.8.8.8) and  send request back to client
                try (DatagramSocket googleSocket = new DatagramSocket()) {
                    googleSocket.setSoTimeout(2000); // wait 2 sec

                    InetAddress googleAddress = InetAddress.getByName("8.8.8.8");
                    int googlePort = 53;

                    // forward the original bytes we received from dig
                    DatagramPacket toGoogle = new DatagramPacket(
                            requestBytes,
                            requestBytes.length,
                            googleAddress,
                            googlePort
                    );
                    googleSocket.send(toGoogle);

                    // receive google's response
                    byte[] googleBuf = new byte[512];
                    DatagramPacket fromGoogle = new DatagramPacket(googleBuf, googleBuf.length);
                    googleSocket.receive(fromGoogle);

                    System.out.println("Got " + fromGoogle.getLength() + "bytes from google");

                    // Now we copy exact response bytes
                    byte[] googleBytes = new byte[fromGoogle.getLength()];
                    System.arraycopy(fromGoogle.getData(), 0, googleBytes, 0,fromGoogle.getLength());

                    try {
                        DNSMessage googleMsg = DNSMessage.decodeMessage(googleBytes);

                        // cache only fisrt answer record
                        if (googleMsg.answers.length > 0) {
                            DNSRecord firstAnswer = googleMsg.answers[0];

                            cache.put(question, googleBytes, firstAnswer.ttl);
                            System.out.println("Stored in cache: " + firstAnswer);
                        } else {
                            System.out.println("Google response has 0 answers: not caching.");
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to parse Google response for cache: " + e.getMessage());
                    }

                    // send response back to the original client
                    DatagramPacket backToClient = new DatagramPacket(
                            googleBytes,
                            googleBytes.length,
                            clientAddress,
                            clientPort
                    );
                    socket.send(backToClient);
                    System.out.println("Relayed response back to client");
                } catch (SocketTimeoutException e) {
                    System.out.println("Timed out waiting for google DNS response");
                }

                System.out.println("Received " + packetLength + "bytes from " + clientAddress.getHostAddress() + ";" + clientPort);
            }

        }
    }
}

/**
 * Sources I used to help me understand
 * https://www.baeldung.com/udp-in-java
 * https://www.geeksforgeeks.org/java/working-udp-datagramsockets-java/
 * ChatGPT for clarifying ideas and ask questions about folder structures.
 * Also, I used last semester classes to review try catch
 */
