package chat.ui;

import chat.client.ClientAPI;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.io.IOException;

public class ChatController {

    @FXML
    private TextField inputField;

    @FXML
    private TextArea chatArea;

    @FXML
    private Button sendButton;

    private ClientAPI client;
    private String username;

    @FXML
    private void initialize() {
        // Demander le nom
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Entrez votre nom :");
        dialog.setTitle("Nom d'utilisateur");
        dialog.showAndWait().ifPresent(name -> username = name);
        if (username == null || username.isEmpty()) username = "Anonyme";

        client = new ClientAPI();
        try {
            client.connect("127.0.0.1", 5000, username, this::receiveMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        sendButton.setOnAction(e -> sendMessage());
    }

    private void sendMessage() {
        String msg = inputField.getText();
        if (!msg.isEmpty()) {
            client.sendMessage(msg);
            inputField.clear();
        }
    }

    private void receiveMessage(String msg) {
        Platform.runLater(() -> chatArea.appendText(msg + "\n"));
    }
}



