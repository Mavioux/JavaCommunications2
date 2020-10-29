
import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class userApplication {


    static String echo_with_no_delay_request_code = "E0000\r";
    static String echo_with_added_delay_request_code = "E1054\r";
    static String image_request_code = "M2114";
    static String audio_request_code = "A3157 \r";
    static String ithakicopter_request_code = "M8844\r";

    static int serverPort = 38003;
    static byte[] hostIP = {(byte) 155, (byte) 207, 18, (byte) 208};
    static int clientPort = 48003;

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
                System.out.println("Sent the request " + request_code);
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

        //Save echoString on a csv file
        try (PrintWriter out = new PrintWriter((request_code == echo_with_added_delay_request_code) ? "./data/echo_with_added_delay.csv" : "./data./echo_with_no_delay.csv")) {
            out.println(echoString);
        }catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    static void image(DatagramSocket s, DatagramSocket r, InetAddress hostAddress, String cameraParameter) {
        String request_code = image_request_code + cameraParameter + "\r";
        System.out.println(request_code);
        byte[] txbuffer = request_code.getBytes();
        DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);

        byte[] rxbuffer = new byte[128]; //Really important to set that to 128
        DatagramPacket q = new DatagramPacket(rxbuffer, rxbuffer.length);

        try{
            s.send(p);
            System.out.println("Sent the image request: " + request_code);
        } catch (Exception e) {
            e.printStackTrace();
        }

        File file = null;
        OutputStream image = null;
        try {
            file = new File("./data/images/image" + cameraParameter + ".jpg");
            image = new FileOutputStream(file);
        } catch (Exception e) {
            System.out.println(e.toString());
            return;
        }

        try {
            file = new File("./data/images/image" + cameraParameter + ".jpg");
            image = new FileOutputStream(file);
        } catch (Exception e) {
            System.out.println(e.toString());
            return;
        }

        byte[] imageBytes = new byte[0];
        int counter = 0;
        for(;;) {
            try {
                r.receive(q);
                byte[] buffer = q.getData();
//                imageBytes = Arrays.copyOf(imageBytes, (imageBytes.length +q.getData().length));
//                for(int i = 0; i < q.getData().length; i++) {
//                    imageBytes[counter + i] = buffer[i];
//                }
//                for (int j = 0; j < imageBytes.length; j++) {
//                    System.out.println("data: " + j + " " + imageBytes[j]);
//                }
//                counter += q.getData().length;


                image.write(buffer);

            } catch (Exception e) {
                System.out.println(e);
            }
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
        r.setSoTimeout(1000);

        //Echo Request With Added Delay
//        echo(0.25, s, r, hostAddress, echo_with_added_delay_request_code);
        //Echo Request With No Added Delay
//        echo(0.25, s, r, hostAddress, echo_with_no_delay_request_code);

        //Image Request From Cam 1
        image(s, r, hostAddress, "CAM=FIX");

        s.close();
        r.close();
    }
}




