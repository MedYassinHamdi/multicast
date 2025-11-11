package multicast;

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
 * CLIENT MULTICAST
 *
 * Application client de chat utilisant le protocole multicast IP.
 * Structure identique aux clients TCP/UDP mais utilise MulticastSocket.
 *
 * FONCTIONNALITÉS:
 * - Rejoindre un groupe multicast
 * - Envoi de messages texte au groupe
 * - Envoi de fichiers/images au groupe
 * - Réception et affichage des messages du groupe
 * - Liste des destinataires disponibles
 *
 * DIFFÉRENCE AVEC UDP/TCP:
 * - MULTICAST: MulticastSocket qui rejoint un groupe (joinGroup)
 * - UDP: DatagramSocket point à point avec un serveur
 * - TCP: Socket avec connexion établie
 * - MULTICAST: Tous les membres du groupe reçoivent tous les messages (broadcast naturel)
 *
 * ADRESSE MULTICAST:
 * - Plage: 224.0.0.0 à 239.255.255.255 (classe D)
 * - 224.0.0.0 à 224.0.0.255: Réservées (protocoles de routage)
 * - 239.0.0.0 à 239.255.255.255: Scope organisationnel (recommandé pour applications)
 */
public class Client extends JFrame {
    private static final long serialVersionUID = 1L;

    // Configuration par défaut
    private static final String DEFAULT_GROUP = "230.0.0.0";
    private static final int DEFAULT_PORT = 4446;
    private static final int BUFFER_SIZE = 65535;

    // ========== RÉSEAU ==========

    /**
     * Socket multicast pour rejoindre le groupe et communiquer
     */
    private MulticastSocket socket;

    /**
     * Adresse du groupe multicast (classe D: 224.0.0.0 à 239.255.255.255)
     */
    private InetAddress group;

    /**
     * Port du groupe multicast
     */
    private int port;

    /**
     * Thread de réception des messages du groupe
     */
    private Thread readerThread;

    /**
     * Indicateur de connexion au groupe
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
    private JButton btnSend, btnFile, btnJoin, btnLeave;

    /**
     * Champs de configuration
     */
    private JTextField txtGroup, txtPort, txtPseudo;

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
        super("💬 Client Multicast");
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

        // Groupe multicast
        JLabel lg = new JLabel("Groupe:"); lg.setForeground(Color.WHITE);
        c.gridx = 2; top.add(lg, c);
        txtGroup = new JTextField(DEFAULT_GROUP, 10);
        c.gridx = 3; top.add(txtGroup, c);

        // Port
        JLabel lpt = new JLabel("Port:"); lpt.setForeground(Color.WHITE);
        c.gridx = 4; top.add(lpt, c);
        txtPort = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        c.gridx = 5; top.add(txtPort, c);

        // Boutons de connexion
        btnJoin = new JButton("Rejoindre");
        btnJoin.setBackground(new Color(0, 150, 110));
        btnJoin.setForeground(Color.WHITE);
        btnLeave = new JButton("Quitter");
        btnLeave.setBackground(new Color(220, 20, 60));
        btnLeave.setForeground(Color.WHITE);
        c.gridx = 6; top.add(btnJoin, c);
        c.gridx = 7; top.add(btnLeave, c);

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
        btnJoin.addActionListener(e -> joinGroup());
        btnLeave.addActionListener(e -> leaveGroup());
        btnSend.addActionListener(e -> sendText());
        btnFile.addActionListener(e -> sendFile());
        inputField.addActionListener(e -> sendText());

        updateButtons();

        // Fermeture propre à la fermeture de la fenêtre
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                leaveGroup();
            }
        });
    }

    // ========== MÉTHODES DE CONNEXION ==========

    /**
     * REJOINDRE LE GROUPE MULTICAST
     *
     * PROCESSUS:
     * 1. Validation des paramètres (groupe, port)
     * 2. Création du MulticastSocket
     * 3. Appel de joinGroup() pour rejoindre le groupe multicast
     * 4. Démarrage du thread de réception
     *
     * DIFFÉRENCE AVEC UDP/TCP:
     * - MULTICAST: socket.joinGroup(group) - abonnement au groupe
     * - UDP: Pas d'abonnement, juste création du socket
     * - TCP: Socket.connect() - connexion point à point
     *
     * GROUPE MULTICAST:
     * Le groupe est identifié par une adresse IP de classe D (224.0.0.0 à 239.255.255.255).
     * Tous les membres du groupe reçoivent tous les messages envoyés au groupe.
     */
    private void joinGroup() {
        // Vérification si déjà connecté
        if (connected) {
            info("Déjà membre du groupe.");
            return;
        }

        // Récupération et validation du groupe
        String groupAddr = txtGroup.getText().trim();

        // Validation du port
        int p;
        try {
            p = Integer.parseInt(txtPort.getText().trim());
            if (p <= 0 || p > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            warn("Port invalide.");
            return;
        }

        try {
            // Résolution de l'adresse du groupe multicast
            group = InetAddress.getByName(groupAddr);
            port = p;

            // Vérification que c'est bien une adresse multicast (classe D)
            if (!group.isMulticastAddress()) {
                warn("L'adresse doit être multicast (224.0.0.0 à 239.255.255.255).");
                return;
            }

            // Création du socket multicast
            socket = new MulticastSocket(port);

            // Rejoindre le groupe multicast (abonnement)
            // À partir de ce moment, le socket reçoit tous les messages envoyés au groupe
            socket.joinGroup(group);

            // Activation de la connexion
            connected = true;

            // Démarrage du thread de réception
            readerThread = new Thread(this::readLoop, "Multicast-Reader");
            readerThread.start();

            // Mise à jour de l'interface
            setStatus(true, "Membre du groupe " + groupAddr + ":" + port);
            info("Vous avez rejoint le groupe.");
            updateButtons();

            // Envoi d'un message d'annonce au groupe
            Message hello = new Message(Message.Type.TEXTE, safePseudo(), "Tous", "*** a rejoint le groupe ***");
            sendMessage(hello);

        } catch (IOException ex) {
            warn("Erreur de connexion : " + ex.getMessage());
            leaveGroup();
        }
    }

    /**
     * QUITTER LE GROUPE MULTICAST
     *
     * PROCESSUS:
     * 1. Envoi d'un message d'au revoir
     * 2. Appel de leaveGroup() pour quitter le groupe
     * 3. Fermeture du socket
     * 4. Arrêt du thread de réception
     *
     * DIFFÉRENCE AVEC UDP/TCP:
     * - MULTICAST: socket.leaveGroup(group) - désabonnement du groupe
     * - UDP: Juste fermeture du socket
     * - TCP: socket.close() - fermeture de la connexion
     */
    private void leaveGroup() {
        if (!connected) return;

        // Envoi d'un message d'au revoir au groupe
        try {
            Message bye = new Message(Message.Type.TEXTE, safePseudo(), "Tous", "*** a quitté le groupe ***");
            sendMessage(bye);
        } catch (Exception ignore) {}

        connected = false;

        // Quitter le groupe multicast (désabonnement)
        try {
            if (socket != null && group != null) {
                socket.leaveGroup(group);
            }
        } catch (IOException ignore) {}

        // Fermeture du socket
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
     * BOUCLE DE RÉCEPTION DES MESSAGES DU GROUPE
     *
     * Thread en arrière-plan qui reçoit continuellement les messages multicast.
     *
     * PROCESSUS:
     * 1. Attente d'un paquet multicast (bloquant)
     * 2. Désérialisation du message
     * 3. Traitement selon le type (TEXTE, FICHIER, LISTE)
     *
     * DIFFÉRENCE AVEC UDP:
     * - MULTICAST: Reçoit TOUS les messages du groupe (y compris ses propres messages)
     * - UDP: Reçoit uniquement les messages envoyés directement au client
     *
     * NOTE: Il faut filtrer ses propres messages pour éviter de les afficher deux fois.
     */
    private void readLoop() {
        // Buffer de réception
        byte[] buffer = new byte[BUFFER_SIZE];

        while (connected) {
            try {
                // Préparation du paquet de réception
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Réception d'un paquet multicast (bloquant)
                // Tous les membres du groupe reçoivent ce paquet
                socket.receive(packet);

                // Désérialisation du message
                ByteArrayInputStream bis = new ByteArrayInputStream(
                    packet.getData(), 0, packet.getLength()
                );
                ObjectInputStream ois = new ObjectInputStream(bis);
                Object o = ois.readObject();

                // Vérification du type d'objet reçu
                if (!(o instanceof Message msg)) continue;

                // Filtrage: Ne pas afficher ses propres messages
                // (car on les affiche déjà localement lors de l'envoi)
                if (msg.sender != null && msg.sender.equals(safePseudo())) {
                    continue;
                }

                // Traitement selon le type de message
                switch (msg.type) {
                    case TEXTE -> appendText("💬 " + msg.sender + " : " + msg.text + "\n");

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
                break;
            } catch (Exception ex) {
                if (connected) {
                    warn("Erreur réception : " + ex.getMessage());
                }
                break;
            }
        }
        leaveGroup();
    }

    // ========== MÉTHODES D'ENVOI ==========

    /**
     * ENVOI D'UN MESSAGE TEXTE AU GROUPE
     *
     * Récupère le texte du champ de saisie et l'envoie au groupe multicast.
     *
     * DIFFÉRENCE AVEC UDP/TCP:
     * - MULTICAST: Le message est envoyé à l'adresse du groupe, tous les membres le reçoivent
     * - UDP: Le message est envoyé à une adresse IP spécifique (serveur)
     * - TCP: Le message est envoyé via le flux de la connexion établie
     */
    private void sendText() {
        if (!connected) {
            warn("Rejoignez d'abord un groupe.");
            return;
        }

        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        String target = (String) targetCombo.getSelectedItem();
        if (target == null || target.isBlank()) target = "Tous";

        Message msg = new Message(Message.Type.TEXTE, safePseudo(), target, text);

        try {
            sendMessage(msg);
            appendText("↗️ " + text + "\n");
            inputField.setText("");
        } catch (IOException e) {
            warn("Erreur envoi : " + e.getMessage());
        }
    }

    /**
     * ENVOI D'UN FICHIER AU GROUPE
     *
     * Ouvre un sélecteur de fichier et envoie le fichier choisi au groupe multicast.
     */
    private void sendFile() {
        if (!connected) {
            warn("Rejoignez d'abord un groupe.");
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
     * ENVOI D'UN MESSAGE AU GROUPE MULTICAST
     *
     * Sérialise le message et l'envoie via multicast.
     *
     * @param msg Message à envoyer
     * @throws IOException Si erreur d'envoi
     *
     * DIFFÉRENCE AVEC UDP:
     * - MULTICAST: Même sérialisation, mais envoi vers l'adresse du groupe
     * - UDP: Envoi vers l'adresse du serveur
     *
     * Le paquet est envoyé à l'adresse multicast et au port du groupe.
     * Tous les membres du groupe (incluant l'émetteur) reçoivent le message.
     */
    private void sendMessage(Message msg) throws IOException {
        // Sérialisation du message en bytes
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(msg);
        oos.flush();
        byte[] data = bos.toByteArray();

        // Création et envoi du paquet multicast
        // Le paquet est envoyé à l'adresse du groupe, pas à un destinataire spécifique
        DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
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
        btnJoin.setEnabled(!connected);
        btnLeave.setEnabled(connected);
        txtGroup.setEnabled(!connected);
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

