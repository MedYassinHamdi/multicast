package udp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * SERVEUR UDP
 *
 * Application serveur de chat utilisant le protocole UDP.
 * Structure identique au serveur TCP mais utilise DatagramSocket au lieu de ServerSocket.
 *
 * FONCTIONNALITÉS:
 * - Écoute des messages UDP sur un port configuré
 * - Gestion de plusieurs clients simultanés
 * - Routage des messages (broadcast ou unicast)
 * - Transfert de fichiers entre clients
 * - Mise à jour dynamique de la liste des clients
 *
 * DIFFÉRENCE AVEC TCP:
 * - UDP: Un seul socket pour tous les clients, identification par adresse IP + port
 * - TCP: Un socket par client (accept crée un nouveau Socket pour chaque client)
 * - UDP nécessite la gestion manuelle des adresses clients
 */
public class ServeurGUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_PORT = 9999;
    private static final int BUFFER_SIZE = 65535; // Taille maximale d'un paquet UDP

    // ========== RÉSEAU ==========

    /**
     * Socket UDP du serveur
     */
    private DatagramSocket serverSocket;

    /**
     * Indicateur d'état du serveur
     */
    private volatile boolean running = false;

    /**
     * Thread principal d'écoute
     */
    private Thread acceptThread;

    /**
     * Ensemble thread-safe des clients connectés
     */
    private final Set<ClientHandler> clients = new CopyOnWriteArraySet<>();

    // ========== INTERFACE GRAPHIQUE ==========

    /**
     * Zone de logs du serveur
     */
    private JTextArea logArea;

    /**
     * Modèle et liste des clients connectés
     */
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;

    /**
     * Boutons de contrôle
     */
    private JButton btnStart, btnStop;

    /**
     * Champ de configuration du port
     */
    private JTextField txtPort;

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
        super("🖥️ Serveur UDP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
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

        JLabel lpt = new JLabel("Port:"); lpt.setForeground(Color.WHITE);
        c.gridx = 0; top.add(lpt, c);
        txtPort = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        c.gridx = 1; top.add(txtPort, c);

        btnStart = new JButton("Démarrer");
        btnStart.setBackground(new Color(0,150,110));
        btnStart.setForeground(Color.WHITE);
        btnStop = new JButton("Arrêter");
        btnStop.setBackground(new Color(220,20,60));
        btnStop.setForeground(Color.WHITE);
        c.gridx = 2; top.add(btnStart, c);
        c.gridx = 3; top.add(btnStop, c);

        root.add(top, BorderLayout.NORTH);

        // ───────────── ZONE CENTRALE (SPLIT) ─────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.3);

        // Panel gauche: Liste des clients
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Clients connectés"));
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        left.add(new JScrollPane(clientList), BorderLayout.CENTER);

        // Panel droit: Logs
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Logs"));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        right.add(new JScrollPane(logArea), BorderLayout.CENTER);

        split.setLeftComponent(left);
        split.setRightComponent(right);
        root.add(split, BorderLayout.CENTER);

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
     * DÉMARRAGE DU SERVEUR
     *
     * PROCESSUS:
     * 1. Validation du port
     * 2. Création du DatagramSocket
     * 3. Démarrage du thread d'écoute
     *
     * DIFFÉRENCE AVEC TCP:
     * - UDP: DatagramSocket(port) - un seul socket pour tous les clients
     * - TCP: ServerSocket(port) - accept() crée un nouveau Socket par client
     */
    private void startServer() {
        if (running) return;

        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
            if (port <= 0 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            alert("Port invalide.");
            return;
        }

        try {
            // Création du socket UDP
            serverSocket = new DatagramSocket(port);
            running = true;

            // Démarrage du thread d'écoute
            acceptThread = new Thread(this::acceptLoop, "UDP-Listener");
            acceptThread.start();

            append("✅ Serveur démarré sur le port " + port);
            setStatus(true, "En ligne — port " + port);
            updateButtons();
        } catch (IOException ex) {
            alert("Erreur ouverture serveur : " + ex.getMessage());
        }
    }

    /**
     * ARRÊT DU SERVEUR
     *
     * Ferme le socket et arrête tous les threads proprement.
     */
    private void stopServer() {
        running = false;

        // Fermeture du socket UDP
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignore) {}

        // Nettoyage de la liste des clients
        clients.clear();
        refreshClientList();

        // Attente de la fin du thread d'écoute
        if (acceptThread != null && acceptThread.isAlive()) {
            try {
                acceptThread.join(200);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }

        append("⏹️ Serveur arrêté.");
        setStatus(false, "Hors ligne");
        updateButtons();
    }

    /**
     * BOUCLE D'ÉCOUTE DES MESSAGES UDP
     *
     * Reçoit continuellement les paquets UDP et crée/met à jour les clients.
     *
     * DIFFÉRENCE AVEC TCP:
     * - UDP: Une seule boucle qui reçoit tous les messages de tous les clients
     * - TCP: accept() bloquant qui crée un thread par client
     */
    private void acceptLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (running) {
            try {
                // Préparation du paquet de réception
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Réception d'un paquet (bloquant)
                serverSocket.receive(packet);

                // Désérialisation du message
                ByteArrayInputStream bis = new ByteArrayInputStream(
                    packet.getData(), 0, packet.getLength()
                );
                ObjectInputStream ois = new ObjectInputStream(bis);
                Object o = ois.readObject();

                if (!(o instanceof Message msg)) continue;

                // Récupération de l'adresse du client
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                // Recherche ou création du client handler
                ClientHandler handler = findOrCreateClient(msg.sender, clientAddress, clientPort);

                // Traitement du message par le handler
                if (handler != null) {
                    handler.handleMessage(msg);
                }

            } catch (IOException e) {
                if (running) append("⚠️ Erreur réception : " + e.getMessage());
            } catch (ClassNotFoundException e) {
                append("⚠️ Erreur désérialisation : " + e.getMessage());
            }
        }
    }

    /**
     * RECHERCHE OU CRÉATION D'UN CLIENT HANDLER
     *
     * Si le client existe déjà (même pseudo), met à jour son adresse.
     * Sinon, crée un nouveau handler.
     *
     * @param nickname Pseudo du client
     * @param address  Adresse IP du client
     * @param port     Port UDP du client
     * @return ClientHandler trouvé ou créé
     */
    private ClientHandler findOrCreateClient(String nickname, InetAddress address, int port) {
        // Recherche d'un client existant avec le même pseudo
        for (ClientHandler h : clients) {
            if (h.nickname.equals(nickname)) {
                // Mise à jour de l'adresse (le client peut avoir changé de port)
                h.updateAddress(address, port);
                return h;
            }
        }

        // Création d'un nouveau client
        ClientHandler newHandler = new ClientHandler(nickname, address, port);
        clients.add(newHandler);
        append("➕ " + nickname + " connecté (" + address.getHostAddress() + ")");
        refreshClientList();
        broadcastList();

        return newHandler;
    }

    /**
     * DIFFUSION DE LA LISTE DES CLIENTS
     *
     * Envoie la liste à jour de tous les clients connectés.
     */
    private void broadcastList() {
        String list = clients.stream()
            .map(c -> c.nickname)
            .collect(Collectors.joining(","));
        Message listMsg = new Message(Message.Type.LISTE, "Serveur", "Tous", list);
        for (ClientHandler c : clients) c.send(listMsg);
    }

    /**
     * ENVOI D'UN MESSAGE VERS LE(S) DESTINATAIRE(S)
     *
     * Gère le routage broadcast ou unicast.
     *
     * @param msg  Message à envoyer
     * @param from Handler de l'émetteur (pour éviter de lui renvoyer en broadcast)
     */
    private void sendToTarget(Message msg, ClientHandler from) {
        if ("Tous".equalsIgnoreCase(msg.target)) {
            // Broadcast à tous sauf l'émetteur
            for (ClientHandler c : clients) {
                if (c != from) c.send(msg);
            }
            append(msg.sender + " a envoyé '" + msg.text + "' à tous");
        } else {
            // Unicast vers un client spécifique
            for (ClientHandler c : clients) {
                if (c.nickname.equals(msg.target)) {
                    c.send(msg);
                    break;
                }
            }
            append(msg.sender + " a envoyé '" +
                   (msg.text != null ? msg.text : msg.filename) + "' à " + msg.target);
        }
    }

    /**
     * RAFRAÎCHISSEMENT DE LA LISTE DES CLIENTS DANS L'INTERFACE
     */
    private void refreshClientList() {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (ClientHandler c : clients) {
                clientListModel.addElement(c.nickname);
            }
        });
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
        txtPort.setEnabled(!running);
    }

    /**
     * Affiche un message d'alerte
     */
    private void alert(String m) {
        JOptionPane.showMessageDialog(this, m, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // ───────────── CLASSE INTERNE: CLIENT HANDLER ─────────────

    /**
     * HANDLER DE CLIENT UDP
     *
     * Représente un client connecté et gère l'envoi de messages vers lui.
     *
     * DIFFÉRENCE AVEC TCP:
     * - UDP: Stocke l'adresse IP et le port du client
     * - TCP: Possède un Socket dédié avec des flux ObjectInputStream/ObjectOutputStream
     * - UDP: Pas de thread par client, l'envoi est synchrone
     */
    private class ClientHandler {
        /**
         * Pseudo du client
         */
        private final String nickname;

        /**
         * Adresse IP du client (peut changer si le client se reconnecte)
         */
        private InetAddress address;

        /**
         * Port UDP du client (peut changer si le client se reconnecte)
         */
        private int port;

        /**
         * Constructeur
         *
         * @param nickname Pseudo du client
         * @param address  Adresse IP du client
         * @param port     Port UDP du client
         */
        ClientHandler(String nickname, InetAddress address, int port) {
            this.nickname = nickname;
            this.address = address;
            this.port = port;
        }

        /**
         * Met à jour l'adresse du client
         * (utile si le client change de port entre les messages)
         */
        void updateAddress(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        /**
         * TRAITEMENT D'UN MESSAGE REÇU
         *
         * Analyse le message et effectue l'action appropriée.
         *
         * @param msg Message reçu
         */
        void handleMessage(Message msg) {
            // Message HELLO (connexion initiale)
            if (msg.type == Message.Type.TEXTE && "HELLO".equals(msg.target)) {
                // Le client s'est déjà présenté, on a créé son handler
                return;
            }

            // Traitement selon le type
            switch (msg.type) {
                case TEXTE -> sendToTarget(msg, this);

                case FICHIER -> {
                    append(msg.sender + " a envoyé le fichier '" + msg.filename + "' à " +
                            ("Tous".equalsIgnoreCase(msg.target) ? "tous" : msg.target) +
                            " (" + (msg.fileBytes == null ? 0 : msg.fileBytes.length) + " octets)");
                    sendToTarget(msg, this);
                }

                case LISTE -> {
                    // Pas utilisé côté client pour l'instant
                }
            }
        }

        /**
         * ENVOI D'UN MESSAGE AU CLIENT
         *
         * Sérialise le message et l'envoie via UDP.
         *
         * @param msg Message à envoyer
         */
        void send(Message msg) {
            try {
                // Sérialisation du message
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(msg);
                oos.flush();
                byte[] data = bos.toByteArray();

                // Création et envoi du paquet UDP
                DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                serverSocket.send(packet);

            } catch (IOException e) {
                append("⚠️ Erreur envoi vers " + nickname + " : " + e.getMessage());
            }
        }
    }

    /**
     * POINT D'ENTRÉE DE L'APPLICATION
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServeurGUI().setVisible(true));
    }
}

