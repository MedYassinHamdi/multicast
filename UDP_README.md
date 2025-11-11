# APPLICATION DE CHAT UDP

## 📋 TABLE DES MATIÈRES

1. [Description](#description)
2. [Architecture](#architecture)
3. [Fonctionnalités](#fonctionnalités)
4. [Installation et Compilation](#installation-et-compilation)
5. [Utilisation](#utilisation)
6. [Protocole de Communication](#protocole-de-communication)
7. [Structure du Code](#structure-du-code)
8. [Comparaison UDP vs TCP vs Multicast](#comparaison-udp-vs-tcp-vs-multicast)
9. [Limitations et Contraintes](#limitations-et-contraintes)
10. [Dépannage](#dépannage)

---

## 📖 DESCRIPTION

Application de chat en temps réel utilisant le **protocole UDP (User Datagram Protocol)** pour la communication réseau. L'application comprend un serveur centralisé et des clients avec interfaces graphiques modernes.

### Caractéristiques principales:
- ✅ Communication UDP point-à-point via serveur
- ✅ Envoi de messages texte (broadcast ou privé)
- ✅ Transfert d'images et fichiers
- ✅ Affichage inline des images
- ✅ Liste dynamique des clients connectés
- ✅ Interface graphique intuitive (Swing)
- ✅ Logs détaillés pour monitoring

---

## 🏗️ ARCHITECTURE

### Modèle Client-Serveur UDP

```
┌─────────────┐                    ┌─────────────┐
│   Client 1  │◄──── Message ─────►│   Serveur   │
│  (UDP GUI)  │     (UDP Packet)   │  (UDP GUI)  │
└─────────────┘                    └─────────────┘
                                          ▲
                                          │
┌─────────────┐                           │
│   Client 2  │◄──────────────────────────┘
│  (UDP GUI)  │     Retransmission
└─────────────┘
```

### Composants:

1. **UDPServerGUI**: Serveur centralisé
   - Écoute sur un port UDP unique
   - Gère une table de clients connectés
   - Route les messages entre clients
   - Fournit la liste des clients

2. **UDPClientGUI**: Client graphique
   - Se connecte au serveur
   - Envoie messages texte et fichiers
   - Reçoit messages des autres clients
   - Affiche images inline

3. **Message**: Classe de données
   - Encapsule tous les types de messages
   - Sérialisation Java (Serializable)
   - Types: TEXTE, FICHIER, LISTE

---

## ⚙️ FONCTIONNALITÉS

### Côté Serveur (UDPServerGUI)

- 🚀 **Démarrage/Arrêt** du serveur sur port configurable
- 👥 **Gestion multi-clients** avec table ConcurrentHashMap
- 📡 **Routage de messages**:
  - Broadcast: vers tous les clients sauf l'émetteur
  - Unicast: vers un client spécifique
- 📋 **Liste des clients**: Envoi sur demande
- 📊 **Monitoring**: Logs détaillés et compteur de clients
- 🔒 **Thread-safe**: Gestion concurrente sécurisée

### Côté Client (UDPClientGUI)

- 🔌 **Connexion** au serveur avec pseudo personnalisé
- 💬 **Chat texte**:
  - Messages broadcast ("Tous")
  - Messages privés (client spécifique)
- 📎 **Transfert de fichiers**:
  - Images (PNG, JPG, GIF...) affichées inline
  - Autres fichiers (PDF, TXT...)
- 📋 **Liste dynamique** des destinataires
- 🔄 **Rafraîchissement** de la liste des clients
- 📊 **Logs techniques** détaillés

---

## 💾 INSTALLATION ET COMPILATION

### Prérequis

- **Java JDK 11** ou supérieur
- **Maven** (optionnel, pour build automatisé)

### Compilation avec Maven

```cmd
cd "D:\Systeme communicant\MultiCast"
mvn clean compile
```

### Compilation manuelle (sans Maven)

```cmd
cd "D:\Systeme communicant\MultiCast\src\main\java"
javac udp\*.java
```

---

## 🚀 UTILISATION

### Méthode 1: Fichiers .bat (Windows)

1. **Lancer le serveur**:
   ```cmd
   run-udp-server.bat
   ```

2. **Lancer un ou plusieurs clients**:
   ```cmd
   run-udp-client.bat
   ```

### Méthode 2: Ligne de commande

1. **Serveur**:
   ```cmd
   java -cp "target\classes" udp.UDPServerGUI
   ```

2. **Client**:
   ```cmd
   java -cp "target\classes" udp.UDPClientGUI
   ```

### Configuration

#### Serveur:
- **Port**: Choisir un port entre 1024 et 65535 (défaut: 6000)
- Cliquer sur **"Démarrer"**

#### Client:
- **Pseudo**: Votre nom d'utilisateur (obligatoire)
- **Serveur**: Adresse IP du serveur (défaut: localhost)
- **Port**: Port du serveur (doit correspondre)
- Cliquer sur **"Connecter"**

### Test en local

1. Lancer 1 serveur
2. Lancer 2+ clients
3. Se connecter avec des pseudos différents
4. Tester les messages broadcast et privés
5. Tester l'envoi d'images

---

## 🔌 PROTOCOLE DE COMMUNICATION

### Types de Messages

| Type     | Description                           | Contenu                                    |
|----------|---------------------------------------|--------------------------------------------|
| TEXTE    | Message de chat                       | sender, target, text                       |
| FICHIER  | Transfert de fichier                  | sender, target, filename, fileBytes        |
| LISTE    | Demande/Réponse liste des clients     | sender, text (liste des pseudos)           |

### Flux de Communication

#### Envoi d'un message texte:

```
1. Client crée: Message(TEXTE, "Alice", "Bob", "Salut!")
2. Client sérialise: byte[] data = message.toBytes()
3. Client envoie: DatagramPacket → Serveur
4. Serveur désérialise: Message msg = Message.fromBytes(data)
5. Serveur identifie destinataire: "Bob"
6. Serveur retransmet: DatagramPacket → Client Bob
7. Client Bob désérialise et affiche: "Alice → Bob : Salut!"
```

#### Broadcast:

```
1. Client envoie: Message(TEXTE, "Alice", "Tous", "Hello!")
2. Serveur reçoit et identifie broadcast
3. Serveur boucle sur tous les clients (sauf Alice)
4. Serveur envoie à: Bob, Charlie, David...
5. Tous reçoivent: "Alice : Hello!"
```

#### Demande de liste:

```
1. Client envoie: Message(LISTE, "Alice", null, null)
2. Serveur construit liste: "Alice\nBob\nCharlie"
3. Serveur répond: Message(LISTE, "Serveur", "Alice", liste)
4. Client Alice met à jour sa liste déroulante
```

---

## 📁 STRUCTURE DU CODE

### Package: `udp`

```
udp/
├── Message.java          (250 lignes, commentaires détaillés)
├── UDPClientGUI.java     (1050 lignes, commentaires détaillés)
└── UDPServerGUI.java     (650 lignes, commentaires détaillés)
```

### Classe Message

**Responsabilité**: Encapsulation des données échangées

**Attributs principaux**:
- `Type type` - TEXTE, FICHIER, LISTE
- `String sender` - Pseudo émetteur
- `String target` - Pseudo destinataire
- `String text` - Contenu textuel
- `String filename` - Nom du fichier
- `byte[] fileBytes` - Données du fichier

**Méthodes clés**:
- `byte[] toBytes()` - Sérialisation
- `static Message fromBytes(byte[])` - Désérialisation

### Classe UDPClientGUI

**Responsabilité**: Interface client et communication réseau

**Composants réseau**:
- `DatagramSocket socket` - Socket UDP
- `InetAddress serverAddress` - Adresse du serveur
- `Thread listenerThread` - Écoute des messages

**Méthodes principales**:
- `connect()` - Connexion au serveur
- `disconnect()` - Déconnexion
- `sendText()` - Envoi message texte
- `sendFile()` - Envoi fichier
- `listenLoop()` - Boucle de réception

### Classe UDPServerGUI

**Responsabilité**: Routage des messages et gestion des clients

**Composants réseau**:
- `DatagramSocket socket` - Socket UDP serveur
- `ConcurrentHashMap<String, ClientInfo> clients` - Table des clients
- `Thread serverThread` - Écoute des messages

**Méthodes principales**:
- `startServer()` - Démarrage du serveur
- `stopServer()` - Arrêt du serveur
- `serverLoop()` - Boucle de réception
- `registerClient()` - Enregistrement client
- `broadcastMessage()` - Diffusion broadcast
- `unicastMessage()` - Envoi unicast

---

## ⚖️ COMPARAISON UDP vs TCP vs MULTICAST

| Critère              | UDP Point-à-Point       | TCP Point-à-Point      | Multicast              |
|----------------------|-------------------------|------------------------|------------------------|
| **Connexion**        | Sans connexion          | Avec connexion         | Sans connexion         |
| **Fiabilité**        | Non garanti             | Garanti (ACK, retrans) | Non garanti            |
| **Ordre**            | Non garanti             | Garanti                | Non garanti            |
| **Vitesse**          | Rapide                  | Moyenne                | Très rapide            |
| **Overhead**         | Faible (8 bytes header) | Élevé (20+ bytes)      | Faible                 |
| **Routage**          | Serveur centralisé      | Point-à-point          | Diffusion de groupe    |
| **Architecture**     | Client-Serveur          | Client-Serveur ou P2P  | Groupe distribué       |
| **Scalabilité**      | Moyenne                 | Faible (1 socket/cli)  | Excellente             |
| **Complexité serveur**| Moyenne                | Élevée (threads/client)| Faible (relai optionnel)|
| **Cas d'usage**      | Chat temps réel         | Transferts fiables     | Streaming, broadcast   |

### Quand utiliser UDP?
- ✅ Chat en temps réel (perte de message acceptable)
- ✅ Jeux vidéo (latence critique)
- ✅ Streaming audio/vidéo (perte de paquet tolérée)
- ✅ Communication légère et rapide

### Quand utiliser TCP?
- ✅ Transfert de fichiers importants
- ✅ Transactions bancaires
- ✅ APIs REST
- ✅ Toute communication où la fiabilité est critique

### Quand utiliser Multicast?
- ✅ Diffusion à grande échelle
- ✅ Streaming en direct (IPTV)
- ✅ Mise à jour de données en temps réel
- ✅ Applications distribuées

---

## ⚠️ LIMITATIONS ET CONTRAINTES

### Limitations UDP

1. **Taille des paquets**:
   - Maximum théorique: 65 535 bytes (64 Ko)
   - Maximum pratique: 8 192 - 32 768 bytes (8-32 Ko)
   - Au-delà: fragmentation IP → risque de perte

2. **Perte de paquets**:
   - UDP ne garantit pas la livraison
   - En réseau local: perte rare (<0.1%)
   - En réseau distant: perte plus fréquente

3. **Ordre des messages**:
   - Les paquets peuvent arriver dans le désordre
   - Implémentation actuelle: pas de numérotation

### Limitations de l'implémentation

1. **Pas de timeout client**:
   - Les clients déconnectés restent dans la liste
   - Solution: implémenter un heartbeat

2. **Pas de chiffrement**:
   - Messages en clair sur le réseau
   - Solution: implémenter TLS/DTLS

3. **Pas de compression**:
   - Fichiers volumineux non compressés
   - Solution: ajouter compression (GZIP)

4. **Pas de fragmentation applicative**:
   - Fichiers limités à ~32 Ko
   - Solution: découper en plusieurs paquets

---

## 🔧 DÉPANNAGE

### Problème: "Port already in use"

**Cause**: Un autre programme utilise le port 6000

**Solution**:
```
1. Changer le port dans l'interface (ex: 6001)
2. Ou arrêter l'autre programme:
   netstat -ano | findstr :6000
   taskkill /PID <PID> /F
```

### Problème: "Serveur introuvable"

**Cause**: Mauvaise adresse IP ou serveur arrêté

**Solution**:
```
1. Vérifier que le serveur est démarré
2. Vérifier l'adresse IP:
   - Serveur local: "localhost" ou "127.0.0.1"
   - Serveur distant: obtenir l'IP avec "ipconfig"
3. Vérifier le firewall Windows
```

### Problème: Messages non reçus

**Cause**: Firewall bloque UDP

**Solution**:
```
1. Ouvrir Windows Defender Firewall
2. "Autoriser une application"
3. Ajouter Java (javaw.exe)
4. Autoriser réseau privé et public
```

### Problème: Images non affichées

**Cause**: Format non supporté ou fichier corrompu

**Solution**:
```
1. Utiliser formats standards: PNG, JPG, GIF
2. Vérifier taille < 32 Ko
3. Consulter les logs pour erreurs
```

### Problème: ClassNotFoundException

**Cause**: Projet non compilé ou mauvais classpath

**Solution**:
```cmd
mvn clean compile
```

---

## 📚 RÉFÉRENCES

### Documentation Java
- [DatagramSocket](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/DatagramSocket.html)
- [DatagramPacket](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/DatagramPacket.html)
- [Serializable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/Serializable.html)

### Protocole UDP
- [RFC 768 - User Datagram Protocol](https://tools.ietf.org/html/rfc768)
- [UDP sur Wikipedia](https://fr.wikipedia.org/wiki/User_Datagram_Protocol)

---

## 👥 AUTEUR ET LICENCE

**Projet**: Application de Chat UDP  
**Contexte**: Systèmes Communicants  
**Date**: Novembre 2025  
**Licence**: Usage éducatif

---

## 📝 NOTES TECHNIQUES

### Sérialisation Java

L'application utilise `ObjectOutputStream` et `ObjectInputStream` pour la sérialisation:

**Avantages**:
- Simple à implémenter
- Préserve les types Java
- Gère les graphes d'objets

**Inconvénients**:
- Binaire propriétaire (Java uniquement)
- Taille des données élevée
- Pas compatible avec autres langages

**Alternative**: JSON avec Gson/Jackson (plus universel)

### Thread Safety

Le serveur utilise `ConcurrentHashMap` pour la table des clients:
- Pas de verrou global
- Opérations atomiques (put, get, remove)
- Itération thread-safe avec keySet()

### Gestion des erreurs

L'application log toutes les exceptions:
- `IOException`: Erreurs réseau (socket fermé, timeout)
- `ClassNotFoundException`: Classe Message introuvable
- `SocketException`: Socket fermé pendant receive()

---

**🎉 Bon développement avec UDP!**

