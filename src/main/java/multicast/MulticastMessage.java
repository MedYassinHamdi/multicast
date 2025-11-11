package multicast;

import java.io.*;

/**
 * CLASSE MESSAGE MULTICAST
 *
 * Cette classe encapsule les données envoyées via multicast.
 * Elle supporte deux types de messages: TEXTE et IMAGE.
 *
 * SÉRIALISATION JAVA:
 * - Implémente Serializable pour convertir l'objet en bytes (et vice-versa)
 * - Permet de transmettre des objets Java complexes via le réseau
 * - serialVersionUID assure la compatibilité lors de la désérialisation
 *
 * PROTOCOLE DE COMMUNICATION:
 * 1. L'émetteur crée un MulticastMessage (texte ou image)
 * 2. Appelle toBytes() pour sérialiser l'objet en tableau de bytes
 * 3. Envoie les bytes via UDP multicast (DatagramPacket)
 * 4. Le récepteur reçoit les bytes via UDP
 * 5. Appelle fromBytes() pour désérialiser et reconstruire l'objet
 * 6. Utilise les getters pour accéder au contenu
 */
public class MulticastMessage implements Serializable {

    /**
     * SERIAL VERSION UID
     *
     * - Identifiant de version pour la sérialisation Java
     * - Si la classe change, modifier ce numéro évite les erreurs de désérialisation
     * - Garantit que l'émetteur et le récepteur utilisent la même structure de classe
     */
    private static final long serialVersionUID = 1L;

    /**
     * ENUMÉRATION DES TYPES DE MESSAGES
     *
     * - TEXT: pour les messages textuels simples
     * - IMAGE: pour les images (stockées comme tableau de bytes)
     * - Permet au récepteur de savoir comment traiter le message reçu
     */
    public enum MessageType {
        TEXT,    // Message textuel
        IMAGE    // Message contenant une image
    }

    // ========== ATTRIBUTS DE LA CLASSE ==========

    /**
     * Type du message (TEXT ou IMAGE)
     * Détermine quels autres champs sont utilisés
     */
    private MessageType type;

    /**
     * Contenu textuel (utilisé seulement si type == TEXT)
     * Null pour les messages de type IMAGE
     */
    private String textContent;

    /**
     * Données de l'image en bytes (utilisé seulement si type == IMAGE)
     * Contient l'image encodée (JPEG, PNG, GIF...)
     * Null pour les messages de type TEXT
     */
    private byte[] imageData;

    /**
     * Format de l'image (jpg, png, gif...)
     * Utilisé pour reconstruire l'image côté récepteur avec ImageIO
     * Null pour les messages de type TEXT
     */
    private String imageFormat;

    /**
     * Information sur l'émetteur (optionnel, pas utilisé actuellement)
     * Pourrait contenir un nom d'utilisateur, ID, etc.
     */
    private String senderInfo;

    /**
     * Timestamp de création du message
     * Enregistre le moment (en millisecondes depuis epoch Unix) où le message est créé
     * Utile pour ordonner les messages ou afficher l'heure d'envoi
     */
    private long timestamp;

    // ========== CONSTRUCTEURS ==========

    /**
     * CONSTRUCTEUR POUR MESSAGES TEXTE
     *
     * @param text Le contenu textuel à envoyer
     *
     * Initialise:
     * - type = TEXT
     * - textContent = le texte fourni
     * - timestamp = moment actuel
     * - imageData et imageFormat restent null
     */
    public MulticastMessage(String text) {
        this.type = MessageType.TEXT;
        this.textContent = text;
        this.timestamp = System.currentTimeMillis(); // Millisecondes depuis 01/01/1970
    }

    /**
     * CONSTRUCTEUR POUR MESSAGES IMAGE
     *
     * @param imageData Les bytes de l'image (contenu du fichier image)
     * @param imageFormat Le format de l'image ("jpg", "png", "gif"...)
     *
     * Initialise:
     * - type = IMAGE
     * - imageData = les bytes de l'image
     * - imageFormat = le format pour reconstruction
     * - timestamp = moment actuel
     * - textContent reste null
     */
    public MulticastMessage(byte[] imageData, String imageFormat) {
        this.type = MessageType.IMAGE;
        this.imageData = imageData;
        this.imageFormat = imageFormat;
        this.timestamp = System.currentTimeMillis();
    }

    // ========== GETTERS (ACCESSEURS) ==========

    /**
     * Retourne le type du message (TEXT ou IMAGE)
     * Permet au récepteur de savoir comment traiter le message
     */
    public MessageType getType() {
        return type;
    }

    /**
     * Retourne le contenu textuel
     * À utiliser seulement si getType() == TEXT
     */
    public String getTextContent() {
        return textContent;
    }

    /**
     * Retourne les données de l'image en bytes
     * À utiliser seulement si getType() == IMAGE
     */
    public byte[] getImageData() {
        return imageData;
    }

    /**
     * Retourne le format de l'image (jpg, png, gif...)
     * À utiliser seulement si getType() == IMAGE
     */
    public String getImageFormat() {
        return imageFormat;
    }

    /**
     * Retourne les informations sur l'émetteur
     * (Optionnel, peut être null)
     */
    public String getSenderInfo() {
        return senderInfo;
    }

    /**
     * Définit les informations sur l'émetteur
     */
    public void setSenderInfo(String senderInfo) {
        this.senderInfo = senderInfo;
    }

    /**
     * Retourne le timestamp de création
     * Format: millisecondes depuis epoch Unix (01/01/1970)
     */
    public long getTimestamp() {
        return timestamp;
    }

    // ========== MÉTHODES DE SÉRIALISATION ==========

    /**
     * SÉRIALISATION: Convertit l'objet MulticastMessage en tableau de bytes
     *
     * PROCESSUS:
     * 1. Crée un ByteArrayOutputStream (flux de bytes en mémoire)
     * 2. Crée un ObjectOutputStream pour écrire des objets Java
     * 3. Écrit l'objet 'this' (toutes ses données) dans le flux
     * 4. Retourne le tableau de bytes résultant
     *
     * Ces bytes peuvent ensuite être:
     * - Envoyés via le réseau dans un DatagramPacket
     * - Stockés dans un fichier
     * - Transmis par n'importe quel canal de communication binaire
     *
     * @return Tableau de bytes représentant cet objet
     * @throws IOException Si erreur d'écriture
     */
    public byte[] toBytes() throws IOException {
        // Flux de sortie en mémoire (accumule les bytes)
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // Flux pour écrire des objets Java dans le flux de bytes
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        // Écrit l'objet complet (tous les attributs) dans le flux
        oos.writeObject(this);

        // Force l'écriture des données bufferisées
        oos.flush();

        // Retourne le tableau de bytes final
        return bos.toByteArray();
    }

    /**
     * DÉSÉRIALISATION: Reconstruit un objet MulticastMessage depuis des bytes
     *
     * PROCESSUS:
     * 1. Crée un ByteArrayInputStream depuis le tableau de bytes reçu
     * 2. Crée un ObjectInputStream pour lire des objets Java
     * 3. Lit et reconstruit l'objet MulticastMessage
     * 4. Retourne l'objet reconstruit (avec tous ses attributs)
     *
     * IMPORTANT:
     * - Méthode statique (appelée sur la classe, pas sur une instance)
     * - Les bytes doivent provenir d'un MulticastMessage sérialisé
     * - Le serialVersionUID doit correspondre
     *
     * @param data Tableau de bytes à désérialiser
     * @return Objet MulticastMessage reconstruit
     * @throws IOException Si erreur de lecture
     * @throws ClassNotFoundException Si la classe MulticastMessage n'est pas trouvée
     */
    public static MulticastMessage fromBytes(byte[] data) throws IOException, ClassNotFoundException {
        // Flux d'entrée depuis le tableau de bytes
        ByteArrayInputStream bis = new ByteArrayInputStream(data);

        // Flux pour lire des objets Java depuis les bytes
        ObjectInputStream ois = new ObjectInputStream(bis);

        // Lit et reconstruit l'objet (cast vers MulticastMessage)
        // readObject() retourne un Object générique, d'où le cast
        return (MulticastMessage) ois.readObject();
    }
}

