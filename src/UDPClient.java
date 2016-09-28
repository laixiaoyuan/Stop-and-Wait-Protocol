import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class UDPClient {
    private static final int TIMEOUT = 100;
    private static final int CHUNKS = 10;
    public static void main(String args[]) throws IOException {
        String inputFileName = "/Users/Lexie/Desktop/test.png";
        InputStream inputStream = new FileInputStream(inputFileName);

        byte[] buf = new byte[CHUNKS];

        DatagramSocket ds = new DatagramSocket(9000);
        InetAddress loc = InetAddress.getLocalHost();
        int count;
        byte seqNo_expect = 0;
        byte seqNo_receive = 1;

        System.out.println("Start sending data...");

        // sending file name
        String outputFileName = "test.png";
        byte[] buf_fileName = outputFileName.getBytes();

        DatagramPacket dp_send = new DatagramPacket(buf_fileName, buf_fileName.length, loc, 3000);
        ds.send(dp_send);

        // flag is used for faking packet error.
        boolean flag = true;

        // sending buffer
        while ((count = inputStream.read(buf)) != -1) {

            Checksum checksum = new CRC32();
            checksum.update(buf, 0, buf.length);
            long checksumValue = checksum.getValue();

            byte[] buf_send = new byte [2 + CHUNKS];
            buf_send[0] = seqNo_expect;
            buf_send[1] = (byte) checksumValue;
            for (int i = 2; i < buf_send.length; i++) {
                buf_send[i] = buf[i - 2];
            }

            dp_send = new DatagramPacket(buf_send, buf_send.length, loc, 3000);

            DatagramPacket dp_receive = new DatagramPacket(new byte[1], 1);

            // set timeout
            ds.setSoTimeout(TIMEOUT);

            boolean receivedResponse = false;

            while (!receivedResponse || seqNo_receive != seqNo_expect) {

// test to see if a buffer is not sent to Server, will the program complete sending the file.
/*
                if (flag) {
                    System.out.println("Not sending the first packet...");
                    flag = false;
                }
                else {
                    ds.send(dp_send);
                }
*/

                ds.send(dp_send);      // if the above code is used for simulation, this line should be deleted.


                try {
                    ds.receive(dp_receive);
                    seqNo_receive = dp_receive.getData()[0];

                    if (!dp_receive.getAddress().equals(loc)) {
                        throw new IOException("Received packet from an unknow source.");
                    }

                    receivedResponse = true;
                }
                catch (InterruptedIOException e){
                    System.out.println("Time out. Retry...");
                }
            }
            seqNo_expect = (byte) (seqNo_expect ^ 1);
        }

        byte[] buf_end = new byte[1];
        buf_end[0] = -1;
        dp_send = new DatagramPacket(buf_end, buf_end.length, loc, 3000);
        ds.send(dp_send);
        System.out.println("Data sending successful. Closing...");

        ds.close();
        inputStream.close();

    }
}
