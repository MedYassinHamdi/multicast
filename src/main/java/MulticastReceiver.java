import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Multicast Receiver with Swing GUI
 * Receives messages from the multicast group
 * Multiple instances can run simultaneously
 */
public class MulticastReceiver extends JFrame {
    private JTextArea messageArea;
    private JLabel statusLabel;
    private JLabel userIdLabel;
    private JButton startButton;
    private JButton stopButton;
    private MulticastSocket socket;
    private InetAddress group;
    private NetworkInterface networkInterface;
    private boolean isRunning = false;
    private Thread receiverThread;
    private String userId;
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

        JButton clearButton = new JButton("🗑️ Clear");
        clearButton.setFont(new Font("Arial", Font.BOLD, 14));
        clearButton.setBackground(new Color(105, 105, 105));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFocusPainted(false);
        clearButton.addActionListener(e -> {
            messageArea.setText("");
            messageCount = 0;
            updateStatus("Messages cleared");
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

        // Message Display Area
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel messageLabel = new JLabel("Received Messages:");
        messageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        centerPanel.add(messageLabel, BorderLayout.NORTH);

        messageArea = new JTextArea(20, 50);
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Courier New", Font.PLAIN, 13));
        messageArea.setBackground(new Color(240, 248, 255));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        setSize(800, 600);
        setLocationRelativeTo(null);

        // Offset window position for multiple instances
        Point location = getLocation();
        Random random = new Random();
        location.x += random.nextInt(100);
        location.y += random.nextInt(100);
        setLocation(location);
    }

    private void startReceiving() {
        if (isRunning) {
            return;
        }

        try {
            socket = new MulticastSocket(MulticastConfig.PORT);
            group = InetAddress.getByName(MulticastConfig.MULTICAST_ADDRESS);

            // Join the multicast group
            SocketAddress socketAddress = new InetSocketAddress(group, MulticastConfig.PORT);
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());

            if (networkInterface == null) {
                // Fallback to first available network interface
                networkInterface = NetworkInterface.getNetworkInterfaces().nextElement();
            }

            socket.joinGroup(socketAddress, networkInterface);

            isRunning = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            updateStatus("Connected - Listening for messages...");

            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            logMessage("=".repeat(60));
            logMessage("[" + timestamp + "] " + userId + " joined the multicast group");
            logMessage("=".repeat(60) + "\n");

            // Start receiver thread
            receiverThread = new Thread(this::receiveMessages);
            receiverThread.setDaemon(true);
            receiverThread.start();

        } catch (IOException e) {
            updateStatus("Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to start receiver: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            isRunning = false;
        }
    }

    private void stopReceiving() {
        if (!isRunning) {
            return;
        }

        isRunning = false;

        try {
            if (socket != null && group != null) {
                SocketAddress socketAddress = new InetSocketAddress(group, MulticastConfig.PORT);
                socket.leaveGroup(socketAddress, networkInterface);
            }
        } catch (IOException e) {
            logMessage("Error leaving group: " + e.getMessage());
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        updateStatus("Disconnected");

        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        logMessage("\n" + "=".repeat(60));
        logMessage("[" + timestamp + "] " + userId + " left the multicast group");
        logMessage("=".repeat(60) + "\n");
    }

    private void receiveMessages() {
        byte[] buffer = new byte[MulticastConfig.BUFFER_SIZE];

        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                String senderAddress = packet.getAddress().getHostAddress();
                String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

                messageCount++;

                SwingUtilities.invokeLater(() -> {
                    logMessage("┌─ Message #" + messageCount + " ─────────────────────────────");
                    logMessage("│ Time: " + timestamp);
                    logMessage("│ From: " + senderAddress);
                    logMessage("│ Content: " + message);
                    logMessage("└" + "─".repeat(50) + "\n");
                    updateStatus("Connected - Received " + messageCount + " message(s)");
                });

            } catch (IOException e) {
                if (isRunning) {
                    SwingUtilities.invokeLater(() ->
                        logMessage("Error receiving message: " + e.getMessage() + "\n")
                    );
                }
            }
        }
    }

    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(message + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
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

