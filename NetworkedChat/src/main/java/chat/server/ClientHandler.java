package chat.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            // Demander le nom du client à la première ligne reçue
            username = in.readLine();
            if (username == null || username.isEmpty()) {
                username = "Anonyme";
            }

            // Ajouter le client au broadcaster
            server.getBroadcaster().addClient(out);

            // Message système
            server.broadcast("[Système] " + username + " est connecté !");

            String message;
            while ((message = in.readLine()) != null) {
                // Ajouter le nom devant le message
                server.broadcast(username + ": " + message);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                server.getBroadcaster().removeClient(out);
            }
            server.broadcast("[Système] " + username + " a quitté le chat.");
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}



