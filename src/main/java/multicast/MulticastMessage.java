package multicast;

import java.io.*;

/**
 * Message wrapper for multicast communication
 * Supports both TEXT and IMAGE message types
 */
public class MulticastMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        TEXT,
        IMAGE
    }

    private MessageType type;
    private String textContent;
    private byte[] imageData;
    private String imageFormat; // jpg, png, etc.
    private String senderInfo;
    private long timestamp;

    // Constructor for text messages
    public MulticastMessage(String text) {
        this.type = MessageType.TEXT;
        this.textContent = text;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor for image messages
    public MulticastMessage(byte[] imageData, String imageFormat) {
        this.type = MessageType.IMAGE;
        this.imageData = imageData;
        this.imageFormat = imageFormat;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public MessageType getType() {
        return type;
    }

    public String getTextContent() {
        return textContent;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public String getSenderInfo() {
        return senderInfo;
    }

    public void setSenderInfo(String senderInfo) {
        this.senderInfo = senderInfo;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Serialize message to byte array
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }

    // Deserialize message from byte array
    public static MulticastMessage fromBytes(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (MulticastMessage) ois.readObject();
    }
}

