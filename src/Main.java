
public class Main {
    public static void main (String[] args) throws Exception{
        DNSServer server = new DNSServer(8053);
        server.run();
    }
}