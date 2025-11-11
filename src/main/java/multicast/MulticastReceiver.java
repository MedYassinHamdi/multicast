package multicast;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
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
    private JPanel chatPanel;
    private JScrollPane chatScrollPane;
    private JLabel statusLabel;
    private JLabel userIdLabel;
    private JButton startButton;
    private JButton stopButton;
    private MulticastSocket socket;
    private InetAddress group;
    private NetworkInterface networkInterface;
    private boolean isRunning = false;
    private Thread receiverThread;
    private final String userId;
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

        JButton clearButton = new JButton("🗑️ Clear Chat");
        clearButton.setFont(new Font("Arial", Font.BOLD, 14));
        clearButton.setBackground(new Color(105, 105, 105));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFocusPainted(false);
        clearButton.addActionListener(e -> {
            chatPanel.removeAll();
            chatPanel.revalidate();
            chatPanel.repaint();
            messageCount = 0;
            updateStatus("Chat cleared");
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

        // Chat Display Area
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel chatLabel = new JLabel("Chat - Messages & Images:");
        chatLabel.setFont(new Font("Arial", Font.BOLD, 14));
        centerPanel.add(chatLabel, BorderLayout.NORTH);

        // Chat panel to hold messages and images
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(Color.WHITE);

        chatScrollPane = new JScrollPane(chatPanel);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        centerPanel.add(chatScrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        setSize(900, 700);
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
            addSystemMessage(userId + " joined the multicast group at " + timestamp);

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
            addSystemMessage("Error leaving group: " + e.getMessage());
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        updateStatus("Disconnected");

        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        addSystemMessage(userId + " left the multicast group at " + timestamp);
    }

    private void receiveMessages() {
        byte[] buffer = new byte[MulticastConfig.BUFFER_SIZE];

        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

                MulticastMessage message = MulticastMessage.fromBytes(data);
                String senderAddress = packet.getAddress().getHostAddress();
                String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

                messageCount++;

                SwingUtilities.invokeLater(() -> {
                    if (message.getType() == MulticastMessage.MessageType.TEXT) {
                        addTextMessage(timestamp, senderAddress, message.getTextContent());
                    } else if (message.getType() == MulticastMessage.MessageType.IMAGE) {
                        addImageMessage(timestamp, senderAddress, message.getImageData(), message.getImageFormat());
                    }
                    updateStatus("Connected - Received " + messageCount + " message(s)");
                });

            } catch (IOException | ClassNotFoundException e) {
                if (isRunning) {
                    SwingUtilities.invokeLater(() ->
                        addSystemMessage("Error receiving message: " + e.getMessage())
                    );
                }
            }
        }
    }

    private void addTextMessage(String timestamp, String sender, String text) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(new Color(230, 240, 255));
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createLineBorder(new Color(100, 149, 237), 2)
        ));
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header
        JLabel headerLabel = new JLabel("📝 Text Message #" + messageCount);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 13));
        headerLabel.setForeground(new Color(0, 100, 200));
        messagePanel.add(headerLabel);

        JLabel infoLabel = new JLabel("From: " + sender + " | Time: " + timestamp);
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        infoLabel.setForeground(Color.DARK_GRAY);
        messagePanel.add(infoLabel);

        messagePanel.add(Box.createVerticalStrut(5));

        // Message content
        JTextArea contentArea = new JTextArea(text);
        contentArea.setFont(new Font("Arial", Font.PLAIN, 14));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setBackground(new Color(230, 240, 255));
        contentArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        messagePanel.add(contentArea);

        chatPanel.add(messagePanel);
        chatPanel.add(Box.createVerticalStrut(10));
        chatPanel.revalidate();

        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void addImageMessage(String timestamp, String sender, byte[] imageData, String format) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bis);

            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            messagePanel.setBackground(new Color(255, 240, 245));
            messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createLineBorder(new Color(255, 105, 180), 2)
            ));
            messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Header
            JLabel headerLabel = new JLabel("🖼️ Image Message #" + messageCount);
            headerLabel.setFont(new Font("Arial", Font.BOLD, 13));
            headerLabel.setForeground(new Color(200, 0, 100));
            messagePanel.add(headerLabel);

            JLabel infoLabel = new JLabel("From: " + sender + " | Time: " + timestamp + " | Size: " + (imageData.length / 1024) + " KB");
            infoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            infoLabel.setForeground(Color.DARK_GRAY);
            messagePanel.add(infoLabel);

            messagePanel.add(Box.createVerticalStrut(5));

            // Scale image if too large
            int maxWidth = 600;
            int maxHeight = 400;

            double scaleX = (double) maxWidth / image.getWidth();
            double scaleY = (double) maxHeight / image.getHeight();
            double scale = Math.min(Math.min(scaleX, scaleY), 1.0); // Don't upscale

            int scaledWidth = (int) (image.getWidth() * scale);
            int scaledHeight = (int) (image.getHeight() * scale);

            Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
            imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            imageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Make image clickable to view full size
            imageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            imageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    showFullSizeImage(image);
                }
            });
            imageLabel.setToolTipText("Click to view full size");

            messagePanel.add(imageLabel);

            chatPanel.add(messagePanel);
            chatPanel.add(Box.createVerticalStrut(10));
            chatPanel.revalidate();

            // Scroll to bottom
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });

        } catch (IOException e) {
            addSystemMessage("Error displaying image: " + e.getMessage());
        }
    }

    private void showFullSizeImage(BufferedImage image) {
        JDialog dialog = new JDialog(this, "Full Size Image", false);
        dialog.setLayout(new BorderLayout());

        JLabel imageLabel = new JLabel(new ImageIcon(image));
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.setPreferredSize(new Dimension(
            Math.min(image.getWidth() + 50, 1200),
            Math.min(image.getHeight() + 50, 800)
        ));

        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addSystemMessage(String message) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(new Color(255, 250, 205));
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createLineBorder(new Color(218, 165, 32), 1)
        ));
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        JLabel label = new JLabel("⚠️ [" + timestamp + "] " + message);
        label.setFont(new Font("Arial", Font.ITALIC, 12));
        label.setForeground(new Color(139, 69, 19));
        messagePanel.add(label);

        chatPanel.add(messagePanel);
        chatPanel.add(Box.createVerticalStrut(5));
        chatPanel.revalidate();

        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
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

