package udp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.nio.file.Files;

/**
 * CLIENT UDP
 *
 * Application client de chat utilisant le protocole UDP.
 * Structure identique au client TCP mais utilise DatagramSocket au lieu de Socket.
 *
 * FONCTIONNALITÉS:
 * - Connexion au serveur UDP
 * - Envoi de messages texte
 * - Envoi de fichiers/images
 * - Réception et affichage des messages
 * - Liste des destinataires disponibles
 *
 * DIFFÉRENCE AVEC TCP:
 * - UDP: Sans connexion, utilise DatagramSocket et DatagramPacket
 * - TCP: Avec connexion, utilise Socket avec flux ObjectInputStream/ObjectOutputStream
 * - UDP nécessite la sérialisation manuelle des objets Message en bytes
 */
public class Client extends JFrame {
    private static final long serialVersionUID = 1L;

    // Configuration par défaut
    private static final int BUFFER_SIZE = 65535; // Taille maximale d'un paquet UDP

    // ========== RÉSEAU ==========

    /**
     * Socket UDP pour la communication avec le serveur
     */
    private DatagramSocket socket;

    /**
     * Adresse IP du serveur UDP
     */
    private InetAddress serverAddress;

    /**
     * Port du serveur UDP
     */
    private int serverPort;

    /**
     * Thread de réception des messages
     */
    private Thread readerThread;

    /**
     * Indicateur de connexion active
     */
    private volatile boolean connected = false;

    // ========== INTERFACE GRAPHIQUE ==========

    /**
     * Zone d'affichage des messages (avec support des images)
     */
    private JTextPane chatPane;

    /**
     * Champ de saisie des messages
     */
    private JTextField inputField;

    /**
     * Boutons d'action
     */
    private JButton btnSend, btnFile, btnConnect, btnDisconnect;

    /**
     * Champs de configuration
     */
    private JTextField txtHost, txtPort, txtPseudo;

    /**
     * Liste déroulante des destinataires
     */
    private DefaultComboBoxModel<String> targetModel;
    private JComboBox<String> targetCombo;

    /**
     * Label d'état de la connexion
     */
    private JLabel statusLabel;

    /**
     * CONSTRUCTEUR
     *
     * Initialise l'interface graphique et configure les écouteurs d'événements.
     */
    public Client() {
        super("💬 Client UDP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));
        setContentPane(root);

        // ───────────── BARRE DE CONNEXION (TOP) ─────────────
        JPanel top = new JPanel(new GridBagLayout());
        top.setBackground(new Color(21, 64, 160));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.gridy = 0;

        // Pseudo
        JLabel lp = new JLabel("Pseudo:"); lp.setForeground(Color.WHITE);
        c.gridx = 0; top.add(lp, c);
        txtPseudo = new JTextField("User", 10);
        c.gridx = 1; top.add(txtPseudo, c);

        // Hôte
        JLabel lh = new JLabel("Hôte:"); lh.setForeground(Color.WHITE);
        c.gridx = 2; top.add(lh, c);
        txtHost = new JTextField("127.0.0.1", 10);
        c.gridx = 3; top.add(txtHost, c);

        // Port
        JLabel lpt = new JLabel("Port:"); lpt.setForeground(Color.WHITE);
        c.gridx = 4; top.add(lpt, c);
        txtPort = new JTextField("9999", 6);
        c.gridx = 5; top.add(txtPort, c);

        // Boutons de connexion
        btnConnect = new JButton("Se connecter");
        btnConnect.setBackground(new Color(0, 150, 110));
        btnConnect.setForeground(Color.WHITE);
        btnDisconnect = new JButton("Déconnexion");
        btnDisconnect.setBackground(new Color(220, 20, 60));
        btnDisconnect.setForeground(Color.WHITE);
        c.gridx = 6; top.add(btnConnect, c);
        c.gridx = 7; top.add(btnDisconnect, c);

        root.add(top, BorderLayout.NORTH);

        // ───────────── ZONE CENTRALE (SPLIT) ─────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.25);

        // Panel gauche: Destinataire
        JPanel left = new JPanel(new GridBagLayout());
        left.setBorder(BorderFactory.createTitledBorder("Destinataire"));
        GridBagConstraints lc = new GridBagConstraints();
        lc.insets = new Insets(6,6,6,6);
        lc.gridx = 0; lc.gridy = 0; lc.fill = GridBagConstraints.HORIZONTAL; lc.weightx = 1.0;
        targetModel = new DefaultComboBoxModel<>();
        targetModel.addElement("Tous");
        targetCombo = new JComboBox<>(targetModel);
        left.add(targetCombo, lc);

        // Panel droit: Discussion
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Discussion"));
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setContentType("text/plain");
        right.add(new JScrollPane(chatPane), BorderLayout.CENTER);

        split.setLeftComponent(left);
        split.setRightComponent(right);
        root.add(split, BorderLayout.CENTER);

        // ───────────── ZONE INFÉRIEURE (SAISIE + STATUT) ─────────────
        JPanel south = new JPanel(new BorderLayout(8,8));

        JPanel bottom = new JPanel(new BorderLayout(8,8));
        inputField = new JTextField();
        JPanel buttons = new JPanel();
        btnFile = new JButton("📎 Image / Fichier");
        btnSend = new JButton("Envoyer");
        btnFile.setBackground(new Color(99,102,241));
        btnFile.setForeground(Color.WHITE);
        btnSend.setBackground(new Color(59,130,246));
        btnSend.setForeground(Color.WHITE);
        buttons.add(btnFile);
        buttons.add(btnSend);
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);

        statusLabel = new JLabel("Hors ligne");
        statusLabel.setForeground(new Color(160,0,0));

        south.add(bottom, BorderLayout.CENTER);
        south.add(statusLabel, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        // ───────────── CONFIGURATION DES ACTIONS ─────────────
        btnConnect.addActionListener(e -> connect());
        btnDisconnect.addActionListener(e -> disconnect());
        btnSend.addActionListener(e -> sendText());
        btnFile.addActionListener(e -> sendFile());
        inputField.addActionListener(e -> sendText());

        updateButtons();

        // Fermeture propre à la fermeture de la fenêtre
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }

    // ========== MÉTHODES DE CONNEXION ==========

    /**
     * CONNEXION AU SERVEUR UDP
     *
     * PROCESSUS:
     * 1. Validation des paramètres (host, port)
     * 2. Création du DatagramSocket
     * 3. Résolution de l'adresse du serveur
     * 4. Envoi d'un message HELLO pour s'identifier
     * 5. Démarrage du thread de réception
     *
     * DIFFÉRENCE AVEC TCP:
     * - UDP: Pas de connexion établie, juste création du socket
     * - TCP: Connexion explicite avec Socket(host, port)
     */
    private void connect() {
        // Vérification si déjà connecté
        if (connected) {
            info("Déjà connecté.");
            return;
        }

        // Récupération et validation de l'hôte
        String host = txtHost.getText().trim();

        // Validation du port
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
            if (port <= 0 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            warn("Port invalide.");
            return;
        }

        try {
            // Création du socket UDP
            socket = new DatagramSocket();

            // Résolution de l'adresse du serveur
            serverAddress = InetAddress.getByName(host);
            serverPort = port;

            // Envoi d'un message HELLO pour s'identifier au serveur
            Message hello = new Message(Message.Type.TEXTE, safePseudo(), "HELLO", "hello");
            sendMessage(hello);

            // Activation de la connexion
            connected = true;

            // Démarrage du thread de réception
            readerThread = new Thread(this::readLoop, "UDP-Reader");
            readerThread.start();

            // Mise à jour de l'interface
            setStatus(true, "Connecté — " + host + ":" + port);
            info("Connecté au serveur.");
            updateButtons();

        } catch (IOException ex) {
            warn("Connexion échouée : " + ex.getMessage());
            disconnect();
        }
    }

    /**
     * DÉCONNEXION DU SERVEUR
     *
     * Ferme le socket et arrête le thread de réception proprement.
     */
    private void disconnect() {
        connected = false;

        // Fermeture du socket UDP
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        socket = null;

        // Attente de la fin du thread de réception
        if (readerThread != null && readerThread.isAlive()) {
            try {
                readerThread.join(200);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }

        // Mise à jour de l'interface
        setStatus(false, "Hors ligne");
        updateButtons();
    }

    /**
     * BOUCLE DE RÉCEPTION DES MESSAGES
     *
     * Thread en arrière-plan qui reçoit continuellement les paquets UDP
     * et traite les messages reçus.
     *
     * PROCESSUS:
     * 1. Attente d'un paquet UDP (bloquant)
     * 2. Désérialisation du message
     * 3. Traitement selon le type (TEXTE, FICHIER, LISTE)
     */
    private void readLoop() {
        // Buffer de réception
        byte[] buffer = new byte[BUFFER_SIZE];

        while (connected) {
            try {
                // Préparation du paquet de réception
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Réception d'un paquet (bloquant)
                socket.receive(packet);

                // Désérialisation du message
                ByteArrayInputStream bis = new ByteArrayInputStream(
                    packet.getData(), 0, packet.getLength()
                );
                ObjectInputStream ois = new ObjectInputStream(bis);
                Object o = ois.readObject();

                // Vérification du type d'objet reçu
                if (!(o instanceof Message msg)) continue;

                // Traitement selon le type de message
                switch (msg.type) {
                    case TEXTE -> appendText("💬 " + msg.sender + " → " + msg.target + " : " + msg.text + "\n");

                    case FICHIER -> {
                        if (msg.fileBytes != null && msg.filename != null) {
                            String lower = msg.filename.toLowerCase();
                            // Affichage des images inline
                            if (lower.endsWith(".png") || lower.endsWith(".jpg") ||
                                lower.endsWith(".jpeg") || lower.endsWith(".gif")) {
                                appendImage(new ImageIcon(msg.fileBytes));
                            }
                            appendText("🗂️ Fichier reçu de " + msg.sender + " : " + msg.filename +
                                       " (" + msg.fileBytes.length + " octets)\n");
                        }
                    }

                    case LISTE -> SwingUtilities.invokeLater(() -> {
                        // Mise à jour de la liste des destinataires
                        targetModel.removeAllElements();
                        targetModel.addElement("Tous");
                        String[] users = (msg.text == null ? "" : msg.text).split(",");
                        for (String u : users) {
                            if (u != null && !u.isBlank()) {
                                targetModel.addElement(u.trim());
                            }
                        }
                    });
                }

            } catch (EOFException eof) {
                info("Serveur fermé.");
                break;
            } catch (Exception ex) {
                if (connected) {
                    warn("Erreur réception : " + ex.getMessage());
                }
                break;
            }
        }
        disconnect();
    }

    // ========== MÉTHODES D'ENVOI ==========

    /**
     * ENVOI D'UN MESSAGE TEXTE
     *
     * Récupère le texte du champ de saisie et l'envoie au serveur.
     */
    private void sendText() {
        if (!connected) {
            warn("Connectez-vous d'abord.");
            return;
        }

        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        String target = (String) targetCombo.getSelectedItem();
        if (target == null || target.isBlank()) target = "Tous";

        Message msg = new Message(Message.Type.TEXTE, safePseudo(), target, text);

        try {
            sendMessage(msg);
            appendText("↗️ (" + target + ") " + text + "\n");
            inputField.setText("");
        } catch (IOException e) {
            warn("Erreur envoi : " + e.getMessage());
        }
    }

    /**
     * ENVOI D'UN FICHIER
     *
     * Ouvre un sélecteur de fichier et envoie le fichier choisi au serveur.
     */
    private void sendFile() {
        if (!connected) {
            warn("Connectez-vous d'abord.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File f = chooser.getSelectedFile();
        try {
            // Lecture du fichier en bytes
            byte[] bytes = Files.readAllBytes(f.toPath());

            String target = (String) targetCombo.getSelectedItem();
            if (target == null || target.isBlank()) target = "Tous";

            Message msg = new Message(Message.Type.FICHIER, safePseudo(), target, f.getName(), bytes);
            sendMessage(msg);

            appendText("↗️ Fichier envoyé : " + f.getName() + " (" + bytes.length + " octets)\n");
        } catch (IOException e) {
            warn("Erreur envoi fichier : " + e.getMessage());
        }
    }

    /**
     * ENVOI D'UN MESSAGE AU SERVEUR
     *
     * Sérialise le message et l'envoie via UDP.
     *
     * @param msg Message à envoyer
     * @throws IOException Si erreur d'envoi
     */
    private void sendMessage(Message msg) throws IOException {
        // Sérialisation du message en bytes
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(msg);
        oos.flush();
        byte[] data = bos.toByteArray();

        // Création et envoi du paquet UDP
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
        socket.send(packet);
    }

    // ========== MÉTHODES D'AIDE À L'INTERFACE ==========

    /**
     * Ajoute du texte à la zone de discussion
     */
    private void appendText(String text) {
        SwingUtilities.invokeLater(() -> {
            Document doc = chatPane.getDocument();
            try {
                doc.insertString(doc.getLength(), text, null);
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    /**
     * Ajoute une image à la zone de discussion
     */
    private void appendImage(ImageIcon icon) {
        SwingUtilities.invokeLater(() -> {
            chatPane.setCaretPosition(chatPane.getDocument().getLength());
            chatPane.insertIcon(icon);
            appendText("\n");
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
     * Active/désactive les boutons selon l'état de connexion
     */
    private void updateButtons() {
        btnConnect.setEnabled(!connected);
        btnDisconnect.setEnabled(connected);
        txtHost.setEnabled(!connected);
        txtPort.setEnabled(!connected);
        txtPseudo.setEnabled(!connected);
        btnSend.setEnabled(connected);
        btnFile.setEnabled(connected);
        targetCombo.setEnabled(connected);
    }

    /**
     * Retourne le pseudo saisi (ou "User" par défaut)
     */
    private String safePseudo() {
        String p = txtPseudo.getText();
        if (p == null) p = "";
        p = p.trim();
        if (p.isEmpty()) p = "User";
        return p;
    }

    /**
     * Affiche un message d'information
     */
    private void info(String m){
        JOptionPane.showMessageDialog(this, m, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Affiche un message d'avertissement
     */
    private void warn(String m){
        JOptionPane.showMessageDialog(this, m, "Attention", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * POINT D'ENTRÉE DE L'APPLICATION
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client().setVisible(true));
    }
}

