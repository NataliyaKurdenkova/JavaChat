import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/*
1. Написать консольный вариант клиент\серверного приложения,
в котором пользователь может писать сообщения, как на клиентской стороне, так и на серверной.
Т.е. если на клиентской стороне написать "Привет", нажать Enter то сообщение должно передаться на сервер
и там отпечататься в консоли. Если сделать то же самое на серверной стороне,
сообщение соответственно передается клиенту и печатается у него в консоли.
Есть одна особенность, которую нужно учитывать: клиент или сервер может написать несколько сообщений подряд,
 такую ситуацию необходимо корректно обработать
Разобраться с кодом с занятия, он является фундаментом проекта-чата
ВАЖНО! Сервер общается только с одним клиентом, т.е. не нужно запускать цикл,
который будет ожидать второго/третьего/n-го клиентов
 */
public class ServerChat {
    static ServerSocket serverSocket;
    static final int PORT = 8189;
    static Socket clientSocket;
    static DataInputStream inServ;
    static DataOutputStream outServ;
    static Scanner inScanServ;
    static String inStr;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Сервер запущен");
            clientSocket = serverSocket.accept();
            System.out.println("Клиент соединился");

            inScanServ = new Scanner(System.in);
            inServ = new DataInputStream(clientSocket.getInputStream());
            outServ = new DataOutputStream(clientSocket.getOutputStream());

            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        inStr = inScanServ.nextLine();
                        if (inScanServ.equals("/end")) {
                            inScanServ.close();
                            break;
                        }
                        try {
                            System.out.println("Server write: " + inStr);
                            outServ.writeUTF(inStr);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
            while (true) {

               String str = inServ.readUTF();
                if (str.equals("/end")) {
                    System.out.println("Client diconnected");
                    inServ.close();
                    break;
                }
                System.out.println("Client: " + str);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inServ.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                System.out.println("client disconnected");
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                System.out.println("server disconnected");
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
