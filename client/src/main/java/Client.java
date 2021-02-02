import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


import java.net.Socket;
import java.util.Scanner;

public class Client {
    static final int PORT = 8189;
    static Socket clientSocket;
    static String IP_ADRESS = "localhost";
    static DataInputStream inSock;
    static DataOutputStream outSock;
    static Scanner in;

    public static void main(String[] args) {
        try {
            clientSocket = new Socket(IP_ADRESS, PORT);
            in = new Scanner(System.in);
            outSock = new DataOutputStream(clientSocket.getOutputStream());
            inSock = new DataInputStream(clientSocket.getInputStream());

            System.out.println("client connected" + clientSocket.getRemoteSocketAddress());
            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        String inScan = in.nextLine();
                        if (in.equals("/end")) {
                            System.out.println("Client diconnected");
                            in.close();
                            break;
                        }
                        try {
                            System.out.println("Client write: " + inScan);
                            outSock.writeUTF(inScan);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

            while (true) {
                String str = inSock.readUTF();
                if (str.equals("/end")) {
                    try {
                        System.out.println("Server diconnected");
                        inSock.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    break;
                }
                System.out.println("Server: "+ str);

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                System.out.println("client disconected");
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
}

