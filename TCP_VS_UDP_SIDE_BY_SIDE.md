# COMPARAISON CÔTE À CÔTE: TCP vs UDP

## 📦 STRUCTURE DES PACKAGES

```
tcp/                          udp/
├── Message.java             ├── Message.java
├── Client.java              ├── Client.java
└── ServeurGUI.java          └── ServeurGUI.java
```

**Identique!** Mêmes noms de classes, même organisation.

---

## 🔍 CLASSE MESSAGE

### Structure (IDENTIQUE)

```java
// TCP et UDP: EXACTEMENT LE MÊME CODE!
package tcp;  // ou udp;

import java.io.Serializable;

public class Message implements Serializable {
    public enum Type { TEXTE, FICHIER, LISTE }

    public Type type;
    public String sender;
    public String target;
    public String text;
    public String filename;
    public byte[] fileBytes;

    // Constructeurs identiques
    public Message(Type type, String sender, String target, String text) { ... }
    public Message(Type type, String sender, String target, String filename, byte[] fileBytes) { ... }
    public Message() {}
}
```

**Différence:** AUCUNE - Le code est 100% identique!

---

## 💻 CLASSE CLIENT

### Déclaration des attributs réseau

```java
// ========== TCP ==========
private Socket socket;
private ObjectOutputStream out;
private ObjectInputStream in;
private Thread readerThread;
private volatile boolean connected = false;

// ========== UDP ==========
private DatagramSocket socket;
private InetAddress serverAddress;
private int serverPort;
private Thread readerThread;
private volatile boolean connected = false;
```

**Différence:** Type de socket et absence de flux (ObjectInputStream/OutputStream) en UDP.

---

### Méthode connect()

```java
// ========== TCP ==========
private void connect() {
    // ...validation...
    
    socket = new Socket(host, port);
    out = new ObjectOutputStream(socket.getOutputStream()); 
    out.flush();
    in = new ObjectInputStream(socket.getInputStream());

    Message hello = new Message(Message.Type.TEXTE, safePseudo(), "HELLO", "hello");
    out.writeObject(hello); 
    out.flush();

    connected = true;
    readerThread = new Thread(this::readLoop, "TCP-Reader");
    readerThread.start();
    // ...
}

// ========== UDP ==========
private void connect() {
    // ...validation...
    
    socket = new DatagramSocket();
    serverAddress = InetAddress.getByName(host);
    serverPort = port;

    Message hello = new Message(Message.Type.TEXTE, safePseudo(), "HELLO", "hello");
    sendMessage(hello);  // Sérialisation + envoi UDP

    connected = true;
    readerThread = new Thread(this::readLoop, "UDP-Reader");
    readerThread.start();
    // ...
}
```

**Différences:**
- TCP: Création de flux ObjectInputStream/OutputStream
- UDP: Stockage de l'adresse et du port du serveur
- TCP: Envoi direct avec `out.writeObject()`
- UDP: Envoi via méthode `sendMessage()` qui sérialise

---

### Méthode disconnect()

```java
// ========== TCP ==========
private void disconnect() {
    connected = false;
    try { if (in != null) in.close(); } catch (IOException ignore) {}
    try { if (out != null) out.close(); } catch (IOException ignore) {}
    try { if (socket != null) socket.close(); } catch (IOException ignore) {}
    socket = null; in = null; out = null;
    // ...thread cleanup...
}

// ========== UDP ==========
private void disconnect() {
    connected = false;
    if (socket != null && !socket.isClosed()) {
        socket.close();
    }
    socket = null;
    // ...thread cleanup...
}
```

**Différence:** TCP ferme les flux en plus du socket.

---

### Boucle de réception readLoop()

```java
// ========== TCP ==========
private void readLoop() {
    while (connected) {
        try {
            Object o = in.readObject();  // ← Lecture directe
            if (!(o instanceof Message msg)) continue;
            
            switch (msg.type) {
                case TEXTE -> appendText(...);
                case FICHIER -> { ... }
                case LISTE -> { ... }
            }
        } catch (EOFException eof) { ... }
        catch (Exception ex) { ... }
    }
    disconnect();
}

// ========== UDP ==========
private void readLoop() {
    byte[] buffer = new byte[BUFFER_SIZE];
    
    while (connected) {
        try {
            // Réception du paquet
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            
            // Désérialisation manuelle
            ByteArrayInputStream bis = new ByteArrayInputStream(
                packet.getData(), 0, packet.getLength()
            );
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object o = ois.readObject();
            
            if (!(o instanceof Message msg)) continue;
            
            switch (msg.type) {
                case TEXTE -> appendText(...);
                case FICHIER -> { ... }
                case LISTE -> { ... }
            }
        } catch (EOFException eof) { ... }
        catch (Exception ex) { ... }
    }
    disconnect();
}
```

**Différences:**
- TCP: `in.readObject()` (automatique)
- UDP: `socket.receive()` + désérialisation manuelle avec ByteArrayInputStream

---

### Méthode sendText()

```java
// ========== TCP ==========
private void sendText() {
    if (!connected) { warn("Connectez-vous d'abord."); return; }
    String text = inputField.getText().trim();
    if (text.isEmpty()) return;

    String target = (String) targetCombo.getSelectedItem();
    if (target == null || target.isBlank()) target = "Tous";
    
    Message msg = new Message(Message.Type.TEXTE, safePseudo(), target, text);
    try {
        out.writeObject(msg);  // ← Envoi direct
        out.flush();
        appendText("↗️ (" + target + ") " + text + "\n");
        inputField.setText("");
    } catch (IOException e) { warn("Erreur envoi : " + e.getMessage()); }
}

// ========== UDP ==========
private void sendText() {
    if (!connected) { warn("Connectez-vous d'abord."); return; }
    String text = inputField.getText().trim();
    if (text.isEmpty()) return;

    String target = (String) targetCombo.getSelectedItem();
    if (target == null || target.isBlank()) target = "Tous";
    
    Message msg = new Message(Message.Type.TEXTE, safePseudo(), target, text);
    try {
        sendMessage(msg);  // ← Sérialisation + envoi UDP
        appendText("↗️ (" + target + ") " + text + "\n");
        inputField.setText("");
    } catch (IOException e) { warn("Erreur envoi : " + e.getMessage()); }
}
```

**Différence:** 
- TCP: `out.writeObject(msg)` (automatique)
- UDP: `sendMessage(msg)` (méthode qui sérialise manuellement)

---

### Méthode sendMessage() (UDP uniquement)

```java
// ========== UDP SEULEMENT ==========
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
```

**Cette méthode n'existe pas en TCP** car l'envoi est géré par ObjectOutputStream directement.

---

### Interface graphique

```java
// TCP et UDP: EXACTEMENT LE MÊME CODE!

// Constructeur, buildUI(), appendText(), appendImage(), 
// setStatus(), updateButtons(), safePseudo(), info(), warn()
// → IDENTIQUES!
```

**Différence:** AUCUNE - L'interface est 100% identique!

---

## 🖥️ CLASSE SERVEUR

### Déclaration des attributs réseau

```java
// ========== TCP ==========
private ServerSocket serverSocket;
private volatile boolean running = false;
private Thread acceptThread;
private final Set<ClientHandler> clients = new CopyOnWriteArraySet<>();

// ========== UDP ==========
private DatagramSocket serverSocket;
private volatile boolean running = false;
private Thread acceptThread;
private final Set<ClientHandler> clients = new CopyOnWriteArraySet<>();
```

**Différence:** `ServerSocket` (TCP) vs `DatagramSocket` (UDP).

---

### Méthode startServer()

```java
// ========== TCP ==========
private void startServer() {
    // ...validation...
    serverSocket = new ServerSocket(port);
    running = true;
    acceptThread = new Thread(this::acceptLoop, "TCP-Acceptor");
    acceptThread.start();
    // ...
}

// ========== UDP ==========
private void startServer() {
    // ...validation...
    serverSocket = new DatagramSocket(port);
    running = true;
    acceptThread = new Thread(this::acceptLoop, "UDP-Listener");
    acceptThread.start();
    // ...
}
```

**Différence:** Type de socket créé.

---

### Boucle acceptLoop()

```java
// ========== TCP ==========
private void acceptLoop() {
    while (running) {
        try {
            Socket clientSocket = serverSocket.accept();  // ← Bloquant, attend connexion
            ClientHandler handler = new ClientHandler(clientSocket);
            clients.add(handler);
            new Thread(handler, "Client-" + clientSocket.getPort()).start();
        } catch (IOException e) { ... }
    }
}

// ========== UDP ==========
private void acceptLoop() {
    byte[] buffer = new byte[BUFFER_SIZE];
    
    while (running) {
        try {
            // Réception d'un paquet
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);  // ← Bloquant, attend paquet
            
            // Désérialisation
            ByteArrayInputStream bis = new ByteArrayInputStream(
                packet.getData(), 0, packet.getLength()
            );
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object o = ois.readObject();
            
            if (!(o instanceof Message msg)) continue;
            
            // Récupération adresse client
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();
            
            // Recherche ou création du handler
            ClientHandler handler = findOrCreateClient(msg.sender, clientAddress, clientPort);
            if (handler != null) {
                handler.handleMessage(msg);
            }
        } catch (IOException | ClassNotFoundException e) { ... }
    }
}
```

**Différences majeures:**
- TCP: `accept()` crée un nouveau Socket par client, lance un thread par client
- UDP: `receive()` reçoit tous les messages, un seul thread pour tous les clients
- UDP: Identification du client par adresse IP + port du paquet
- UDP: Recherche/création manuelle du ClientHandler

---

### Classe interne ClientHandler

```java
// ========== TCP ==========
private class ClientHandler implements Runnable {
    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String nickname = "?";

    ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream()); 
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Lecture du message HELLO
            Object hello = in.readObject();
            if (hello instanceof Message hm && ...) {
                nickname = hm.sender;
                // ...
            }

            // Boucle de lecture
            while (true) {
                Object o = in.readObject();
                if (!(o instanceof Message msg)) continue;
                // Traitement du message...
            }
        } catch (Exception ex) { ... }
        finally {
            close();
            clients.remove(this);
            // ...
        }
    }

    void send(Message msg) { 
        try { 
            out.writeObject(msg); 
            out.flush(); 
        } catch (IOException ignore) {} 
    }

    void close() {
        try { if (in != null) in.close(); } catch (IOException ignore) {}
        try { if (out != null) out.close(); } catch (IOException ignore) {}
        try { if (socket != null) socket.close(); } catch (IOException ignore) {}
    }
}

// ========== UDP ==========
private class ClientHandler {
    private final String nickname;
    private InetAddress address;
    private int port;

    ClientHandler(String nickname, InetAddress address, int port) { 
        this.nickname = nickname;
        this.address = address;
        this.port = port;
    }
    
    void updateAddress(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    void handleMessage(Message msg) {
        // Traitement selon le type (HELLO, TEXTE, FICHIER, LISTE)
        // ...
    }

    void send(Message msg) { 
        try {
            // Sérialisation
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(msg);
            oos.flush();
            byte[] data = bos.toByteArray();
            
            // Envoi du paquet UDP
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            serverSocket.send(packet);
        } catch (IOException e) { ... }
    }
}
```

**Différences majeures:**
- TCP: Implémente `Runnable`, a son propre thread, possède un Socket dédié
- UDP: Simple classe de données, pas de thread, stocke adresse IP + port
- TCP: `send()` utilise ObjectOutputStream
- UDP: `send()` sérialise manuellement et envoie via DatagramPacket
- TCP: Méthode `run()` avec boucle de lecture
- UDP: Méthode `handleMessage()` appelée depuis la boucle principale

---

### Interface graphique

```java
// TCP et UDP: EXACTEMENT LE MÊME CODE!

// Constructeur, startServer(), stopServer(), broadcastList(),
// sendToTarget(), refreshClientList(), append(), setStatus(), 
// updateButtons(), alert()
// → IDENTIQUES!
```

**Différence:** AUCUNE - L'interface est 100% identique!

---

## 📊 TABLEAU RÉCAPITULATIF

| Aspect | TCP | UDP |
|--------|-----|-----|
| **Classe Message** | Identique | Identique |
| **Socket Client** | Socket | DatagramSocket |
| **Connexion Client** | Socket(host, port) | DatagramSocket() |
| **Flux Client** | ObjectInputStream/OutputStream | - |
| **Envoi Client** | out.writeObject(msg) | Sérialisation manuelle + send(packet) |
| **Réception Client** | in.readObject() | receive(packet) + désérialisation manuelle |
| **Socket Serveur** | ServerSocket | DatagramSocket |
| **Accept Serveur** | accept() crée Socket | receive() reçoit paquet |
| **Thread par client** | Oui (TCP) | Non (UDP) |
| **ClientHandler** | Implémente Runnable | Simple classe données |
| **Stockage client** | Socket dédié | InetAddress + port |
| **Interface graphique** | Identique | Identique |
| **Fonctionnalités** | Identiques | Identiques |

---

## 🎯 CONCLUSION

### Points communs (90% du code)
- ✅ Structure des classes identique
- ✅ Classe Message 100% identique
- ✅ Interface graphique 100% identique
- ✅ Logique métier identique
- ✅ Gestion des événements identique

### Différences (10% du code)
- ❌ Type de socket (Socket/ServerSocket vs DatagramSocket)
- ❌ Sérialisation (automatique vs manuelle)
- ❌ Gestion des clients (thread par client vs adresse IP + port)
- ❌ Boucle d'écoute (acceptLoop différent)

### Résumé
**La seule vraie différence est le protocole réseau utilisé!** Le reste du code (interface, logique, Message) est identique. C'est parfait pour comprendre la différence entre TCP et UDP! 🎓

