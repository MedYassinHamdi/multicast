import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Multicast Sender with Swing GUI
 * Sends messages to all receivers in the multicast group
 */
public class MulticastSender extends JFrame {
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JTextArea logArea;
    private MulticastSocket socket;
    private InetAddress group;
    private boolean isRunning = false;

    public MulticastSender() {
        super("Multicast Sender");
        initializeUI();
        initializeNetwork();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Title Panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(70, 130, 180));
        JLabel titleLabel = new JLabel("📡 Multicast Message Sender");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // Center Panel - Message Input
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel inputLabel = new JLabel("Enter Message:");
        inputLabel.setFont(new Font("Arial", Font.BOLD, 14));
        centerPanel.add(inputLabel, BorderLayout.NORTH);

        messageArea = new JTextArea(5, 40);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 14));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScroll = new JScrollPane(messageArea);
        centerPanel.add(messageScroll, BorderLayout.CENTER);

        sendButton = new JButton("Send to All Receivers");
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendButton.setBackground(new Color(34, 139, 34));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(e -> sendMessage());
        centerPanel.add(sendButton, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom Panel - Log Area
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JLabel logLabel = new JLabel("Activity Log:");
        logLabel.setFont(new Font("Arial", Font.BOLD, 12));
        bottomPanel.add(logLabel, BorderLayout.NORTH);

        logArea = new JTextArea(8, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        logArea.setBackground(new Color(245, 245, 245));
        JScrollPane logScroll = new JScrollPane(logArea);
        bottomPanel.add(logScroll, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        // Info Panel
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel addressLabel = new JLabel("Multicast Address: " + MulticastConfig.MULTICAST_ADDRESS);
        JLabel portLabel = new JLabel("Port: " + MulticastConfig.PORT);
        addressLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        portLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoPanel.add(addressLabel);
        infoPanel.add(portLabel);
        add(infoPanel, BorderLayout.WEST);

        // Add enter key listener to message area
        messageArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        setSize(700, 600);
        setLocationRelativeTo(null);
    }

    private void initializeNetwork() {
        try {
            socket = new MulticastSocket();
            group = InetAddress.getByName(MulticastConfig.MULTICAST_ADDRESS);
            isRunning = true;
            log("✓ Sender initialized successfully");
            log("Ready to send messages to multicast group");
            log("----------------------------------------");
        } catch (IOException e) {
            log("✗ Error initializing network: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to initialize network: " + e.getMessage(),
                    "Network Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a message to send!",
                    "Empty Message",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length,
                    group,
                    MulticastConfig.PORT
            );
            socket.send(packet);

            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            log("[" + timestamp + "] Sent: " + message);
            messageArea.setText("");
            messageArea.requestFocus();

        } catch (IOException e) {
            log("✗ Error sending message: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to send message: " + e.getMessage(),
                    "Send Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void dispose() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            MulticastSender sender = new MulticastSender();
            sender.setVisible(true);
        });
    }
}

