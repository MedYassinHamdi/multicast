package tcp;

import java.io.Serializable;

public class Message implements Serializable {
    public enum Type { TEXTE, FICHIER, LISTE }

    public Type type;
    public String sender;
    public String target;
    public String text;
    public String filename;
    public byte[] fileBytes;

    // Texte / Liste
    public Message(Type type, String sender, String target, String text) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.text = text;
    }

    // Fichier
    public Message(Type type, String sender, String target, String filename, byte[] fileBytes) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.filename = filename;
        this.fileBytes = fileBytes;
    }

    public Message() {}
}
