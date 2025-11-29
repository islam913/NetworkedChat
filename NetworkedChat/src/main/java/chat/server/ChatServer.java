package chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    private final ServerBroadcaster broadcaster = new ServerBroadcaster();

    public ServerBroadcaster getBroadcaster() {
        return broadcaster;
    }

    public void broadcast(String message) {
        broadcaster.broadcast(message);
    }

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Server started on port 5000");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket, this);
            new Thread(handler).start();
        }
    }

    public static void main(String[] args) {
        try {
            new ChatServer().startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


