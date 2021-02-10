package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private ServerSocket servSocket;
    private Socket clSocket;
    private final int PORT = 8189;

    private List<ClientHandler> clients;

    private AuthService authService;

    public Server() {
        clients=new CopyOnWriteArrayList<>();
        authService = new SimpleAuthService();
        try {
            servSocket = new ServerSocket(PORT);
            System.out.println("server started");
            while (true) {
                clSocket = servSocket.accept();
                System.out.println("client conected"+clSocket.getRemoteSocketAddress());
                new ClientHandler(this,clSocket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                servSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // отправка сообщения всем пользователям списка clients
    public void broadcastMsg(ClientHandler sender, String msg){
        String message = String.format("[ %s ] : %s", sender.getNickname(), msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }
// приватные сообщения
    public void msgPrivate(ClientHandler author, String receiver, String msg){

        for (ClientHandler c : clients) {
            if(c.getNickname().equals(receiver)){
                c.sendMsg(String.format("[ %s ]: %s", author.getNickname(),  msg));
                author.sendMsg(String.format("[ I ]: %s", author.getNickname(),  msg));
                return;
            }
        }
        author.sendMsg("user: "+ receiver+ "not found");
    }


    // добавляет(подписывает) клиента в список
    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastClientList();
    }
    // удаляет(отписывает) клиента из списка
    public void unsubscribe(ClientHandler clientHandler){
        clients.remove(clientHandler);
        broadcastClientList();
    }


    public AuthService getAuthService() {
        return authService;
    }


//отображение всех залогинившихся клиентов в листе справа чата
    public void broadcastClientList(){
        StringBuilder sb = new StringBuilder(Command.CLIENT_LIST);
        for (ClientHandler c : clients) {
            sb.append(" ").append(c.getNickname());
        }

        String message = sb.toString();

        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public boolean isLoginAuthenticated(String login){
        for (ClientHandler c : clients) {
            if(c.getLogin().equals(login)){
                return true;
            }
        }
        return false;
    }

}


