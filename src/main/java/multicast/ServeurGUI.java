package multicast;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;

/**
 * SERVEUR RELAI MULTICAST
 *
 * Application serveur relai pour le chat multicast.
 * Structure identique aux serveurs TCP/UDP mais utilise MulticastSocket.
 *
 * FONCTIONNALITÉS:
 * - Écoute les messages du groupe multicast
 * - Affiche les logs de tous les messages transitant
 * - Peut retransmettre/relayer les messages (optionnel)
 * - Monitoring du trafic du groupe
 *
 * DIFFÉRENCE AVEC UDP/TCP:
 * - MULTICAST: Serveur relai optionnel, le groupe fonctionne en peer-to-peer
 * - UDP: Serveur central obligatoire pour router les messages
 * - TCP: Serveur central obligatoire pour accepter les connexions
 *
 * RÔLE DU SERVEUR MULTICAST:
 * En multicast pur, les clients communiquent directement via le groupe.
 * Ce serveur est un "relai" optionnel qui:
 * 1. Monitore le trafic du groupe (logs)
 * 2. Peut filtrer/modifier les messages
 * 3. Peut gérer une liste de participants
 * 4. Fournit une interface d'administration
 *
 * ARCHITECTURE MULTICAST:
 * - Tous les membres (clients + serveur) rejoignent le même groupe
 * - Chaque message envoyé au groupe est reçu par tous les membres
 * - Pas de routage centralisé, c'est le réseau IP qui gère la diffusion
 */
public class ServeurGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    // Configuration par défaut
    private static final String DEFAULT_GROUP = "230.0.0.0";
    private static final int DEFAULT_PORT = 4446;
    private static final int BUFFER_SIZE = 65535;

    // ========== RÉSEAU ==========

    /**
     * Socket multicast du serveur relai
     */
    private MulticastSocket socket;

    /**
     * Adresse du groupe multicast
     */
    private InetAddress group;

    /**
     * Port du groupe multicast
     */
    private int port;

    /**
     * Indicateur d'état du serveur
     */
    private volatile boolean running = false;

    /**
     * Thread d'écoute du groupe
     */
    private Thread listenerThread;

    // ========== INTERFACE GRAPHIQUE ==========

    /**
     * Zone de logs du serveur
     */
    private JTextArea logArea;

    /**
     * Boutons de contrôle
     */
    private JButton btnStart, btnStop;

    /**
     * Champs de configuration
     */
    private JTextField txtGroup, txtPort;

    /**
     * Label d'état du serveur
     */
    private JLabel statusLabel;

    /**
     * CONSTRUCTEUR
     *
     * Initialise l'interface graphique et configure les écouteurs d'événements.
     */
    public ServeurGUI() {
        super("🛰️ Serveur Relai Multicast");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 480);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        // ───────────── BARRE DE CONTRÔLE (TOP) ─────────────
        JPanel top = new JPanel(new GridBagLayout());
        top.setBackground(new Color(21, 64, 160));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.gridy = 0;

        // Groupe multicast
        JLabel lg = new JLabel("Groupe:"); lg.setForeground(Color.WHITE);
        c.gridx = 0; top.add(lg, c);
        txtGroup = new JTextField(DEFAULT_GROUP, 12);
        c.gridx = 1; top.add(txtGroup, c);

        // Port
        JLabel lpt = new JLabel("Port:"); lpt.setForeground(Color.WHITE);
        c.gridx = 2; top.add(lpt, c);
        txtPort = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        c.gridx = 3; top.add(txtPort, c);

        // Boutons
        btnStart = new JButton("Démarrer");
        btnStart.setBackground(new Color(0,150,110));
        btnStart.setForeground(Color.WHITE);
        btnStop = new JButton("Arrêter");
        btnStop.setBackground(new Color(220,20,60));
        btnStop.setForeground(Color.WHITE);
        c.gridx = 4; top.add(btnStart, c);
        c.gridx = 5; top.add(btnStop, c);

        root.add(top, BorderLayout.NORTH);

        // ───────────── ZONE DE LOGS (CENTER) ─────────────
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createTitledBorder("Logs du serveur"));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        center.add(new JScrollPane(logArea), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        // ───────────── BARRE DE STATUT (BOTTOM) ─────────────
        statusLabel = new JLabel("Hors ligne");
        statusLabel.setForeground(new Color(160,0,0));
        root.add(statusLabel, BorderLayout.SOUTH);

        // ───────────── CONFIGURATION DES ACTIONS ─────────────
        btnStart.addActionListener(e -> startServer());
        btnStop.addActionListener(e -> stopServer());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e){
                stopServer();
            }
        });

        updateButtons();
    }

    /**
     * DÉMARRAGE DU SERVEUR RELAI
     *
     * PROCESSUS:
     * 1. Validation des paramètres (groupe, port)
     * 2. Création du MulticastSocket
     * 3. Appel de joinGroup() pour rejoindre le groupe
     * 4. Démarrage du thread d'écoute
     *
     * DIFFÉRENCE AVEC UDP/TCP:
     * - MULTICAST: socket.joinGroup(group) - rejoint le groupe comme un client
     * - UDP: new DatagramSocket(port) - écoute sur un port spécifique
     * - TCP: new ServerSocket(port) - attend des connexions
     *
     * RÔLE:
     * Le serveur rejoint le groupe et reçoit tous les messages comme les clients.
     * Il peut:
     * - Logger tous les messages (monitoring)
     * - Filtrer certains messages
     * - Gérer une liste de participants
     * - Retransmettre vers d'autres groupes/protocoles
     */
    private void startServer() {
        if (running) return;

        // Récupération et validation du groupe
        String groupAddr = txtGroup.getText().trim();

        // Validation du port
        int p;
        try {
            p = Integer.parseInt(txtPort.getText().trim());
            if (p <= 0 || p > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            alert("Port invalide.");
            return;
        }

        try {
            // Résolution de l'adresse du groupe
            group = InetAddress.getByName(groupAddr);
            port = p;

            // Vérification que c'est bien une adresse multicast
            if (!group.isMulticastAddress()) {
                alert("L'adresse doit être multicast (224.0.0.0 à 239.255.255.255).");
                return;
            }

            // Création du socket multicast
            socket = new MulticastSocket(port);

            // Rejoindre le groupe multicast
            // Le serveur devient membre du groupe et reçoit tous les messages
            socket.joinGroup(group);

            running = true;

            // Démarrage du thread d'écoute
            listenerThread = new Thread(this::listenLoop, "Multicast-Listener");
            listenerThread.start();

            append("✅ Serveur démarré - Groupe " + groupAddr + ":" + port);
            append("📡 Écoute du trafic multicast...");
            setStatus(true, "En ligne — " + groupAddr + ":" + port);
            updateButtons();
        } catch (IOException ex) {
            alert("Erreur démarrage serveur : " + ex.getMessage());
        }
    }

    /**
     * ARRÊT DU SERVEUR RELAI
     *
     * PROCESSUS:
     * 1. Quitter le groupe multicast (leaveGroup)
     * 2. Fermer le socket
     * 3. Arrêter le thread d'écoute
     */
    private void stopServer() {
        running = false;

        // Quitter le groupe multicast
        try {
            if (socket != null && group != null) {
                socket.leaveGroup(group);
            }
        } catch (IOException ignore) {}

        // Fermeture du socket
        try {
            if (socket != null) socket.close();
        } catch (Exception ignore) {}

        // Attente de la fin du thread d'écoute
        if (listenerThread != null && listenerThread.isAlive()) {
            try {
                listenerThread.join(200);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }

        append("⏹️ Serveur arrêté.");
        setStatus(false, "Hors ligne");
        updateButtons();
    }

    /**
     * BOUCLE D'ÉCOUTE DU GROUPE MULTICAST
     *
     * Thread en arrière-plan qui reçoit et logue tous les messages du groupe.
     *
     * PROCESSUS:
     * 1. Attente d'un paquet multicast (bloquant)
     * 2. Désérialisation du message
     * 3. Affichage dans les logs
     * 4. Optionnel: retransmission/filtrage
     *
     * DIFFÉRENCE AVEC UDP:
     * - MULTICAST: Reçoit tous les messages du groupe (broadcast naturel)
     * - UDP: Reçoit uniquement les messages envoyés au serveur spécifiquement
     *
     * NOTE:
     * Le serveur reçoit aussi ses propres messages s'il en envoie.
     */
    private void listenLoop() {
        // Buffer de réception
        byte[] buffer = new byte[BUFFER_SIZE];

        while (running) {
            try {
                // Préparation du paquet de réception
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Réception d'un paquet multicast (bloquant)
                socket.receive(packet);

                // Récupération de l'adresse de l'émetteur
                InetAddress senderAddr = packet.getAddress();
                int senderPort = packet.getPort();

                // Désérialisation du message
                ByteArrayInputStream bis = new ByteArrayInputStream(
                    packet.getData(), 0, packet.getLength()
                );
                ObjectInputStream ois = new ObjectInputStream(bis);
                Object o = ois.readObject();

                // Vérification du type d'objet reçu
                if (!(o instanceof Message msg)) continue;

                // Logging selon le type de message
                switch (msg.type) {
                    case TEXTE -> {
                        append("📩 [" + msg.sender + "] → [" + msg.target + "] : " + msg.text);
                        append("   └─ Depuis " + senderAddr.getHostAddress() + ":" + senderPort);
                    }

                    case FICHIER -> {
                        append("📎 [" + msg.sender + "] → [" + msg.target + "] : Fichier '" +
                               msg.filename + "' (" + (msg.fileBytes == null ? 0 : msg.fileBytes.length) + " octets)");
                        append("   └─ Depuis " + senderAddr.getHostAddress() + ":" + senderPort);
                    }

                    case LISTE -> {
                        append("📋 [" + msg.sender + "] Demande/Réponse de liste");
                        append("   └─ Depuis " + senderAddr.getHostAddress() + ":" + senderPort);
                    }
                }

                // OPTION: Retransmission du message (relay)
                // Si on veut que le serveur retransmette les messages:
                // relayMessage(msg);

            } catch (SocketException e) {
                // Socket fermé (arrêt normal)
                if (running) {
                    append("⚠️ Socket fermé: " + e.getMessage());
                }
                break;

            } catch (IOException e) {
                // Erreur réseau
                if (running) {
                    append("⚠️ Erreur réception: " + e.getMessage());
                }
                break;

            } catch (ClassNotFoundException e) {
                // Erreur de désérialisation
                append("❌ Erreur désérialisation: " + e.getMessage());

            } catch (Exception e) {
                // Autres erreurs
                append("❌ Erreur inattendue: " + e.getMessage());
            }
        }
    }

    /**
     * RETRANSMISSION D'UN MESSAGE (OPTIONNEL)
     *
     * Cette méthode peut être utilisée pour retransmettre les messages reçus.
     * Utile pour:
     * - Filtrer certains messages
     * - Modifier/enrichir les messages
     * - Relayer vers d'autres groupes/protocoles
     *
     * @param msg Message à retransmettre
     *
     * NOTE: Décommentez l'appel dans listenLoop() pour activer
     */
    @SuppressWarnings("unused")
    private void relayMessage(Message msg) {
        try {
            // Sérialisation du message
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(msg);
            oos.flush();
            byte[] data = bos.toByteArray();

            // Retransmission au groupe
            DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
            socket.send(packet);

            append("🔄 Message retransmis");

        } catch (IOException e) {
            append("⚠️ Erreur retransmission: " + e.getMessage());
        }
    }

    /**
     * Ajoute un message aux logs
     */
    private void append(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Met à jour le label de statut
     */
    private void setStatus(boolean on, String text) {
        statusLabel.setText(text);
        statusLabel.setForeground(on ? new Color(0,128,0) : new Color(160,0,0));
    }

    /**
     * Active/désactive les boutons selon l'état du serveur
     */
    private void updateButtons() {
        btnStart.setEnabled(!running);
        btnStop.setEnabled(running);
        txtGroup.setEnabled(!running);
        txtPort.setEnabled(!running);
    }

    /**
     * Affiche un message d'alerte
     */
    private void alert(String m) {
        JOptionPane.showMessageDialog(this, m, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * POINT D'ENTRÉE DE L'APPLICATION
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServeurGUI().setVisible(true));
    }
}

