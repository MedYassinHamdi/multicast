# Quick Start Guide - Multicast Socket Demo

## 🚀 How to Run

### Option 1: Using Batch Files (Easiest)
1. **Run the Sender**: Double-click `run-sender.bat`
2. **Run Receivers**: Double-click `run-receiver.bat` multiple times (each click creates a new receiver)

### Option 2: Using Maven Commands
```cmd
# Compile first (only once)
mvn clean compile

# Run Sender
mvn exec:java -Dexec.mainClass="multicast.MulticastSender"

# Run Receiver (in separate terminals, run multiple times)
mvn exec:java -Dexec.mainClass="multicast.MulticastReceiver"
```

### Option 3: Using Java Directly
```cmd
# Compile first
mvn clean compile

# Run Sender
cd target\classes
java multicast.MulticastSender

# Run Receiver (in separate terminals)
cd target\classes
java multicast.MulticastReceiver
```

## 📝 Test Scenario

1. **Start 1 Sender**: Run `run-sender.bat`
2. **Start Multiple Receivers**: Run `run-receiver.bat` 3-4 times to create multiple receivers
3. **In each Receiver window**: Click "🔌 Start Listening" button
4. **In the Sender window**: 
   - Type a message like "Hello everyone!"
   - Click "Send to All Receivers"
5. **Watch**: All receiver windows will display the same message simultaneously!

## 🎯 What You'll See

### Sender Window
- Green "Send to All Receivers" button
- Activity log showing sent messages with timestamps
- Multicast address and port information

### Receiver Windows
- Each has a unique ID (e.g., Receiver-1234, Receiver-5678)
- Connection status indicator (red/green)
- Message counter
- Received messages with timestamps and sender IP
- Start/Stop/Clear buttons

## 🔍 Understanding Multicast

**Key Points:**
- One sender sends to MANY receivers at once
- More efficient than sending individual messages
- Uses special IP address (230.0.0.1)
- All receivers in the group get the same message
- Unlike TCP, there's no individual connection to each receiver
- Unlike UDP unicast, you don't need to know each receiver's address

**Compare to:**
- **TCP**: One-to-one connection (like a phone call)
- **UDP**: One-to-one messaging (like sending individual text messages)
- **Multicast**: One-to-many (like a radio broadcast - everyone tuned in receives it)

## 💡 Experiment Ideas

1. **Start 1 receiver, send messages, then start more receivers** - New receivers only get messages sent after they join
2. **Stop a receiver, send messages, restart it** - Shows join/leave behavior
3. **Send messages with no receivers listening** - Messages are broadcast but not received (no error)
4. **Run on different computers on the same network** - Multicast works across network!

## ⚠️ Troubleshooting

**No messages received?**
- Make sure receivers clicked "Start Listening"
- Check firewall settings (allow Java through firewall)
- Ensure all apps are on same network

**Can't compile?**
- Make sure Maven is installed: `mvn --version`
- Make sure Java 21+ is installed: `java --version`

## 📚 Code Structure

```
multicast.MulticastConfig.java    → Settings (address: 230.0.0.1, port: 4446)
multicast.MulticastSender.java    → Sends messages using MulticastSocket
multicast.MulticastReceiver.java  → Receives messages using MulticastSocket + joinGroup()
```

**Key Java Classes Used:**
- `java.net.MulticastSocket` - Special socket for multicast
- `java.net.DatagramPacket` - Packet for UDP/multicast data
- `java.net.InetAddress` - IP address representation
- `javax.swing.*` - GUI components

Enjoy learning about multicast sockets! 🎉

