import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class DNSServer {
    private final int port;

    public DNSServer(int port) {
        this.port = port;
    }

    public void run() throws Exception{
        try (DatagramSocket socket = new MulticastSocket(port)) {
            System.out.println("MSDNS listing oh UDP port " + port);

            // message size
            byte[] buf = new byte[512];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);


                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();
                int packetLength = packet.getLength();

                System.out.println("Received " + packetLength + "bytes from " + clientAddress.getHostAddress() + ";" + clientPort);
            }

        }
    }
}