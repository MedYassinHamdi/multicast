# Multicast Socket Java Application

## Overview
This is a simple demonstration of how **Java Multicast Sockets** work. The application consists of:
- **multicast.MulticastSender**: GUI application to send messages to all receivers in a multicast group
- **multicast.MulticastReceiver**: GUI application to receive multicast messages
- Multiple receivers can run simultaneously, each with a unique user ID

## What is Multicast?
Multicast is a network communication method where data is sent from one sender to multiple receivers simultaneously. It's more efficient than sending individual messages to each receiver (unicast) and uses special IP addresses in the range 224.0.0.0 to 239.255.255.255.

## Project Structure
```
MultiCast/
├── pom.xml
├── src/
│   └── main/
│       └── java/
│           ├── multicast.MulticastConfig.java    # Configuration constants
│           ├── multicast.MulticastSender.java    # Sender application with GUI
│           └── multicast.MulticastReceiver.java  # Receiver application with GUI
└── README.md
```

## Configuration
The multicast settings are defined in `multicast.MulticastConfig.java`:
- **Multicast Address**: 230.0.0.1 (can be any address in range 224.0.0.0-239.255.255.255)
- **Port**: 4446
- **Buffer Size**: 1024 bytes

## How to Run

### Compile the Project
```bash
mvn clean compile
```

### Run the Sender
```bash
mvn exec:java -Dexec.mainClass="multicast.MulticastSender"
```

Or using Java directly:
```bash
cd target/classes
java multicast.MulticastSender
```

### Run the Receiver (Multiple Times)
You can run this as many times as you want - each instance creates a new receiver with a unique ID:

```bash
mvn exec:java -Dexec.mainClass="multicast.MulticastReceiver"
```

Or using Java directly:
```bash
cd target/classes
java multicast.MulticastReceiver
```

**Tip**: Run multiple receiver instances to see multicast in action!

## How to Use

### Sender Application
1. Launch the multicast.MulticastSender application
2. Type your message in the text area
3. Click "Send to All Receivers" button (or press Ctrl+Enter)
4. The message will be broadcast to all active receivers
5. Check the activity log to see sent messages

### Receiver Application
1. Launch one or more multicast.MulticastReceiver instances
2. Each receiver gets a unique ID (e.g., Receiver-1234)
3. Click "🔌 Start Listening" to join the multicast group
4. The receiver will display all incoming messages with timestamps
5. Click "🛑 Stop Listening" to leave the multicast group
6. Click "🗑️ Clear" to clear the message history

## Features
✅ **Swing GUI** for both sender and receiver
✅ **Multiple Receivers** - Run as many receiver instances as you want
✅ **Unique User IDs** - Each receiver gets a random unique ID
✅ **Real-time messaging** - Messages appear instantly on all receivers
✅ **Timestamps** - All messages include time information
✅ **Message counter** - Track how many messages each receiver has received
✅ **Connection status** - Visual feedback on connection state
✅ **Activity logs** - See what's happening in real-time

## Network Concepts Demonstrated

### UDP vs Multicast
- **UDP** (User Datagram Protocol): One-to-one or one-to-specific communication
- **Multicast**: One-to-many communication using special IP addresses
- **TCP** (Transmission Control Protocol): Connection-oriented, reliable one-to-one

### Key Differences from TCP/UDP
1. **Multicast uses special IP addresses** (224.0.0.0 - 239.255.255.255)
2. **One message reaches multiple receivers** without sending multiple times
3. **Receivers "join" a multicast group** to receive messages
4. **More efficient** than sending individual messages to each receiver
5. **No acknowledgment** like UDP, but designed for group communication

## Troubleshooting

### Firewall Issues
If messages aren't being received, check your firewall settings:
- Allow Java applications through the firewall
- Allow UDP traffic on port 4446

### Network Interface Issues
If you get network interface errors, the application will try to use the first available network interface automatically.

### Multiple Receivers Not Receiving
Ensure all receiver instances are:
1. Running on the same computer or same local network
2. Using the same multicast address and port
3. Have clicked "Start Listening" button

## Learning Points
- How to create and use `MulticastSocket` in Java
- How to join and leave multicast groups
- Difference between unicast, multicast, and broadcast
- Building Swing GUI applications
- Thread management for network operations
- Real-time message display and logging

## Next Steps
You can extend this project by:
- Adding username input for senders
- Implementing a chat application
- Adding encryption for secure messages
- Creating a file transfer feature
- Adding message history persistence
- Implementing receiver discovery

## Requirements
- Java 21 or higher
- Maven 3.x
- Network access (for multicast communication)

## License
Educational project for learning multicast sockets.

---
**Note**: This is a demonstration project. For production use, consider error handling, security, and network optimization.

