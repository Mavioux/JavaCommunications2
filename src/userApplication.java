import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

public class userApplication {


    static String echo_with_no_delay_request_code = "E0000";
    static String echo_with_added_delay_request_code = "E4368";
    static String image_request_code = "A2936";
    static String audio_request_code = "A5225";
    static String ithakicopter_request_code = "Q2180";
    static String vehicle_request_code = "V1297";

    static int serverPort = 38008;
    static byte[] hostIP = {(byte) 155, (byte) 207, 18, (byte) 208};
    static int clientPort = 48008;

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
            try {
                s.send(p);
                System.out.println("Sent the request " + request_code);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (; ; ) {
                System.out.println("Waiting for answer");
                try {
                    r.receive(q);
                    String message = new String(rxbuffer, 0, q.getLength());
                    System.out.println(message);
                    packetCounter++;
                    echoString += message + ", " + requestTime + ", " + System.currentTimeMillis() + ", " + (System.currentTimeMillis() - requestTime) + ", \n";
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
        } catch (Exception e) {
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

        try {
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
        for (; ; ) {
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
                if (request_timeout_counter < 1) {
                    return;
                }
            }
        }
    }

    static void audio(DatagramSocket s, DatagramSocket r, InetAddress hostAddress, String y, String xxx, String lzz, boolean timeStamp) {

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

        try {
            s.send(p);
            System.out.println("Sent Request: " + request_code);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        byte[] importantBuffer = new byte[2 * 128 * Integer.parseInt(xxx)];
        for (int i = 0; i < Integer.parseInt(xxx); i++) {
            try {
                r.receive(q);
                System.out.println("Downloading...");

                int counter = 0;
                int sample2 = 0;
                for (int j = 0; j < q.getData().length; j++) {
                    int help1 = 15;
                    int help2 = 240;
                    int a = q.getData()[j];//The byte containing the 2 nibbles
                    int nibble1 = (help1 & a);//The first nibble
                    int nibble2 = ((help2 & a) >> 4);//The second nibble

                    //Since the difference of xi - x(i-1) is quantized and then 8 is added to it, we follow the reverse process to deconstruct sample1 from the previous sample2 and sample2 from the previous sample1
                    int beta = 3;
                    int difference1 = (nibble1 - 8) * beta;
                    int difference2 = (nibble2 - 8) * beta;

                    //Create Samples
                    int sample1 = sample2 + difference2;
                    sample2 = sample1 + difference1;

                    importantBuffer[2 * 128 * i + counter] = (byte) sample1;
                    importantBuffer[2 * 128 * i + counter + 1] = (byte) sample2;
                    counter += 2;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Initiate the file in which we will write the audio in
        File file = null;
        try {
            if (!timeStamp) {
                file = new File("./data/audio/ithaki_music_repo/audio_" + lzz + "_" + xxx + ".wav");
            } else {
                file = new File("./data/audio/audio_" + System.currentTimeMillis() + "_" + lzz + "_" + y + xxx + ".wav");
            }
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

    static void aq_audio(DatagramSocket s, DatagramSocket r, InetAddress hostAddress, String y, String xxx, String lzz, boolean timeStamp) {

        String request_code = audio_request_code + "AQ" + lzz + y + xxx;
        try {
            r.setSoTimeout(3600);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] txbuffer = request_code.getBytes();
        DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);

        byte[] rxbuffer = new byte[132];
        DatagramPacket q = new DatagramPacket(rxbuffer, rxbuffer.length);

        try{
            s.send(p);
            System.out.println("Sent Request: " + request_code);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        byte[] mean_in_bytes = new byte[4];
        byte[] step_in_bytes = new byte[4];
        byte[] importantBuffer = new byte[2*2*128*Integer.parseInt(xxx)];
        for(int i = 0; i < Integer.parseInt(xxx); i++) {
           try{
               r.receive(q);
               System.out.println("Downloading AQ...");
               System.out.println("q.getData().length: " +q.getData().length);

               //Get the mean value from the first 4 bytes of the q.getData() byte array
               mean_in_bytes[0] = q.getData()[0];
               mean_in_bytes[1] = q.getData()[1];
               mean_in_bytes[2] = (byte)(( q.getData()[1] & 0x80) !=0 ? 0xff : 0x00);
               mean_in_bytes[3] = (byte)(( q.getData()[1] & 0x80) !=0 ? 0xff : 0x00);
               int mean = ByteBuffer.wrap(mean_in_bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
               System.out.println("Mean: " + mean);

               step_in_bytes[0] = q.getData()[2];
               step_in_bytes[1] = q.getData()[3];
               step_in_bytes[2] = (byte)(( q.getData()[3] & 0x80) !=0 ? 0xff : 0x00);
               step_in_bytes[3] = (byte)(( q.getData()[3] & 0x80) !=0 ? 0xff : 0x00);
               int step = ByteBuffer.wrap(step_in_bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
               System.out.println("Step: " + step);

               int counter = 0;
               for (int j = 4; j < q.getData().length; j++){
                   int nibble1 = (int)(q.getData()[j] & 0x0000000F);
                   System.out.println(nibble1);
                   int nibble2 = (int)((q.getData()[j] & 0x000000F0)>>4);
                   System.out.println(nibble2);

                   int difference1 = nibble2-8;
                   int difference2 = nibble1-8;

                   //Creation of samples
                   int sample1 = step*difference1 + mean; //First demodulated sample (16 bits)
                   int sample2 = step*difference2 + mean;//Second demodulated sample (16 bits)

                   //Save the samples to the importantBuffer that contains the song
                   //Maybe rename importantBuffer to songBuffer...
                   importantBuffer[2 * 128 * i + counter] = (byte) (sample1);
                   importantBuffer[2 * 128 * i + counter + 1] = (byte) (sample1 >> 8);
                   importantBuffer[2 * 128 * i + counter + 2] = (byte) (sample2);
                   importantBuffer[2 * 128 * i + counter + 3] = (byte) (sample2 >> 8);
                   counter += 4;
               }

           } catch (Exception e) {
               e.printStackTrace();
           }
        }


        //Initiate the file in which we will write the audio in
        File file = null;
        try {
            if(!timeStamp) {
                file = new File("./data/audio/ithaki_music_repo_AQ/audio_" + lzz + "_" + xxx + ".wav");
            }
            else {
                file = new File("./data/audio/audio_" + System.currentTimeMillis() + "_AQ" + "_" + lzz + "_" + y + xxx + ".wav");
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            return;
        }

        //Write to wav
        InputStream bytes_in = new ByteArrayInputStream(importantBuffer);
        try {
            AudioFormat format = new AudioFormat(8000, 16, 1, true, false);
            AudioInputStream stream = new AudioInputStream(bytes_in, format, importantBuffer.length);
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);
            System.out.println("Saved wav file with name: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void ithakicopter_tcp(InetAddress hostAddress) throws Exception{
        //TCP
        int tcp_port_number = 38048;
        Socket socket = new Socket(hostAddress, tcp_port_number);
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();



        //TCP Request
        StringBuilder data = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(input);
        int character;

        String out_message = ithakicopter_request_code;
        output.write(out_message.getBytes());

        //Reading TCP Response
        while ((character = reader.read()) != -1) {
            data.append((char) character);
//            System.out.println("Reading");
        }

        System.out.println(data);
        System.out.println("Data length: " + data.length());


        //10 TCP Requests
        for(int i = 0; i < 10; i++) {
            socket = new Socket(hostAddress, tcp_port_number);
            input = socket.getInputStream();
            output = socket.getOutputStream();

            out_message = "AUTO FLIGHTLEVEL=" + (i* 50) +" LMOTOR=" + (150 + i * 5) + " RMOTOR=" + (150 + i * 5) + " PILOT \r\n";
            System.out.println(out_message);
            output.write(out_message.getBytes());

            //TCP Response
            StringBuilder data2 = new StringBuilder();
            InputStreamReader reader2 = new InputStreamReader(input);
            while ((character = reader2.read()) != -1) {
                data2.append((char) character);
//                System.out.println("Reading " + i);
            }

            System.out.println(data2);
            System.out.println("Data2 length: " + data2.length());
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


        //Commands!
        //Echo Request With Added Delay
//        echo(0.25, s, r, hostAddress, echo_with_added_delay_request_code);
        //Echo Request With No Added Delay
//        echo(0.25, s, r, hostAddress, echo_with_no_delay_request_code);

        //Image Request From Cam 1
//        image(s, r, hostAddress, "CAM=FIX");
        //Image Request From Cam 2
//        image(s, r, hostAddress, "CAM=PTZ");

        //Audio DPCM Request of a Song
//        audio(s, r, hostAddress, "F", "999", "", true);
        //Audio DPCM Request  of Frequency Generator
//        audio(s, r, hostAddress, "T", "999", "", true);
        //Audio DPCM Request  of Frequency Generator
//        aq_audio(s, r, hostAddress, "F", "999", "", true);

        //Close udp DataSockets
        s.close();
        r.close();

//        out.write("GET  /netlab/hello.htmlHTTP/1.0\r\nHost:ithaki.eng.auth.gr:80\r\n\r\n".getBytes());
//
//        // Read what gets into the input
//        StringBuilder data = new StringBuilder();
//
//        while ((character = reader.read()) != -1) {
//            data.append((char) character);
//        }
//
//        System.out.println(data);

        ithakicopter_tcp(hostAddress);







//        System.out.println(out);


//        //Iterate through ithaki's song repository
//        for(int i = 0; i < 100; i++){
//            String lzz = "";
//            if(i < 10){
//                lzz = "L0" + i;
//            }
//            else {
//                lzz = "L" + i;
//            }
//            System.out.println("lzz: " + lzz);
//            audio(s, r, hostAddress, "", "F", "999", lzz, false);
//        }


    }
}




