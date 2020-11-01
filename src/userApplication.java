
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.*;
import java.io.*;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class userApplication {


    static String echo_with_no_delay_request_code = "E0000";
    static String echo_with_added_delay_request_code = "E4368\r";
    static String image_request_code = "A2936";
    static String audio_request_code = "A5435";
    static String ithakicopter_request_code = "M8844\r";

    static int serverPort = 38010;
    static byte[] hostIP = {(byte) 155, (byte) 207, 18, (byte) 208};
    static int clientPort = 48010;

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

        //Initiate the file in which we will write the image in
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

        int request_timeout_counter = 5;
        for(;;) {
            try {
                request_timeout_counter--;
                r.receive(q);
                request_timeout_counter = 5;
                byte[] buffer = q.getData();
                for (int j = 0; j < buffer.length; j++) {
                    System.out.println("data: " + j + " " + buffer[j]);
                }
//                counter += q.getData().length;
                image.write(buffer);


            } catch (Exception e) {
                System.out.println(e);
                if(request_timeout_counter < 1){
                    return;
                }
            }
        }
    }

    static void audio(DatagramSocket s, DatagramSocket r, InetAddress hostAddress, String y, String xxx, String lzz) {

        String request_code = audio_request_code + lzz + y + xxx;
        try {
            r.setSoTimeout(3600);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] txbuffer = request_code.getBytes();
        DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);

        byte[] rxbuffer = new byte[128];
        DatagramPacket q = new DatagramPacket(rxbuffer, rxbuffer.length);

        try{
            s.send(p);
            System.out.println("Sent the audio request: " + request_code);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        byte[] importantBuffer = new byte[2*128*Integer.parseInt(xxx)];

        for (int i = 0; i < Integer.parseInt(xxx); i++) {

            try {
                System.out.println("i: " + i);
                r.receive(q);
                byte[] buffer = q.getData();
                for (int j = 0; j < q.getData().length; j++) {
                    System.out.println("data: " + j + " " + q.getData()[j]);
                }

                int counter = 0;
                int Sample2 = 0;
                for (int j = 0; j < q.getData().length; j++) {
                    int help1 = 15;
                    int help2 = 240;
                    int a = q.getData()[j];//The byte containing the 2 nibbles
                    int Nibble1 = (help1 & a);//The first nibble
                    int Nibble2 = ((help2 & a) >> 4);//The second nibble

                    int beta = 3;
                    int difference1 = (Nibble1 - 8) * beta;
                    int difference2 = (Nibble2 - 8) * beta;

                    //Create Samples
                    int Sample1 = Sample2 + difference2;
                    Sample2 = Sample1 + difference1;

                    importantBuffer[2*128*i + counter] = (byte) Sample1;
                    importantBuffer[2*128*i + counter + 1] = (byte) Sample2;
                    counter += 2;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


//                int counter = 0;
//                for (int j = 0; j < 256; j++) {
//                    if((j % 8) < 4) {
//                        importantBuffer[j] = 0;
//                    }
//                    else {
//                        importantBuffer[j] = buffer[counter];
//                        counter++;
//                    }
//                }
//                for (int j = 0; j < importantBuffer.length; j++) {
//                    System.out.println("data: " + j + " " + importantBuffer[j]);
//                }

            for(int k = 0; k < importantBuffer.length; k++) {
                System.out.println(k + ": " + importantBuffer[k]);
            }

            //Initiate the file in which we will write the audio in
            File file = null;
            try {
                file = new File("./data/audio/audio_" + System.currentTimeMillis() + "_" + lzz + "_" + xxx + ".wav");
            } catch (Exception e) {
                System.out.println(e.toString());
                return;
            }

            //Write to wav
            InputStream bytes_in = new ByteArrayInputStream(importantBuffer);
            try {
                AudioFormat format = new AudioFormat(8000, 8, 1, true, true);
                AudioInputStream stream = new AudioInputStream(bytes_in, format, importantBuffer.length);
                AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);
                System.out.println("Saved wav file with name: " + file.getName());
            } catch (Exception e) {
                e.printStackTrace();
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
//        image(s, r, hostAddress, "CAM=FIX");
        //Image Request From Cam 2
//        image(s, r, hostAddress, "CAM=PTZ");

        //Audio Request
        audio(s, r, hostAddress, "F", "999", "");

        s.close();
        r.close();
    }
}




