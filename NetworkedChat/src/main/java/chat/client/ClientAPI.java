package chat.client;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientAPI {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Se connecter au serveur avec nom d'utilisateur et listener pour recevoir les messages
    public void connect(String ip, int port, String username, Consumer<String> onMessageReceived) throws IOException {
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Envoyer le nom au serveur en premier
        out.println(username);

        // Thread pour recevoir les messages
        new Thread(() -> {
            String msg;
            try {
                while ((msg = in.readLine()) != null) {
                    onMessageReceived.accept(msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


