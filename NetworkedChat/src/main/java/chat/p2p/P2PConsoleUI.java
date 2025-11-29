package chat.p2p;

import java.util.Scanner;

public class P2PConsoleUI {
    private PeerNode peerNode;
    private Scanner scanner;

    public P2PConsoleUI(PeerNode peerNode) {
        this.peerNode = peerNode;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("\nChat P2P demarre! Tapez vos messages:");
        System.out.println("/connect <host> <port> - Se connecter manuellement");
        System.out.println("/quit - Quitter\n");

        while (true) {
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("/quit")) {
                peerNode.stop();
                break;
            } else if (input.startsWith("/connect ")) {
                handleConnectCommand(input);
            } else if (!input.isEmpty()) {
                peerNode.broadcastMessage(input);
            }
        }

        scanner.close();
    }

    private void handleConnectCommand(String input) {
        String[] parts = input.split(" ");
        if (parts.length == 3) {
            try {
                String host = parts[1];
                int port = Integer.parseInt(parts[2]);
                peerNode.connectToPeer(host, port);
            } catch (NumberFormatException e) {
                System.out.println("Port invalide");
            }
        } else {
            System.out.println("Usage: /connect <host> <port>");
        }
    }
}