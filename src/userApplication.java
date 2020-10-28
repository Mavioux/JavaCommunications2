import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;

public class userApplication {

    String echo_request_code = "E0180\r";
    String image_request_code = "M8844\r";
    String audio_request_code = "A3157 \r";
    String ithakicopter_request_code = "M8844\r";

    void echo(int duration) {

    }

    public static void main(String[] args) throws Exception {

        DatagramSocket s;
        try {
            s = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        String packetInfo = "E8601\r";
        byte[] txbuffer = packetInfo.getBytes();
        int serverPort = 38004;
        byte[] hostIP = {(byte) 155, (byte) 207, 18, (byte) 208};
        InetAddress hostAddress = InetAddress.getByAddress(hostIP);
        DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);
        s.send(p);
        int clientPort = 48004;
        DatagramSocket r = new DatagramSocket(clientPort);
        r.setSoTimeout(1000);
        byte[] rxbuffer = new byte[2048];
        DatagramPacket q = new DatagramPacket(rxbuffer, rxbuffer.length);

        for (;;) {
            try {
                System.out.println(1);
                r.receive(q);
                String message = new String(rxbuffer,0,q.getLength());
                System.out.println(message);
                System.out.println(2);
            } catch (Exception x) {
                System.out.println(x);
            }
        }
//        s.close();
//        r.close();
    }

}
