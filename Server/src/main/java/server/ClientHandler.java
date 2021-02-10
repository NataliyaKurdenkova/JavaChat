package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import java.net.SocketTimeoutException;

public class ClientHandler {
    private Server server;
    private Socket clSocket;

    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.clSocket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {


                try {
                    clSocket.setSoTimeout(5000);

                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                System.out.println("client want to disconnected ");
                                out.writeUTF(Command.END);
                                throw new RuntimeException("client want to disconnected");
                            }
                            if (str.startsWith(Command.AUTH)) {
                                String[] token = str.split("\\s");
                                String newNick = server.getAuthService()
                                        .getNicknameByLoginAndPassword(token[1], token[2]);
                                if (newNick != null) {
                                    if (!server.isLoginAuthenticated(login)) {
                                        nickname = newNick;
                                        sendMsg(Command.AUTH_OK + " " + nickname);
                                        server.subscribe(this);
                                        clSocket.setSoTimeout(0);
                                        break;
                                    } else {
                                        sendMsg("С этим логинов уже вошли");
                                    }
                                } else {
                                    sendMsg("Неверный логин и пароль");

                                }
                            }
                            if (str.startsWith(Command.REG)) {
                                String[] token = str.split("\\s");
                                if (token.length < 4) {
                                    continue;
                                }
                                boolean regSuccessful = server.getAuthService()
                                        .registration(token[1], token[2], token[3]);
                                if (regSuccessful) {
                                    sendMsg(Command.REG_OK);
                                } else {
                                    sendMsg(Command.REG_NO);
                                }
                            }
                        }
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                out.writeUTF(Command.END);
                                break;
                            } else if (str.startsWith(Command.W_NICK)) {
                                String[] token = str.split("\\s+", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.msgPrivate(this, token[1], token[2]);
                            }

                        } else server.broadcastMsg(this, str);


                    }


                } catch (SocketTimeoutException e) {
                    try {
                        System.out.println("taimaut");
                        out.writeUTF(Command.END);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }

                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Client disconnected");
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }).start();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
