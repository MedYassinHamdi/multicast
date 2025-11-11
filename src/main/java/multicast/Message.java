package multicast;

import java.io.Serializable;

/**
 * CLASSE MESSAGE MULTICAST
 *
 * Cette classe représente un message échangé via multicast IP.
 * Elle est sérialisable pour permettre sa transmission via le réseau.
 *
 * TYPES DE MESSAGES SUPPORTÉS:
 * - TEXTE   : Message textuel simple
 * - FICHIER : Transfert de fichier avec nom et contenu
 * - LISTE   : Liste des clients connectés (géré par le serveur relai)
 *
 * UTILISATION MULTICAST:
 * Cette classe est identique à TCP/UDP mais utilisée avec MulticastSocket.
 * Les objets sont sérialisés puis envoyés dans des DatagramPacket vers un groupe multicast.
 *
 * DIFFÉRENCE AVEC UDP/TCP:
 * - MULTICAST: Messages envoyés à un groupe (adresse IP de classe D: 224.0.0.0 à 239.255.255.255)
 * - UDP: Messages envoyés à une adresse IP spécifique (point à point)
 * - TCP: Connexion établie entre deux points précis
 */
public class Message implements Serializable {

    /**
     * Enumération des types de messages possibles
     */
    public enum Type { TEXTE, FICHIER, LISTE }

    // ========== ATTRIBUTS PUBLICS ==========

    /**
     * Type du message (TEXTE, FICHIER ou LISTE)
     */
    public Type type;

    /**
     * Pseudo de l'émetteur du message
     */
    public String sender;

    /**
     * Pseudo du destinataire ("Tous" pour broadcast)
     * Note: En multicast pur, tous reçoivent, mais on garde cette info pour cohérence
     */
    public String target;

    /**
     * Contenu textuel du message (pour type TEXTE ou LISTE)
     */
    public String text;

    /**
     * Nom du fichier (pour type FICHIER)
     */
    public String filename;

    /**
     * Contenu binaire du fichier (pour type FICHIER)
     */
    public byte[] fileBytes;

    // ========== CONSTRUCTEURS ==========

    /**
     * Constructeur pour messages TEXTE et LISTE
     *
     * @param type   Type du message (TEXTE ou LISTE)
     * @param sender Pseudo de l'émetteur
     * @param target Pseudo du destinataire
     * @param text   Contenu textuel
     */
    public Message(Type type, String sender, String target, String text) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.text = text;
    }

    /**
     * Constructeur pour messages FICHIER
     *
     * @param type      Type du message (FICHIER)
     * @param sender    Pseudo de l'émetteur
     * @param target    Pseudo du destinataire
     * @param filename  Nom du fichier
     * @param fileBytes Contenu binaire du fichier
     */
    public Message(Type type, String sender, String target, String filename, byte[] fileBytes) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.filename = filename;
        this.fileBytes = fileBytes;
    }

    /**
     * Constructeur par défaut (requis pour la sérialisation)
     */
    public Message() {}
}

