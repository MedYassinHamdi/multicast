package multicast;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ÉMETTEUR MULTICAST (SENDER)
 *
 * Cette classe implémente l'émetteur qui envoie des messages texte et des images
 * via multicast UDP à tous les récepteurs du groupe.
 *
 * RÔLE DE L'ÉMETTEUR:
 * - Créer et configurer un socket multicast pour l'envoi
 * - Permettre à l'utilisateur d'envoyer des messages texte ou des images
 * - Sérialiser les messages en MulticastMessage
 * - Envoyer les paquets UDP à l'adresse et au port multicast configurés
 * - Valider les tailles pour respecter les limites UDP
 *
 * ARCHITECTURE RÉSEAU:
 * - Utilise MulticastSocket pour l'envoi UDP
 * - Ne "rejoint" PAS le groupe (seuls les récepteurs le font)
 * - Envoie simplement à l'adresse multicast (comme poster une lettre)
 * - Plusieurs émetteurs peuvent coexister sans conflit
 *
 * FONCTIONNEMENT:
 * 1. L'émetteur crée un MulticastSocket (sans bind à un port spécifique)
 * 2. Il résout l'adresse multicast (230.0.0.1)
 * 3. Pour envoyer:
 *    a) Crée un MulticastMessage (texte ou image)
 *    b) Sérialise en bytes avec toBytes()
 *    c) Crée un DatagramPacket avec les bytes, l'adresse et le port
 *    d) Envoie via socket.send()
 * 4. Tous les récepteurs qui ont rejoint le groupe reçoivent le message
 */
public class MulticastSender extends JFrame {
    // ========== COMPOSANTS GUI (non essentiels pour l'examen) ==========
    private JTextArea messageArea;
    private JButton sendTextButton;
    private JButton sendImageButton;
    private JTextArea logArea;
    private JLabel imagePreviewLabel;

    // ========== COMPOSANTS RÉSEAU (ESSENTIELS) ==========

    /**
     * Socket multicast pour l'envoi de paquets UDP
     *
     * MULTICASTSOCKET VS DATAGRAMSOCKET:
     * - MulticastSocket hérite de DatagramSocket
     * - Ajoute les méthodes joinGroup() et leaveGroup()
     * - Pour l'émetteur, on pourrait utiliser DatagramSocket simple
     * - Mais MulticastSocket est plus explicite et permet d'éventuellement
     *   recevoir ses propres messages si on rejoint le groupe
     */
    private MulticastSocket socket;

    /**
     * Adresse IP du groupe multicast
     *
     * - InetAddress représente une adresse IP (IPv4 ou IPv6)
     * - Ici, elle contient l'adresse 230.0.0.1
     * - Utilisée comme destination pour tous les paquets envoyés
     * - Tous les récepteurs écoutent cette adresse
     */
    private InetAddress group;

    /**
     * Indicateur d'état de l'émetteur
     * - true = le socket est initialisé et prêt à envoyer
     * - false = le socket est fermé ou non initialisé
     */
    private boolean isRunning = false;

    // ========== GESTION DES IMAGES ==========
    private File selectedImageFile;       // Fichier image sélectionné par l'utilisateur
    private BufferedImage selectedImage;  // Image chargée en mémoire

    public MulticastSender() {
        super("Multicast Sender");
        initializeUI();
        initializeNetwork();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Title Panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(70, 130, 180));
        JLabel titleLabel = new JLabel("📡 Multicast Message & Image Sender");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // Main Content Panel
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left Panel - Text Message
        JPanel textPanel = new JPanel(new BorderLayout(5, 5));
        textPanel.setBorder(BorderFactory.createTitledBorder("Text Message"));

        messageArea = new JTextArea(8, 30);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 14));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScroll = new JScrollPane(messageArea);
        textPanel.add(messageScroll, BorderLayout.CENTER);

        sendTextButton = new JButton("📤 Send Text Message");
        sendTextButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendTextButton.setBackground(new Color(34, 139, 34));
        sendTextButton.setForeground(Color.WHITE);
        sendTextButton.setFocusPainted(false);
        sendTextButton.addActionListener(e -> sendTextMessage());
        textPanel.add(sendTextButton, BorderLayout.SOUTH);

        mainPanel.add(textPanel);

        // Right Panel - Image
        JPanel imagePanel = new JPanel(new BorderLayout(5, 5));
        imagePanel.setBorder(BorderFactory.createTitledBorder("Image"));

        imagePreviewLabel = new JLabel("No image selected", SwingConstants.CENTER);
        imagePreviewLabel.setPreferredSize(new Dimension(300, 200));
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        imagePreviewLabel.setBackground(Color.WHITE);
        imagePreviewLabel.setOpaque(true);
        JScrollPane imageScroll = new JScrollPane(imagePreviewLabel);
        imagePanel.add(imageScroll, BorderLayout.CENTER);

        JPanel imageButtonPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        JButton selectImageButton = new JButton("🖼️ Select Image");
        selectImageButton.setFont(new Font("Arial", Font.BOLD, 13));
        selectImageButton.setBackground(new Color(70, 130, 180));
        selectImageButton.setForeground(Color.WHITE);
        selectImageButton.setFocusPainted(false);
        selectImageButton.addActionListener(e -> selectImage());
        imageButtonPanel.add(selectImageButton);

        sendImageButton = new JButton("📸 Send Image");
        sendImageButton.setFont(new Font("Arial", Font.BOLD, 13));
        sendImageButton.setBackground(new Color(255, 140, 0));
        sendImageButton.setForeground(Color.WHITE);
        sendImageButton.setFocusPainted(false);
        sendImageButton.setEnabled(false);
        sendImageButton.addActionListener(e -> sendImage());
        imageButtonPanel.add(sendImageButton);

        imagePanel.add(imageButtonPanel, BorderLayout.SOUTH);

        mainPanel.add(imagePanel);

        add(mainPanel, BorderLayout.CENTER);

        // Bottom Panel - Log Area
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JLabel logLabel = new JLabel("Activity Log:");
        logLabel.setFont(new Font("Arial", Font.BOLD, 12));
        bottomPanel.add(logLabel, BorderLayout.NORTH);

        logArea = new JTextArea(8, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        logArea.setBackground(new Color(245, 245, 245));
        JScrollPane logScroll = new JScrollPane(logArea);
        bottomPanel.add(logScroll, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        // Info Panel
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel addressLabel = new JLabel("Multicast: " + MulticastConfig.MULTICAST_ADDRESS);
        JLabel portLabel = new JLabel("Port: " + MulticastConfig.PORT);
        addressLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        portLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoPanel.add(addressLabel);
        infoPanel.add(portLabel);
        add(infoPanel, BorderLayout.WEST);

        // Add enter key listener to message area
        messageArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendTextMessage();
                }
            }
        });

        setSize(900, 700);
        setLocationRelativeTo(null);
    }

    /**
     * INITIALISATION DU RÉSEAU (MÉTHODE ESSENTIELLE)
     *
     * Cette méthode configure le socket multicast pour l'envoi de messages.
     * Elle est appelée automatiquement à la création du sender.
     *
     * ÉTAPES D'INITIALISATION:
     * 1. Créer un MulticastSocket (pour l'envoi UDP multicast)
     * 2. Résoudre l'adresse multicast (String -> InetAddress)
     * 3. Marquer l'émetteur comme actif
     *
     * POINTS TECHNIQUES IMPORTANTS:
     * - L'émetteur NE rejoint PAS le groupe multicast (pas de joinGroup())
     * - Seuls les récepteurs rejoignent le groupe pour recevoir
     * - L'émetteur se contente d'envoyer à l'adresse multicast
     * - Le socket n'est pas lié (bind) à un port spécifique
     * - Le système d'exploitation choisit un port éphémère pour l'envoi
     */
    private void initializeNetwork() {
        try {
            // Création du socket multicast
            // Note: pas de numéro de port car l'émetteur n'écoute pas
            // Le socket sera utilisé uniquement pour envoyer (send)
            socket = new MulticastSocket();

            // Résolution de l'adresse multicast
            // Convertit "230.0.0.1" (String) en InetAddress
            // InetAddress.getByName() peut lever une UnknownHostException
            // si l'adresse est invalide
            group = InetAddress.getByName(MulticastConfig.MULTICAST_ADDRESS);

            // Marque l'émetteur comme prêt à envoyer
            isRunning = true;

            log("✓ Sender initialized successfully");
            log("Ready to send messages to multicast group");
            log("----------------------------------------");

        } catch (IOException e) {
            // En cas d'erreur (socket ou résolution d'adresse)
            log("✗ Error initializing network: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to initialize network: " + e.getMessage(),
                    "Network Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                       name.endsWith(".png") || name.endsWith(".gif");
            }

            @Override
            public String getDescription() {
                return "Image Files (*.jpg, *.png, *.gif)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();

            /**
             * VALIDATION DE LA TAILLE DU FICHIER IMAGE (IMPORTANT)
             *
             * - Vérifie que le fichier ≤ 5 MB (MAX_IMAGE_SIZE)
             * - Empêche le chargement d'images trop grandes
             * - Note: la taille finale après sérialisation sera plus grande
             *   car MulticastMessage ajoute des métadonnées
             * - selectedImageFile.length() retourne la taille en octets
             */
            if (selectedImageFile.length() > MulticastConfig.MAX_IMAGE_SIZE) {
                JOptionPane.showMessageDialog(this,
                    "Image file is too large! Maximum size is 5 MB.",
                    "File Too Large",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Charge l'image en mémoire avec ImageIO
                // ImageIO.read() décode le fichier image en BufferedImage
                selectedImage = ImageIO.read(selectedImageFile);
                displayImagePreview(selectedImage);
                sendImageButton.setEnabled(true);
                log("✓ Image selected: " + selectedImageFile.getName() +
                    " (" + (selectedImageFile.length() / 1024) + " KB)");
            } catch (IOException e) {
                log("✗ Error loading image: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                    "Failed to load image: " + e.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void displayImagePreview(BufferedImage image) {
        // Scale image to fit preview area
        int maxWidth = 280;
        int maxHeight = 180;

        double scaleX = (double) maxWidth / image.getWidth();
        double scaleY = (double) maxHeight / image.getHeight();
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) (image.getWidth() * scale);
        int scaledHeight = (int) (image.getHeight() * scale);

        Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        imagePreviewLabel.setIcon(new ImageIcon(scaledImage));
        imagePreviewLabel.setText("");
    }

    /**
     * ENVOI D'UN MESSAGE TEXTE (MÉTHODE ESSENTIELLE)
     *
     * Cette méthode envoie un message texte via multicast UDP.
     * Elle encapsule tout le processus de préparation et d'envoi.
     *
     * ÉTAPES D'ENVOI:
     * 1. Récupérer et valider le texte de l'utilisateur
     * 2. Créer un objet MulticastMessage de type TEXT
     * 3. Sérialiser le message en bytes (toBytes)
     * 4. Vérifier que la taille respecte BUFFER_SIZE
     * 5. Créer un DatagramPacket avec les bytes + adresse + port
     * 6. Envoyer le paquet via le socket
     *
     * PROTOCOLE UDP:
     * - Pas de connexion préalable (connectionless)
     * - Pas d'accusé de réception (unreliable)
     * - Pas de garantie d'ordre
     * - Rapide et efficace pour le multicast
     *
     * MULTICAST:
     * - Le paquet est envoyé UNE SEULE FOIS à l'adresse du groupe
     * - Le routeur/switch duplique le paquet pour tous les membres du groupe
     * - Plus efficace que d'envoyer individuellement à chaque récepteur
     */
    private void sendTextMessage() {
        // Récupération et nettoyage du texte (enlève espaces début/fin)
        String message = messageArea.getText().trim();

        // Validation: empêche l'envoi de messages vides
        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a message to send!",
                    "Empty Message",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // === ÉTAPE 1: CRÉATION DU MESSAGE ===
            // Crée un objet MulticastMessage de type TEXT
            MulticastMessage msg = new MulticastMessage(message);

            // === ÉTAPE 2: SÉRIALISATION ===
            // Convertit l'objet en tableau de bytes pour transmission réseau
            // toBytes() utilise ObjectOutputStream pour la sérialisation Java
            byte[] buffer = msg.toBytes();

            // === ÉTAPE 3: VALIDATION DE LA TAILLE ===
            // Vérifie que le message sérialisé tient dans un paquet UDP
            // BUFFER_SIZE = 65000 bytes (proche du max UDP de 65535)
            // Si trop grand, le paquet serait fragmenté ou perdu
            if (buffer.length > MulticastConfig.BUFFER_SIZE) {
                JOptionPane.showMessageDialog(this,
                    "Message is too long!",
                    "Message Too Large",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // === ÉTAPE 4: CRÉATION DU PAQUET UDP ===
            /**
             * DatagramPacket = paquet UDP à envoyer
             *
             * Constructeur: DatagramPacket(byte[] buf, int length, InetAddress address, int port)
             * - buffer: les données à envoyer (message sérialisé)
             * - buffer.length: nombre de bytes à envoyer
             * - group: adresse IP de destination (230.0.0.1)
             * - PORT: port de destination (4446)
             *
             * Le paquet contient:
             * - En-tête UDP (source port, dest port, length, checksum)
             * - Données (le MulticastMessage sérialisé)
             */
            DatagramPacket packet = new DatagramPacket(
                    buffer,                    // Données à envoyer
                    buffer.length,             // Taille des données
                    group,                     // Adresse multicast de destination
                    MulticastConfig.PORT       // Port de destination
            );

            // === ÉTAPE 5: ENVOI DU PAQUET ===
            /**
             * socket.send(packet) envoie le paquet UDP
             *
             * - Pas de connexion établie (contrairement à TCP)
             * - Pas d'attente de confirmation
             * - Le paquet part immédiatement
             * - Peut être perdu en route (UDP ne garantit rien)
             * - Tous les récepteurs du groupe multicast le reçoivent
             */
            socket.send(packet);

            // === ÉTAPE 6: LOG ET NETTOYAGE ===
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            log("[" + timestamp + "] Sent text: " + message);
            messageArea.setText("");      // Efface le texte envoyé
            messageArea.requestFocus();   // Remet le focus pour un nouvel envoi

        } catch (IOException e) {
            // Erreurs possibles:
            // - Sérialisation échouée
            // - Socket fermé
            // - Problème réseau
            log("✗ Error sending message: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to send message: " + e.getMessage(),
                    "Send Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * ENVOI D'UNE IMAGE (MÉTHODE ESSENTIELLE)
     *
     * Cette méthode envoie une image via multicast UDP.
     * Le processus est similaire à sendTextMessage mais avec des étapes
     * supplémentaires pour convertir l'image en bytes.
     *
     * ÉTAPES D'ENVOI D'IMAGE:
     * 1. Vérifier qu'une image est sélectionnée
     * 2. Convertir BufferedImage en tableau de bytes (encodage JPEG/PNG/GIF)
     * 3. Créer un MulticastMessage de type IMAGE
     * 4. Sérialiser le message complet
     * 5. Vérifier la taille du paquet
     * 6. Créer et envoyer le DatagramPacket
     *
     * TRAITEMENT D'IMAGE:
     * - BufferedImage = représentation en mémoire de l'image
     * - ImageIO.write() encode l'image dans un format (jpg, png, gif)
     * - Le format détermine la compression et la taille finale
     * - JPG est plus compressé mais perd de la qualité
     * - PNG est sans perte mais plus lourd
     */
    private void sendImage() {
        // Validation: vérifie qu'une image est chargée
        if (selectedImage == null || selectedImageFile == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select an image first!",
                    "No Image Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // === ÉTAPE 1: CONVERSION DE L'IMAGE EN BYTES ===
            /**
             * ENCODAGE DE L'IMAGE:
             *
             * BufferedImage -> bytes nécessite un encodeur d'image
             * ImageIO.write() fait cette conversion
             *
             * Processus:
             * 1. Créer un ByteArrayOutputStream (buffer en mémoire)
             * 2. Déterminer le format (jpg, png, gif) depuis le nom du fichier
             * 3. ImageIO.write() encode l'image dans ce format
             * 4. Récupérer les bytes résultants
             *
             * Note: les bytes contiennent l'image ENCODÉE (pas les pixels bruts)
             * C'est comme le contenu d'un fichier .jpg ou .png
             */
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String format = getImageFormat(selectedImageFile.getName());

            // ImageIO.write(image, format, output) encode l'image
            // - selectedImage: l'image en mémoire
            // - format: "jpg", "png" ou "gif"
            // - baos: flux de sortie (accumule les bytes)
            ImageIO.write(selectedImage, format, baos);

            // Récupère les bytes de l'image encodée
            byte[] imageBytes = baos.toByteArray();

            // === ÉTAPE 2: CRÉATION DU MESSAGE MULTICAST ===
            /**
             * Crée un MulticastMessage de type IMAGE
             * - imageBytes: les bytes de l'image encodée
             * - format: le format pour que le récepteur puisse décoder
             *
             * Le constructeur initialise:
             * - type = IMAGE
             * - imageData = imageBytes
             * - imageFormat = format
             * - timestamp = moment actuel
             */
            MulticastMessage msg = new MulticastMessage(imageBytes, format);

            // === ÉTAPE 3: SÉRIALISATION DU MESSAGE ===
            /**
             * Convertit l'objet MulticastMessage complet en bytes
             *
             * Le buffer final contient:
             * - Métadonnées de sérialisation Java
             * - type (enum IMAGE)
             * - imageData (les bytes de l'image)
             * - imageFormat (String)
             * - timestamp (long)
             *
             * Taille finale > imageBytes.length à cause des métadonnées
             */
            byte[] buffer = msg.toBytes();

            // === ÉTAPE 4: VALIDATION DE LA TAILLE ===
            /**
             * VÉRIFICATION CRUCIALE:
             *
             * Le paquet UDP a une taille maximale (BUFFER_SIZE = 65000)
             * Si le message sérialisé dépasse cette taille:
             * - Le paquet sera fragmenté au niveau IP
             * - Risque élevé de perte (si un fragment est perdu, tout le paquet est perdu)
             * - Certains réseaux bloquent les paquets trop grands
             *
             * Solution si trop grand:
             * - Choisir une image plus petite
             * - Augmenter la compression (JPEG qualité plus basse)
             * - Redimensionner l'image avant l'envoi
             */
            if (buffer.length > MulticastConfig.BUFFER_SIZE) {
                JOptionPane.showMessageDialog(this,
                    "Image is too large to send! Try a smaller image or reduce quality.",
                    "Image Too Large",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // === ÉTAPE 5: CRÉATION ET ENVOI DU PAQUET UDP ===
            /**
             * Même processus que pour sendTextMessage():
             * - Crée un DatagramPacket avec les bytes, l'adresse et le port
             * - Envoie via socket.send()
             * - Le paquet multicast est routé à tous les membres du groupe
             */
            DatagramPacket packet = new DatagramPacket(
                    buffer,                    // Message sérialisé (incluant l'image)
                    buffer.length,             // Taille totale
                    group,                     // Adresse multicast (230.0.0.1)
                    MulticastConfig.PORT       // Port (4446)
            );

            // Envoi du paquet (non bloquant, pas d'attente de confirmation)
            socket.send(packet);

            // === ÉTAPE 6: LOG ===
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            log("[" + timestamp + "] Sent image: " + selectedImageFile.getName() +
                " (" + (buffer.length / 1024) + " KB)");

            // Note: affiche la taille du paquet final (sérialisé), pas juste l'image

        } catch (IOException e) {
            // Erreurs possibles:
            // - ImageIO.write() échoue (format non supporté)
            // - Sérialisation échoue
            // - Socket fermé ou erreur réseau
            log("✗ Error sending image: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to send image: " + e.getMessage(),
                    "Send Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * DÉTECTION DU FORMAT D'IMAGE
     *
     * Extrait le format d'image depuis l'extension du fichier.
     * Utilisé par ImageIO.write() pour encoder l'image correctement.
     *
     * @param filename Nom du fichier (ex: "photo.jpg")
     * @return Format pour ImageIO ("jpg", "png", ou "gif")
     */
    private String getImageFormat(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "png";
        if (lower.endsWith(".gif")) return "gif";
        return "jpg"; // Par défaut pour .jpg et .jpeg
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * NETTOYAGE ET FERMETURE (MÉTHODE IMPORTANTE)
     *
     * Cette méthode est appelée automatiquement quand la fenêtre se ferme.
     * Elle libère les ressources réseau proprement.
     *
     * IMPORTANCE DE LA FERMETURE:
     * - Les sockets sont des ressources système limitées
     * - Un socket non fermé peut rester ouvert indéfiniment
     * - Peut empêcher d'autres applications d'utiliser le même port
     * - Consomme de la mémoire inutilement
     *
     * PROCESSUS DE FERMETURE:
     * 1. Marquer isRunning = false (arrête les opérations en cours)
     * 2. Vérifier que le socket existe et n'est pas déjà fermé
     * 3. Fermer le socket avec close()
     * 4. Appeler super.dispose() pour nettoyer la fenêtre
     */
    @Override
    public void dispose() {
        // Marque l'émetteur comme inactif
        isRunning = false;

        // Ferme le socket s'il est ouvert
        // socket.close() libère le port et les ressources réseau
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        // Appelle la méthode dispose() de JFrame pour nettoyer la GUI
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            MulticastSender sender = new MulticastSender();
            sender.setVisible(true);
        });
    }
}

