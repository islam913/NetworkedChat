package chat.server;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerBroadcaster {

    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();

    public void addClient(PrintWriter out) {
        clients.add(out);
    }

    public void removeClient(PrintWriter out) {
        clients.remove(out);
    }

    public void broadcast(String message) {
        for (PrintWriter out : clients) {
            out.println(message);
        }
    }
}


