package chat.p2p;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class PeerNode {
    private int port;
    private String username;
    private String peerId;
    private ServerSocket serverSocket;
    private Set<PeerHandler> connectedPeers;
    private Set<String> knownPeers;
    private Set<String> processedHelloMessages;
    private ConcurrentHashMap<String, Boolean> processedMessages; // AJOUT
    private ConcurrentHashMap<Integer, Boolean> connectedPorts; // AJOUT - tracker les ports connectés
    private AtomicLong lamportClock; // AJOUT - Horloge de Lamport

    public PeerNode(int port, String username) {
        this.port = port;
        this.username = username;
        this.peerId = UUID.randomUUID().toString().substring(0, 8);
        this.connectedPeers = ConcurrentHashMap.newKeySet();
        this.knownPeers = ConcurrentHashMap.newKeySet();
        this.processedHelloMessages = ConcurrentHashMap.newKeySet();
        this.processedMessages = new ConcurrentHashMap<>(); // AJOUT
        this.connectedPorts = new ConcurrentHashMap<>(); // AJOUT
        this.lamportClock = new AtomicLong(0); // AJOUT - Initialiser à 0
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Peer P2P demarre sur le port " + port);
            System.out.println(username + " [ID: " + peerId + "]");

            new Thread(this::acceptConnections).start();

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    discoverInitialPeers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (IOException e) {
            System.err.println("Erreur demarrage: " + e.getMessage());
        }
    }

    private void acceptConnections() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                PeerHandler handler = new PeerHandler(clientSocket, this);
                connectedPeers.add(handler);
                new Thread(handler).start();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("Erreur acceptation: " + e.getMessage());
                }
            }
        }
    }

    public void connectToPeer(String host, int peerPort) {
        // Ne pas se connecter à soi-même
        if (peerPort == this.port) {
            return;
        }

        // Vérifier si déjà connecté à ce port (opération atomique)
        Boolean alreadyConnected = connectedPorts.putIfAbsent(peerPort, Boolean.TRUE);
        if (alreadyConnected != null) {
            return; // Déjà connecté
        }

        String peerKey = host + ":" + peerPort;
        knownPeers.add(peerKey);

        try {
            Socket peerSocket = new Socket(host, peerPort);
            PeerHandler handler = new PeerHandler(peerSocket, this, peerPort);
            connectedPeers.add(handler);
            new Thread(handler).start();
            System.out.println("Connecte a " + host + ":" + peerPort);
        } catch (IOException e) {
            // Échec de connexion, libérer le port
            connectedPorts.remove(peerPort);
            knownPeers.remove(peerKey);
        }
    }

    public void broadcastMessage(String message) {
        // Incrémenter l'horloge pour l'événement d'envoi (Lamport)
        long timestamp = lamportClock.incrementAndGet();

        // MODIFIÉ: Ajouter le timestamp de Lamport au message
        String messageId = peerId + "-" + System.nanoTime();
        String formattedMessage = "MSG|" + username + "|" + message + "|" + messageId + "|" + timestamp;

        // Marquer comme déjà traité avant d'envoyer
        processedMessages.put(messageId, Boolean.TRUE);

        System.out.println("[Lamport:" + timestamp + "] Envoi: " + message);

        for (PeerHandler handler : connectedPeers) {
            handler.sendMessage(formattedMessage);
        }
    }

    // MODIFIÉ: Vérifier l'ID ET mettre à jour Lamport atomiquement
    public boolean shouldProcessMessage(String messageId, long receivedTimestamp) {
        // putIfAbsent retourne null si c'est la première fois
        Boolean alreadySeen = processedMessages.putIfAbsent(messageId, Boolean.TRUE);

        if (alreadySeen == null) {
            // Nouveau message: mettre à jour l'horloge de Lamport
            updateLamportClock(receivedTimestamp);
            return true;
        }

        // Message déjà vu: NE PAS mettre à jour l'horloge
        return false;
    }

    // Mettre à jour l'horloge de Lamport lors de la réception
    public void updateLamportClock(long receivedTimestamp) {
        long currentClock;
        long newClock;
        do {
            currentClock = lamportClock.get();
            newClock = Math.max(currentClock, receivedTimestamp) + 1;
        } while (!lamportClock.compareAndSet(currentClock, newClock));
    }

    public long getLamportClock() {
        return lamportClock.get();
    }

    public void relayMessage(String message, PeerHandler excludeSender) {
        if (!message.startsWith("MSG|")) {
            return;
        }

        // Relayer aux autres pairs
        for (PeerHandler handler : connectedPeers) {
            if (handler != excludeSender) {
                handler.sendMessage(message);
            }
        }
    }

    public void sharePeerList(PeerHandler requester) {
        StringBuilder peerList = new StringBuilder("PEERS|");
        for (PeerHandler handler : connectedPeers) {
            if (handler != requester) {
                peerList.append(handler.getRemotePort()).append(",");
            }
        }

        if (peerList.length() > "PEERS|".length()) {
            requester.sendMessage(peerList.toString());
        }
    }

    public boolean processHelloMessage(String message, PeerHandler sender) {
        if (processedHelloMessages.contains(message)) {
            return false;
        }

        processedHelloMessages.add(message);
        String[] parts = message.split("\\|", 3);
        if (parts.length >= 3) {
            String remotePeerId = parts[1];
            String remoteUsername = parts[2];
            System.out.println(remoteUsername + " s'est connecte");

            sender.sendMessage("GET_PEERS|" + port);
            return true;
        }
        return false;
    }

    public void processPeerList(String peerListMessage) {
        String[] parts = peerListMessage.split("\\|", 2);
        if (parts.length >= 2 && !parts[1].isEmpty()) {
            String[] peerPorts = parts[1].split(",");
            for (String portStr : peerPorts) {
                try {
                    int peerPort = Integer.parseInt(portStr.trim());
                    if (peerPort > 0 && peerPort != port) {
                        connectToPeer("localhost", peerPort);
                    }
                } catch (NumberFormatException e) {
                    // Ignorer les ports invalides
                }
            }
        }
    }

    // NOUVELLE MÉTHODE: Vérifier si on peut accepter une connexion entrante
    public boolean canAcceptConnection(int remotePort) {
        if (remotePort <= 0 || remotePort == this.port) {
            return false;
        }

        // Essayer de réserver ce port atomiquement
        Boolean alreadyConnected = connectedPorts.putIfAbsent(remotePort, Boolean.TRUE);
        return alreadyConnected == null; // true si nouveau, false si déjà existant
    }

    // NOUVELLE MÉTHODE: Libérer un port lors de la déconnexion
    public void releasePort(int remotePort) {
        connectedPorts.remove(remotePort);
        knownPeers.remove("localhost:" + remotePort);
    }

    private void discoverInitialPeers() {
        System.out.println("Recherche de pairs initiaux...");

        int startRange = Math.max(5100, port - 10);
        int endRange = port + 10;

        for (int targetPort = startRange; targetPort <= endRange; targetPort++) {
            if (targetPort != port) {
                tryConnect(targetPort);
            }
        }
    }

    private void tryConnect(int targetPort) {
        if (targetPort < 5100 || targetPort > 6000) return;

        final int portToTry = targetPort;
        new Thread(() -> {
            try {
                Thread.sleep(500 + new Random().nextInt(1000));
                connectToPeer("localhost", portToTry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void displayMessage(String sender, String message) {
        System.out.println(sender + ": " + message);
    }

    public void stop() {
        try {
            for (PeerHandler handler : connectedPeers) {
                handler.close();
            }
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.err.println("Erreur arret: " + e.getMessage());
        }
    }

    public String getUsername() { return username; }
    public String getPeerId() { return peerId; }
    public int getPort() { return port; }
}

