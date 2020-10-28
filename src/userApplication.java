import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

public class userApplication {

    static String echo_with_added_delay_request_code = "E8601\r";
    static String echo_with_no_delay_request_code = "E0000\r";
    static String image_request_code = "M8844\r";
    static String audio_request_code = "A3157 \r";
    static String ithakicopter_request_code = "M8844\r";

    static int serverPort = 38004;
    static byte[] hostIP = {(byte) 155, (byte) 207, 18, (byte) 208};
    static int clientPort = 48004;

    static void echo(double durationInMins, DatagramSocket s, DatagramSocket r, InetAddress hostAddress, String request_code) {
        String echoString = "";
        int packetCounter = 0;

        byte[] txbuffer = request_code.getBytes();
        DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);

        byte[] rxbuffer = new byte[2048];
        DatagramPacket q = new DatagramPacket(rxbuffer, rxbuffer.length);

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < durationInMins * 60 * 1000) {
            long requestTime = System.currentTimeMillis();
            try{
                s.send(p);
                System.out.println("Sent the request");
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (;;) {
                System.out.println("Waiting for answer");
                try {
                    r.receive(q);
                    String message = new String(rxbuffer,0,q.getLength());
                    System.out.println(message);
                    packetCounter++;
                    echoString += message + ", " + requestTime + ", " + System.currentTimeMillis() + ", " +(System.currentTimeMillis() - requestTime ) + ", \n";
                    break;
                } catch (Exception x) {
                    System.out.println(x);
                }
            }
        }
        System.out.println(echoString);
        System.out.println(packetCounter);

        //Save echoOutput on a csv file
        try (PrintWriter out = new PrintWriter((request_code == echo_with_added_delay_request_code) ? "./data/echo_with_added_delay.csv" : "./data./echo_with_no_delay.csv")) {
            out.println(echoString);
        }catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public static void main(String[] args) throws Exception {
        DatagramSocket s;
        try {
            s = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        DatagramSocket r = new DatagramSocket(clientPort);
        InetAddress hostAddress = InetAddress.getByAddress(hostIP);
        r.setSoTimeout(100);

        echo(0.25, s, r, hostAddress, echo_with_added_delay_request_code);
        echo(0.25, s, r, hostAddress, echo_with_no_delay_request_code);
        s.close();
        r.close();
    }
}




