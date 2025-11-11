package udp;

import java.io.Serializable;

/**
 * CLASSE MESSAGE UDP
 *
 * Cette classe représente un message échangé entre le client et le serveur UDP.
 * Elle est sérialisable pour permettre sa transmission via le réseau.
 *
 * TYPES DE MESSAGES SUPPORTÉS:
 * - TEXTE   : Message textuel simple
 * - FICHIER : Transfert de fichier avec nom et contenu
 * - LISTE   : Liste des clients connectés
 *
 * UTILISATION UDP:
 * Cette classe est identique à la version TCP mais utilisée avec DatagramSocket.
 * Les objets sont sérialisés puis envoyés dans des DatagramPacket.
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

