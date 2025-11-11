package multicast;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * RÉCEPTEUR MULTICAST (RECEIVER)
 *
 * Cette classe implémente le récepteur qui écoute et affiche les messages
 * (texte et images) envoyés par les émetteurs via multicast UDP.
 *
 * RÔLE DU RÉCEPTEUR:
 * - Rejoindre le groupe multicast (joinGroup)
 * - Écouter en permanence les paquets UDP sur le port configuré
 * - Recevoir et désérialiser les messages
 * - Afficher le contenu (texte ou image) dans l'interface
 * - Permettre plusieurs instances simultanées (chat de groupe)
 *
 * DIFFÉRENCES ÉMETTEUR VS RÉCEPTEUR:
 * - L'émetteur envoie simplement à l'adresse multicast (pas de joinGroup)
 * - Le récepteur DOIT rejoindre le groupe pour recevoir les paquets
 * - Le récepteur écoute en continu (boucle bloquante)
 * - Le récepteur utilise un thread séparé pour ne pas bloquer l'UI
 *
 * ARCHITECTURE RÉSEAU:
 * - MulticastSocket lié (bind) au port 4446
 * - Rejoint le groupe 230.0.0.1 sur une interface réseau spécifique
 * - Thread de réception en arrière-plan (daemon thread)
 * - Désérialisation des paquets reçus en MulticastMessage
 * - Mise à jour de l'UI via SwingUtilities.invokeLater (thread-safe)
 *
 * FONCTIONNEMENT:
 * 1. L'utilisateur clique "Start Listening"
 * 2. Le socket est créé et lié au port 4446
 * 3. joinGroup() indique au système de recevoir les paquets multicast
 * 4. Un thread démarre et boucle sur socket.receive()
 * 5. Chaque paquet reçu est désérialisé en MulticastMessage
 * 6. Le message est affiché dans l'interface selon son type
 */
public class MulticastReceiver extends JFrame {
    // ========== COMPOSANTS GUI (non essentiels pour l'examen) ==========
    private JPanel chatPanel;
    private JScrollPane chatScrollPane;
    private JLabel statusLabel;
    private JLabel userIdLabel;
    private JButton startButton;
    private JButton stopButton;

    // ========== COMPOSANTS RÉSEAU (ESSENTIELS) ==========

    /**
     * Socket multicast pour recevoir les paquets UDP
     *
     * CONFIGURATION SPÉCIALE POUR LA RÉCEPTION:
     * - Créé avec new MulticastSocket(PORT) pour lier au port 4446
     * - Le bind au port permet de recevoir les paquets destinés à ce port
     * - Contrairement à l'émetteur qui n'a pas besoin de bind
     */
    private MulticastSocket socket;

    /**
     * Adresse IP du groupe multicast (230.0.0.1)
     *
     * - Utilisée dans joinGroup() pour s'abonner au groupe
     * - Tous les paquets envoyés à cette adresse seront reçus
     * - Identique à l'adresse utilisée par l'émetteur
     */
    private InetAddress group;

    /**
     * Interface réseau utilisée pour le multicast
     *
     * IMPORTANCE:
     * - Un ordinateur peut avoir plusieurs interfaces réseau (Ethernet, WiFi, VPN...)
     * - Il faut spécifier QUELLE interface écoute le multicast
     * - NetworkInterface représente une carte réseau (ex: eth0, wlan0)
     * - Utilisé dans joinGroup(socketAddress, networkInterface)
     * - Nécessaire pour quitter le groupe avec leaveGroup()
     *
     * POURQUOI C'EST NÉCESSAIRE:
     * - Le multicast est lié à une interface physique/virtuelle spécifique
     * - Les paquets multicast ne traversent pas toutes les interfaces automatiquement
     * - Il faut dire au système: "écoute le multicast sur CETTE interface"
     */
    private NetworkInterface networkInterface;

    /**
     * Indicateur d'état du récepteur
     * - true = en train d'écouter les messages
     * - false = arrêté ou non démarré
     * - Contrôle la boucle de réception dans le thread
     */
    private boolean isRunning = false;

    /**
     * Thread de réception en arrière-plan
     *
     * POURQUOI UN THREAD SÉPARÉ:
     * - socket.receive() est BLOQUANT (attend un paquet indéfiniment)
     * - Si exécuté sur le thread UI, l'interface serait figée
     * - Le thread permet de recevoir en continu sans bloquer l'interface
     * - Daemon thread = se termine automatiquement quand l'app se ferme
     */
    private Thread receiverThread;

    /**
     * Identifiant unique du récepteur
     * - Généré aléatoirement (ex: "Receiver-1234")
     * - Permet de distinguer les différentes instances
     * - Utile pour le débogage et les logs
     */
    private final String userId;

    /**
     * Compteur de messages reçus
     * - Incrémenté à chaque message reçu
     * - Affiché dans l'interface pour suivre l'activité
     */
    private int messageCount = 0;

    public MulticastReceiver() {
        super("Multicast Receiver");
        userId = generateUserId();
        initializeUI();
    }

    private String generateUserId() {
        Random random = new Random();
        return "Receiver-" + (1000 + random.nextInt(9000));
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Title Panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(220, 20, 60));

        JLabel titleLabel = new JLabel("📥 Multicast Message Receiver", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        userIdLabel = new JLabel("User ID: " + userId);
        userIdLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userIdLabel.setForeground(Color.YELLOW);
        userIdLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        titlePanel.add(userIdLabel, BorderLayout.WEST);

        add(titlePanel, BorderLayout.NORTH);

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        startButton = new JButton("🔌 Start Listening");
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        startButton.setBackground(new Color(34, 139, 34));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> startReceiving());
        controlPanel.add(startButton);

        stopButton = new JButton("🛑 Stop Listening");
        stopButton.setFont(new Font("Arial", Font.BOLD, 14));
        stopButton.setBackground(new Color(178, 34, 34));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFocusPainted(false);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopReceiving());
        controlPanel.add(stopButton);

        JButton clearButton = new JButton("🗑️ Clear Chat");
        clearButton.setFont(new Font("Arial", Font.BOLD, 14));
        clearButton.setBackground(new Color(105, 105, 105));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFocusPainted(false);
        clearButton.addActionListener(e -> {
            chatPanel.removeAll();
            chatPanel.revalidate();
            chatPanel.repaint();
            messageCount = 0;
            updateStatus("Chat cleared");
        });
        controlPanel.add(clearButton);

        add(controlPanel, BorderLayout.SOUTH);

        // Status Panel
        JPanel statusPanel = new JPanel(new GridLayout(3, 1));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Connection Info"));
        statusPanel.add(new JLabel(" Address: " + MulticastConfig.MULTICAST_ADDRESS));
        statusPanel.add(new JLabel(" Port: " + MulticastConfig.PORT));

        statusLabel = new JLabel(" Status: Not Connected");
        statusLabel.setForeground(Color.RED);
        statusPanel.add(statusLabel);

        add(statusPanel, BorderLayout.WEST);

        // Chat Display Area
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel chatLabel = new JLabel("Chat - Messages & Images:");
        chatLabel.setFont(new Font("Arial", Font.BOLD, 14));
        centerPanel.add(chatLabel, BorderLayout.NORTH);

        // Chat panel to hold messages and images
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(Color.WHITE);

        chatScrollPane = new JScrollPane(chatPanel);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        centerPanel.add(chatScrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        setSize(900, 700);
        setLocationRelativeTo(null);

        // Offset window position for multiple instances
        Point location = getLocation();
        Random random = new Random();
        location.x += random.nextInt(100);
        location.y += random.nextInt(100);
        setLocation(location);
    }

    /**
     * DÉMARRAGE DE LA RÉCEPTION (MÉTHODE ESSENTIELLE)
     *
     * Cette méthode configure le socket multicast et démarre l'écoute des messages.
     * C'est la méthode la plus importante pour comprendre la réception multicast.
     *
     * ÉTAPES DE DÉMARRAGE:
     * 1. Créer et lier (bind) le socket au port multicast
     * 2. Résoudre l'adresse du groupe multicast
     * 3. Identifier l'interface réseau à utiliser
     * 4. Rejoindre le groupe multicast (joinGroup)
     * 5. Démarrer le thread de réception
     *
     * CONCEPTS CLÉS:
     * - BIND: attacher le socket au port 4446 pour recevoir les paquets
     * - JOIN GROUP: s'abonner au groupe multicast pour recevoir ses messages
     * - INTERFACE RÉSEAU: spécifier quelle carte réseau écoute le multicast
     *
     * DIFFÉRENCE AVEC L'ÉMETTEUR:
     * - L'émetteur ne bind pas et ne join pas
     * - Le récepteur DOIT bind au port ET join le groupe
     */
    private void startReceiving() {
        // Évite de démarrer plusieurs fois
        if (isRunning) {
            return;
        }

        try {
            // === ÉTAPE 1: CRÉATION ET BIND DU SOCKET ===
            /**
             * new MulticastSocket(PORT) fait deux choses:
             * 1. Crée un socket UDP capable de multicast
             * 2. Le LIE (bind) au port 4446
             *
             * Le bind est CRUCIAL:
             * - Indique au système d'exploitation: "envoie-moi tous les paquets UDP
             *   qui arrivent sur le port 4446"
             * - Sans bind, le socket ne recevrait rien
             * - Plusieurs sockets peuvent bind au même port multicast
             *   (contrairement à l'unicast où un seul socket peut bind un port)
             *
             * Note: si le port est déjà utilisé par un processus non-multicast,
             * une exception sera levée
             */
            socket = new MulticastSocket(MulticastConfig.PORT);

            // === ÉTAPE 2: RÉSOLUTION DE L'ADRESSE MULTICAST ===
            /**
             * Convertit "230.0.0.1" en objet InetAddress
             * - InetAddress.getByName() résout l'adresse IP
             * - Pour le multicast, c'est une simple conversion (pas de DNS)
             * - L'adresse doit être dans la plage multicast (224.0.0.0 - 239.255.255.255)
             */
            group = InetAddress.getByName(MulticastConfig.MULTICAST_ADDRESS);

            // === ÉTAPE 3: IDENTIFICATION DE L'INTERFACE RÉSEAU ===
            /**
             * JOINGROUP MODERNE REQUIERT UNE INTERFACE RÉSEAU
             *
             * Pourquoi une interface spécifique?
             * - Un ordinateur a souvent plusieurs interfaces (Ethernet, WiFi, VPN, loopback)
             * - Le multicast fonctionne au niveau de la couche liaison (couche 2)
             * - Il faut dire au système: "reçois le multicast sur CETTE interface"
             *
             * Méthodes pour obtenir l'interface:
             * 1. Via InetAddress.getLocalHost() - obtient l'adresse IP locale
             * 2. NetworkInterface.getByInetAddress() - trouve l'interface associée
             * 3. Fallback: première interface disponible si la méthode 1-2 échoue
             */
            SocketAddress socketAddress = new InetSocketAddress(group, MulticastConfig.PORT);

            // Tente d'obtenir l'interface réseau via l'adresse locale
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());

            // Fallback si networkInterface est null (possible sur certains systèmes)
            if (networkInterface == null) {
                /**
                 * getNetworkInterfaces() retourne toutes les interfaces
                 * nextElement() prend la première disponible
                 *
                 * Note: ce n'est pas idéal car la première interface peut être:
                 * - L'interface loopback (127.0.0.1) - ne fonctionnera pas pour le réseau
                 * - Une interface VPN inactive
                 * - Mais c'est un fallback raisonnable pour les tests
                 */
                networkInterface = NetworkInterface.getNetworkInterfaces().nextElement();
            }

            // === ÉTAPE 4: REJOINDRE LE GROUPE MULTICAST (ESSENTIEL) ===
            /**
             * socket.joinGroup(socketAddress, networkInterface)
             *
             * C'EST LA CLÉ DU MULTICAST:
             * - Indique au système d'exploitation: "je veux recevoir les paquets
             *   envoyés à l'adresse/port multicast sur cette interface"
             * - Le système configure la carte réseau pour accepter ces paquets
             * - Au niveau réseau, utilise le protocole IGMP (Internet Group Management Protocol)
             * - Le routeur/switch apprend que cette machine veut les paquets du groupe
             *
             * Que se passe-t-il au niveau réseau?
             * 1. Le système envoie un message IGMP "JOIN" au routeur
             * 2. Le routeur note: "cette machine veut les paquets du groupe 230.0.0.1"
             * 3. Quand un paquet arrive pour ce groupe, le routeur le duplique vers cette machine
             * 4. La carte réseau filtre et accepte les paquets avec cette adresse multicast
             *
             * NOUVELLE API (Java 7+):
             * - Ancienne API: joinGroup(InetAddress) - obsolète
             * - Nouvelle API: joinGroup(SocketAddress, NetworkInterface) - plus flexible
             */
            socket.joinGroup(socketAddress, networkInterface);

            // === ÉTAPE 5: ACTIVATION ET DÉMARRAGE DU THREAD ===
            isRunning = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            updateStatus("Connected - Listening for messages...");

            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            addSystemMessage(userId + " joined the multicast group at " + timestamp);

            /**
             * DÉMARRAGE DU THREAD DE RÉCEPTION
             *
             * - Crée un nouveau thread qui exécute la méthode receiveMessages()
             * - setDaemon(true): le thread se termine automatiquement quand l'app se ferme
             * - start(): lance le thread en arrière-plan
             *
             * Le thread va boucler indéfiniment sur socket.receive() pour recevoir
             * les paquets sans bloquer l'interface utilisateur
             */
            receiverThread = new Thread(this::receiveMessages);
            receiverThread.setDaemon(true);  // Daemon = se termine avec l'application
            receiverThread.start();          // Lance l'exécution du thread

        } catch (IOException e) {
            // Erreurs possibles:
            // - Port déjà utilisé (BindException)
            // - Adresse multicast invalide
            // - Interface réseau non trouvée
            // - Problème avec joinGroup (permissions, pas de multicast supporté...)
            updateStatus("Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to start receiver: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            isRunning = false;
        }
    }

    /**
     * ARRÊT DE LA RÉCEPTION (MÉTHODE IMPORTANTE)
     *
     * Cette méthode arrête proprement l'écoute multicast.
     * Elle quitte le groupe et libère les ressources.
     *
     * ÉTAPES D'ARRÊT:
     * 1. Marquer isRunning = false (arrête la boucle de réception)
     * 2. Quitter le groupe multicast (leaveGroup)
     * 3. Fermer le socket
     * 4. Mettre à jour l'interface
     *
     * IMPORTANCE DE LEAVEGR OUP:
     * - Indique au routeur: "je ne veux plus les paquets de ce groupe"
     * - Envoie un message IGMP LEAVE
     * - Libère les ressources réseau
     * - Sans leaveGroup, le routeur continue d'envoyer les paquets inutilement
     */
    private void stopReceiving() {
        // Évite d'arrêter si déjà arrêté
        if (!isRunning) {
            return;
        }

        // Marque le récepteur comme inactif
        // Cela va arrêter la boucle while(isRunning) dans receiveMessages()
        isRunning = false;

        try {
            /**
             * QUITTER LE GROUPE MULTICAST (ESSENTIEL)
             *
             * socket.leaveGroup(socketAddress, networkInterface)
             *
             * PROCESSUS DE DÉPART:
             * - Indique au système d'exploitation: "ne reçois plus les paquets de ce groupe"
             * - Le système envoie un message IGMP LEAVE au routeur
             * - Le routeur arrête d'envoyer les paquets multicast à cette machine
             * - La carte réseau est reconfigurée pour ignorer ces paquets
             *
             * Que se passe-t-il si on oublie leaveGroup()?
             * - Le socket fermé libère automatiquement le groupe (généralement)
             * - Mais c'est une bonne pratique de l'appeler explicitement
             * - Garantit un nettoyage propre
             * - Réduit la charge réseau immédiatement
             *
             * Note: même interface réseau que lors du joinGroup
             */
            if (socket != null && group != null) {
                SocketAddress socketAddress = new InetSocketAddress(group, MulticastConfig.PORT);
                socket.leaveGroup(socketAddress, networkInterface);
            }
        } catch (IOException e) {
            // Erreur possible si le socket est déjà fermé ou l'interface inaccessible
            addSystemMessage("Error leaving group: " + e.getMessage());
        }

        /**
         * FERMETURE DU SOCKET
         *
         * - Libère le port 4446
         * - Ferme toutes les connexions réseau associées
         * - Interrompt le receive() bloquant dans le thread
         *   (socket.receive() lève une SocketException quand le socket est fermé)
         */
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        // Mise à jour de l'interface
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        updateStatus("Disconnected");

        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        addSystemMessage(userId + " left the multicast group at " + timestamp);
    }

    /**
     * BOUCLE DE RÉCEPTION DES MESSAGES (MÉTHODE ESSENTIELLE)
     *
     * Cette méthode s'exécute dans un thread séparé et écoute en continu
     * les paquets multicast. C'est le cœur du récepteur.
     *
     * FONCTIONNEMENT:
     * - Boucle infinie tant que isRunning == true
     * - socket.receive() BLOQUE jusqu'à la réception d'un paquet
     * - Désérialise le paquet en MulticastMessage
     * - Met à jour l'interface selon le type de message
     *
     * CONCEPTS CLÉS:
     * - BLOCAGE: receive() attend un paquet (peut attendre indéfiniment)
     * - THREAD SÉPARÉ: évite de bloquer l'interface utilisateur
     * - DÉSÉRIALISATION: bytes -> objet MulticastMessage
     * - THREAD-SAFETY: utilise SwingUtilities.invokeLater pour l'UI
     *
     * FLUX DE DONNÉES:
     * Réseau -> DatagramPacket (bytes) -> MulticastMessage (objet) -> UI
     */
    private void receiveMessages() {
        /**
         * PRÉPARATION DU BUFFER DE RÉCEPTION
         *
         * - Alloue un tableau de bytes de taille BUFFER_SIZE (65000)
         * - Ce buffer stockera temporairement les données reçues
         * - Doit être assez grand pour contenir le plus gros message possible
         * - Si un paquet > buffer, il sera tronqué (attention!)
         */
        byte[] buffer = new byte[MulticastConfig.BUFFER_SIZE];

        /**
         * BOUCLE INFINIE DE RÉCEPTION
         *
         * Cette boucle s'exécute tant que isRunning == true
         * - Elle tourne en arrière-plan dans le thread receiverThread
         * - Quand stopReceiving() met isRunning = false, la boucle s'arrête
         * - Le thread se termine alors naturellement
         */
        while (isRunning) {
            try {
                // === ÉTAPE 1: CRÉATION DU PAQUET DE RÉCEPTION ===
                /**
                 * DatagramPacket pour RECEVOIR des données
                 *
                 * Constructeur: DatagramPacket(byte[] buf, int length)
                 * - buffer: tableau de bytes où stocker les données reçues
                 * - buffer.length: taille maximale à recevoir
                 *
                 * Le paquet sera rempli par receive() avec:
                 * - Les données reçues (dans buffer)
                 * - L'adresse IP de l'émetteur (packet.getAddress())
                 * - Le port de l'émetteur (packet.getPort())
                 * - La longueur réelle des données (packet.getLength())
                 */
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // === ÉTAPE 2: RÉCEPTION D'UN PAQUET (BLOQUANT) ===
                /**
                 * socket.receive(packet) - MÉTHODE BLOQUANTE CRUCIALE
                 *
                 * COMPORTEMENT:
                 * - BLOQUE le thread jusqu'à la réception d'un paquet UDP
                 * - Peut attendre indéfiniment s'il n'y a pas de trafic
                 * - Quand un paquet arrive sur le port 4446 pour le groupe multicast:
                 *   1. Le système d'exploitation le place dans le buffer du socket
                 *   2. receive() se "débloque" et remplit le DatagramPacket
                 *   3. Les données sont copiées dans le buffer
                 *   4. L'exécution continue à la ligne suivante
                 *
                 * INTERRUPTION:
                 * - Si socket.close() est appelé, receive() lève une SocketException
                 * - C'est comme ça qu'on arrête proprement la boucle
                 *
                 * MULTICAST:
                 * - Reçoit les paquets de TOUS les émetteurs du groupe
                 * - Pas de notion de "connexion" comme en TCP
                 * - Chaque paquet est indépendant
                 */
                socket.receive(packet);

                // === ÉTAPE 3: EXTRACTION DES DONNÉES REÇUES ===
                /**
                 * Copie uniquement les bytes valides du paquet
                 *
                 * - packet.getLength() retourne la taille réelle des données reçues
                 * - Peut être < buffer.length si le message est petit
                 * - System.arraycopy() copie uniquement la partie utile
                 * - Évite de traiter des données parasites du buffer
                 *
                 * Paramètres de System.arraycopy:
                 * - Source: packet.getData() (le buffer du paquet)
                 * - srcPos: 0 (début)
                 * - Destination: data (nouveau tableau)
                 * - destPos: 0 (début)
                 * - Length: packet.getLength() (taille exacte)
                 */
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

                // === ÉTAPE 4: DÉSÉRIALISATION DU MESSAGE ===
                /**
                 * Conversion bytes -> MulticastMessage
                 *
                 * - MulticastMessage.fromBytes(data) est une méthode statique
                 * - Utilise ObjectInputStream pour désérialiser
                 * - Reconstruit l'objet complet (type, contenu, timestamp, etc.)
                 * - Peut lever IOException ou ClassNotFoundException si les données
                 *   sont corrompues ou ne correspondent pas à un MulticastMessage
                 */
                MulticastMessage message = MulticastMessage.fromBytes(data);

                // === ÉTAPE 5: EXTRACTION DES MÉTADONNÉES ===
                /**
                 * Informations sur l'émetteur et le moment
                 *
                 * - packet.getAddress().getHostAddress(): adresse IP de l'émetteur
                 * - timestamp: heure de réception formatée (HH:mm:ss)
                 * - messageCount: compteur incrémenté pour chaque message
                 */
                String senderAddress = packet.getAddress().getHostAddress();
                String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

                messageCount++;

                // === ÉTAPE 6: MISE À JOUR DE L'INTERFACE (THREAD-SAFE) ===
                /**
                 * SwingUtilities.invokeLater() - IMPORTANT POUR SWING
                 *
                 * PROBLÈME:
                 * - Ce code s'exécute dans receiverThread (thread d'arrière-plan)
                 * - Swing n'est PAS thread-safe (l'UI doit être modifiée uniquement
                 *   depuis l'Event Dispatch Thread)
                 * - Modifier l'UI directement peut causer des bugs imprévisibles
                 *
                 * SOLUTION:
                 * - invokeLater() planifie le code à exécuter sur l'EDT
                 * - Le lambda sera exécuté plus tard par le thread UI
                 * - Garantit la thread-safety
                 *
                 * TRAITEMENT SELON LE TYPE:
                 * - Si TEXT: appelle addTextMessage() pour afficher le texte
                 * - Si IMAGE: appelle addImageMessage() pour décoder et afficher l'image
                 */
                SwingUtilities.invokeLater(() -> {
                    if (message.getType() == MulticastMessage.MessageType.TEXT) {
                        addTextMessage(timestamp, senderAddress, message.getTextContent());
                    } else if (message.getType() == MulticastMessage.MessageType.IMAGE) {
                        addImageMessage(timestamp, senderAddress, message.getImageData(), message.getImageFormat());
                    }
                    updateStatus("Connected - Received " + messageCount + " message(s)");
                });

            } catch (IOException | ClassNotFoundException e) {
                /**
                 * GESTION DES ERREURS
                 *
                 * Erreurs possibles:
                 * - IOException: socket fermé (normal lors de l'arrêt), problème réseau
                 * - ClassNotFoundException: désérialisation échouée (incompatibilité de version)
                 * - Données corrompues
                 *
                 * On affiche l'erreur SEULEMENT si isRunning == true
                 * (si false, c'est qu'on a arrêté volontairement, pas une vraie erreur)
                 */
                if (isRunning) {
                    SwingUtilities.invokeLater(() ->
                        addSystemMessage("Error receiving message: " + e.getMessage())
                    );
                }
            }
        }
        /**
         * FIN DE LA BOUCLE
         *
         * Quand isRunning devient false:
         * - La boucle while se termine
         * - Le thread receiverThread se termine
         * - Le récepteur est complètement arrêté
         */
    }

    private void addTextMessage(String timestamp, String sender, String text) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(new Color(230, 240, 255));
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createLineBorder(new Color(100, 149, 237), 2)
        ));
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header
        JLabel headerLabel = new JLabel("📝 Text Message #" + messageCount);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 13));
        headerLabel.setForeground(new Color(0, 100, 200));
        messagePanel.add(headerLabel);

        JLabel infoLabel = new JLabel("From: " + sender + " | Time: " + timestamp);
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        infoLabel.setForeground(Color.DARK_GRAY);
        messagePanel.add(infoLabel);

        messagePanel.add(Box.createVerticalStrut(5));

        // Message content
        JTextArea contentArea = new JTextArea(text);
        contentArea.setFont(new Font("Arial", Font.PLAIN, 14));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setBackground(new Color(230, 240, 255));
        contentArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        messagePanel.add(contentArea);

        chatPanel.add(messagePanel);
        chatPanel.add(Box.createVerticalStrut(10));
        chatPanel.revalidate();

        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    /**
     * AFFICHAGE D'UN MESSAGE IMAGE (MÉTHODE IMPORTANTE)
     *
     * Cette méthode décode et affiche une image reçue via multicast.
     * Elle convertit les bytes en BufferedImage pour l'affichage.
     *
     * PROCESSUS:
     * 1. Créer un flux depuis les bytes de l'image
     * 2. Décoder l'image avec ImageIO.read()
     * 3. Redimensionner si nécessaire pour l'affichage
     * 4. Créer un panneau GUI et afficher l'image
     *
     * DÉCODAGE D'IMAGE:
     * - imageData contient l'image ENCODÉE (JPEG, PNG, GIF...)
     * - ImageIO.read() décode et reconstruit la BufferedImage
     * - Le format est détecté automatiquement depuis les bytes
     */
    private void addImageMessage(String timestamp, String sender, byte[] imageData, String format) {
        try {
            // === DÉCODAGE DE L'IMAGE ===
            /**
             * CONVERSION bytes -> BufferedImage
             *
             * - ByteArrayInputStream crée un flux depuis le tableau de bytes
             * - ImageIO.read() lit le flux et décode l'image
             * - Supporte automatiquement JPEG, PNG, GIF, BMP...
             * - Retourne une BufferedImage (représentation en mémoire de l'image)
             *
             * BufferedImage contient:
             * - Les pixels de l'image (couleurs)
             * - Les dimensions (largeur, hauteur)
             * - Le type de couleur (RGB, ARGB, grayscale...)
             */
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bis);

            // === INTERFACE GRAPHIQUE (code GUI omis pour l'examen) ===
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            messagePanel.setBackground(new Color(255, 240, 245));
            messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createLineBorder(new Color(255, 105, 180), 2)
            ));
            messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Header
            JLabel headerLabel = new JLabel("🖼️ Image Message #" + messageCount);
            headerLabel.setFont(new Font("Arial", Font.BOLD, 13));
            headerLabel.setForeground(new Color(200, 0, 100));
            messagePanel.add(headerLabel);

            JLabel infoLabel = new JLabel("From: " + sender + " | Time: " + timestamp + " | Size: " + (imageData.length / 1024) + " KB");
            infoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            infoLabel.setForeground(Color.DARK_GRAY);
            messagePanel.add(infoLabel);

            messagePanel.add(Box.createVerticalStrut(5));

            // === REDIMENSIONNEMENT DE L'IMAGE ===
            /**
             * Scale image if too large
             *
             * - Calcule le ratio pour ajuster l'image à la zone d'affichage
             * - Préserve les proportions (aspect ratio)
             * - Ne grossit pas les petites images (scale ≤ 1.0)
             */
            int maxWidth = 600;
            int maxHeight = 400;

            double scaleX = (double) maxWidth / image.getWidth();
            double scaleY = (double) maxHeight / image.getHeight();
            double scale = Math.min(Math.min(scaleX, scaleY), 1.0); // Don't upscale

            int scaledWidth = (int) (image.getWidth() * scale);
            int scaledHeight = (int) (image.getHeight() * scale);

            Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
            imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            imageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Make image clickable to view full size
            imageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            imageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    showFullSizeImage(image);
                }
            });
            imageLabel.setToolTipText("Click to view full size");

            messagePanel.add(imageLabel);

            chatPanel.add(messagePanel);
            chatPanel.add(Box.createVerticalStrut(10));
            chatPanel.revalidate();

            // Scroll to bottom
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });

        } catch (IOException e) {
            addSystemMessage("Error displaying image: " + e.getMessage());
        }
    }

    private void showFullSizeImage(BufferedImage image) {
        JDialog dialog = new JDialog(this, "Full Size Image", false);
        dialog.setLayout(new BorderLayout());

        JLabel imageLabel = new JLabel(new ImageIcon(image));
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.setPreferredSize(new Dimension(
            Math.min(image.getWidth() + 50, 1200),
            Math.min(image.getHeight() + 50, 800)
        ));

        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addSystemMessage(String message) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(new Color(255, 250, 205));
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createLineBorder(new Color(218, 165, 32), 1)
        ));
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        JLabel label = new JLabel("⚠️ [" + timestamp + "] " + message);
        label.setFont(new Font("Arial", Font.ITALIC, 12));
        label.setForeground(new Color(139, 69, 19));
        messagePanel.add(label);

        chatPanel.add(messagePanel);
        chatPanel.add(Box.createVerticalStrut(5));
        chatPanel.revalidate();

        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(" Status: " + status);
            if (status.contains("Connected")) {
                statusLabel.setForeground(new Color(34, 139, 34));
            } else if (status.contains("Error")) {
                statusLabel.setForeground(Color.RED);
            } else {
                statusLabel.setForeground(Color.ORANGE);
            }
        });
    }

    @Override
    public void dispose() {
        stopReceiving();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            MulticastReceiver receiver = new MulticastReceiver();
            receiver.setVisible(true);
        });
    }
}

