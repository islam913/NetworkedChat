package chat.p2p;

import java.io.*;
import java.net.*;

public class PeerHandler implements Runnable {
    private Socket peerSocket;
    private PeerNode peerNode;
    private BufferedReader reader;
    private PrintWriter writer;
    private int remotePort;
    private boolean isValidConnection;

    // Constructeur pour connexions sortantes (on connaît le port)
    public PeerHandler(Socket socket, PeerNode peerNode, int remotePort) {
        this.peerSocket = socket;
        this.peerNode = peerNode;
        this.remotePort = remotePort;
        this.isValidConnection = true; // Connexion sortante = validée
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Erreur creation handler: " + e.getMessage());
        }
    }

    // Constructeur pour connexions entrantes (port inconnu)
    public PeerHandler(Socket socket, PeerNode peerNode) {
        this.peerSocket = socket;
        this.peerNode = peerNode;
        this.remotePort = -1;
        this.isValidConnection = false; // Sera validé lors du HELLO
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Erreur creation handler: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            sendMessage("HELLO|" + peerNode.getPeerId() + "|" + peerNode.getUsername() + "|" + peerNode.getPort());

            String message;
            while ((message = reader.readLine()) != null) {
                processMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Deconnexion d'un pair");
        } finally {
            close();
        }
    }

    private void processMessage(String message) {
        // MODIFIÉ: Accepter jusqu'à 6 parties pour inclure le timestamp de Lamport
        String[] parts = message.split("\\|", 6);
        if (parts.length < 2) return;

        String command = parts[0];

        switch (command) {
            case "HELLO":
                if (parts.length >= 4) {
                    String peerId = parts[1];
                    String username = parts[2];
                    int receivedPort = Integer.parseInt(parts[3]);

                    // Pour les connexions entrantes, valider le port
                    if (!isValidConnection) {
                        if (peerNode.canAcceptConnection(receivedPort)) {
                            this.remotePort = receivedPort;
                            this.isValidConnection = true;
                            System.out.println(username + " (port " + remotePort + ") s'est connecte");
                            peerNode.sharePeerList(this);
                        } else {
                            // Port déjà connecté, fermer cette connexion en double
                            try {
                                peerSocket.close();
                            } catch (IOException e) {
                                // Ignorer
                            }
                            return;
                        }
                    } else {
                        // Connexion sortante, juste afficher
                        System.out.println(username + " (port " + remotePort + ") s'est connecte");
                        peerNode.sharePeerList(this);
                    }
                }
                break;

            case "GET_PEERS":
                if (parts.length >= 2) {
                    int requesterPort = Integer.parseInt(parts[1]);
                    peerNode.sharePeerList(this);
                }
                break;

            case "PEERS":
                peerNode.processPeerList(message);
                break;

            case "MSG":
                // MODIFIÉ: Le message a maintenant 5 parties (MSG|sender|text|messageId|timestamp)
                if (isValidConnection && parts.length >= 5) {
                    String sender = parts[1];
                    String msg = parts[2];
                    String messageId = parts[3];
                    long receivedTimestamp = Long.parseLong(parts[4]);

                    // Vérifier si c'est un nouveau message ET mettre à jour Lamport en une seule opération
                    if (peerNode.shouldProcessMessage(messageId, receivedTimestamp)) {
                        // Nouveau message: afficher avec le timestamp ET relayer
                        if (!sender.equals(peerNode.getUsername())) {
                            System.out.println("[Lamport:" + peerNode.getLamportClock() + "] " + sender + ": " + msg);
                        }
                        peerNode.relayMessage(message, this);
                    }
                    // Sinon, déjà vu: on ignore ET on ne met PAS à jour l'horloge
                }
                break;
        }
    }

    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    public void close() {
        // Libérer le port seulement si c'était une connexion valide
        if (isValidConnection && remotePort > 0) {
            peerNode.releasePort(remotePort);
        }

        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (peerSocket != null) peerSocket.close();
        } catch (IOException e) {
            System.err.println("Erreur fermeture: " + e.getMessage());
        }
    }

    public int getRemotePort() {
        return remotePort;
    }
}

