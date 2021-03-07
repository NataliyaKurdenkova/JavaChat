package client;

import commands.Command;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;


public class Controller implements Initializable {
    @FXML
    public ListView clientList;
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
    private Stage regStage;
    private RegController regController;

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
     //для установки фокуса на textField при инициализации окна
      /*  Platform.runLater(new Runnable() {
            public void run() {
                textField1.requestFocus();
            }
        });*/

        //для получения ссылки на сцену, чтобы потом поменять название окна
        Platform.runLater(() -> {
            stage = (Stage) textField1.getScene().getWindow();
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    System.out.println("bye");
                    if (clSocket != null && !clSocket.isClosed()) {
                        try {
                            out.writeUTF(Command.END);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
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
                            String str = in.readUTF();

                            if (str.startsWith("/")) {
                                if (str.equals(Command.END)) {
                                   // System.out.println("server disconnected us");
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
                            if (str.startsWith("/")) {
                                if (str.equals(Command.END)) {
                                    setAuthenticated(false);
                                    break;
                                }
                                if (str.startsWith(Command.CLIENT_LIST)) {
                                    String[] token = str.split("\\s");
                                    Platform.runLater(() -> {
                                        clientList.getItems().clear();
                                        for (int i = 1; i < token.length; i++) {
                                            clientList.getItems().add(token[i]);
                                        }
                                    });
                                }
                            } else {
                                textArea1.appendText(str + "\n");
                            }

                        }
                        } catch(RuntimeException e){
                            System.out.println(e.getMessage());
                        } catch(IOException e){
                            e.printStackTrace();
                        } finally{
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
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);
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

    public void clientListClicked(MouseEvent mouseEvent) {
        System.out.println(clientList.getSelectionModel().getSelectedItem());
        String msg = String.format("%s %s ", Command.W_NICK, clientList.getSelectionModel().getSelectedItem());
        textField1.setText(msg);
    }

    //создать и показать окно регистрации
    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();

            regController = fxmlLoader.getController();
            regController.setController(this);

            regStage = new Stage();
            regStage.setTitle("регистрация");
            regStage.setScene(new Scene(root, 400, 300));

            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.initStyle(StageStyle.UTILITY);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void clickRegBtn(ActionEvent actionEvent) {
        if (regStage == null) {
            createRegWindow();
        }
        regStage.show();
    }
    public void tryToReg(String login, String password, String nickname) {
        String message = String.format("%s %s %s %s", Command.REG, login, password, nickname);
        if (clSocket == null || clSocket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
