import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class UDPServer {
    private static final int CHUNKS = 10;

    public static void main (String[] args) throws IOException {

        byte[] buf_receive = new byte[2 + CHUNKS];
        DatagramSocket ds = new DatagramSocket(3000);
        DatagramPacket dp_receive = new DatagramPacket(buf_receive, 2 + CHUNKS);

        System.out.println("Server is on, waiting for client to send data...");

        ds.receive(dp_receive);
        byte[] fileName = dp_receive.getData();
        int nameLength = 0;
        for (int i =0 ; i < fileName.length;i++) {
            if (fileName[i] == 0) {
                nameLength = i;
                break;
            }
        }
        String outputFileName = new String(fileName);
        OutputStream outputStream = new FileOutputStream("/Users/Lexie/Documents/" + outputFileName.substring(0,nameLength));

        // flag is used for faking packet error.
        boolean flag = true;

        byte seqNo_expect = 0;
        boolean running = true;
        while (running) {

            ds.receive(dp_receive);

            byte[] buf_write = new byte[CHUNKS];

            buf_receive = dp_receive.getData();
            byte seqNo_receive = buf_receive[0];
            byte checksum_receive = buf_receive[1];

            for (int i = 0; i < CHUNKS; i++) {
                buf_write[i] = buf_receive[i + 2];
            }

            Checksum checksum = new CRC32();
            checksum.update(buf_write, 0, buf_write.length);
            long checksumValue = checksum.getValue();


// test to see if a checksumValue calculated is not equal to the checksum_receive, can the program complete to send file or not.
/*
            if (flag) {
                checksumValue += 1;
                flag = false;
                System.out.println("make first checksum invalid..");
            }
*/

            byte[] buf_send = new byte[1];


            if ((seqNo_receive == seqNo_expect) && (checksum_receive == (byte) checksumValue)) {
                outputStream.write(buf_write, 0, CHUNKS);
                buf_send[0] = seqNo_expect;
                DatagramPacket dp_send = new DatagramPacket(buf_send, 1, dp_receive.getAddress(), 9000);
                
// test to see if Server does not send ACK back to client, can the program complete sending the file or not.
/*
                if (flag) {
                    System.out.println("Not sending ACK back to client for the first time...");
                    flag = false;
                }
                else {
                    ds.send(dp_send);
                }
*/

                ds.send(dp_send);    // if the above code is used for simulation, this line should be deleted.

                seqNo_expect = (byte) (seqNo_expect ^ 1);
                dp_receive.setLength(2 + CHUNKS);

            }

            else if (seqNo_receive == -1) {
                System.out.println("Data all received.");
                running = false;
            }

            else {
//                buf_send[0] = (byte) (seqNo_receive ^ 1);
                buf_send[0] = (byte) (seqNo_expect ^ 1);

                DatagramPacket dp_send = new DatagramPacket(buf_send, 1, dp_receive.getAddress(), 9000);
                ds.send(dp_send);
                dp_receive.setLength(2 + CHUNKS);
            }
        }
        System.out.println("Closing...");
        ds.close();
        outputStream.close();

    }
}