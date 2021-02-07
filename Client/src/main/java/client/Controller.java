package client;

import commands.Command;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;


public class Controller implements Initializable {
    @FXML
    private TextArea textArea1;
    @FXML
    private TextField textField1;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox authPanel;
    @FXML
    private HBox messagePanel;

    private Socket clSocket;
    private final int PORT = 8189;
    private final String IP_ADRESS = "localhost";
    private DataInputStream in;
    private DataOutputStream out;
    private boolean authenticated;
    private String nickname;
    private Stage stage;

    @FXML
    public void clickBtn2(ActionEvent actionEvent) {
        try {
            //проверка, чтобы сообщения после удаления пробелов в начале и конце сообщения было не пустым, тогда отправить
            if (textField1.getText().trim().length() > 0) {
                out.writeUTF(textField1.getText());
                textField1.clear();
                textField1.requestFocus();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // textArea1.appendText(textField1.getText() + "\n");

    }



    @Override
    public void initialize(URL location, ResourceBundle resources) {
      /*  Platform.runLater(new Runnable() {
            public void run() {
                textField1.requestFocus();
            }
        });*/

        //для получения ссылки на сцену, чтобы потом поменять название окна
        Platform.runLater(() -> {
            stage = (Stage) textField1.getScene().getWindow();
        });

        setAuthenticated(false);
    }

    private void connect() {

        try {
            clSocket = new Socket(IP_ADRESS, PORT);

            in = new DataInputStream(clSocket.getInputStream());
            out = new DataOutputStream(clSocket.getOutputStream());
            new Thread(new Runnable() {
                public void run() {
                    try {

                        //цикл аутентификации
                        while (true) {
                            String str = null;

                            str = in.readUTF();

                            if (str.startsWith("/")) {
                                if (str.equals(Command.END)) {
                                    System.out.println("server disconnected us");
                                    throw new RuntimeException("server disconnected us");
                                }
                                if (str.startsWith(Command.AUTH_OK)) {
                                    //разбиваем сообщение по пробелам и берем nickname
                                    nickname = str.split("\\s")[1];
                                    setAuthenticated(true);
                                    break;
                                }
                            } else {
                                textArea1.appendText(str + "\n");
                            }
                        }
// цикл работы
                        while (true) {
                            String str = in.readUTF();

                            if (str.equals(Command.END)) {
                                setAuthenticated(false);
                                break;
                            }

                            textArea1.appendText(str + "\n");
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        setAuthenticated(false);
                        try {
                            clSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }).start();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void trytoAuth(ActionEvent actionEvent) {
        //если сокет уже закрыт (напрмер пользователь отсоединился ИЛИ Еще не подключался, то подключится)
        if (clSocket == null || clSocket.isClosed()) {
            connect();
        }
        //String msg = String.format("/auth %s %s", loginField.getText().trim(), passwordField.getText().trim());
        String msg = String.format("%s %s %s",Command.AUTH, loginField.getText().trim(), passwordField.getText().trim());
        try {
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        messagePanel.setVisible(authenticated);
        messagePanel.setManaged(authenticated);
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
// если пользователь отключился стираем его никнейм
        if (!authenticated) {
            nickname = "";
        }

        setTitle(nickname);
        textArea1.clear();
    }

    private void setTitle(String title) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (title.equals("")) {
                    stage.setTitle("Чат");
                } else {
                    stage.setTitle(String.format("Чат  [ %s ]", title));
                }
            }
        });
    }
}
