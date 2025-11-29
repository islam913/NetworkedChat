package chat.p2p;

import java.util.Scanner;

public class P2PMain {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Configuration Peer-to-Peer");

        // Port
        System.out.print("Port (defaut 5001): ");
        String portInput = scanner.nextLine();
        int port = portInput.isEmpty() ? 5001 : Integer.parseInt(portInput);

        // Username
        System.out.print("Votre nom: ");
        String username = scanner.nextLine();
        if (username.isEmpty()) username = "User" + port;

        // Demarrer le peer
        PeerNode peer = new PeerNode(port, username);
        peer.start();

        // Interface console
        P2PConsoleUI ui = new P2PConsoleUI(peer);
        ui.start();

        scanner.close();
    }
}