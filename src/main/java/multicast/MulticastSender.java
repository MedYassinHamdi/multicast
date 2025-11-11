package multicast;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Multicast Sender with Swing GUI
 * Sends messages to all receivers in the multicast group
 */
public class MulticastSender extends JFrame {
    private JTextArea messageArea;
    private JButton sendTextButton;
    private JButton sendImageButton;
    private JTextArea logArea;
    private JLabel imagePreviewLabel;
    private MulticastSocket socket;
    private InetAddress group;
    private boolean isRunning = false;
    private File selectedImageFile;
    private BufferedImage selectedImage;

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
        JLabel titleLabel = new JLabel("📡 Multicast Message & Image Sender");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // Main Content Panel
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left Panel - Text Message
        JPanel textPanel = new JPanel(new BorderLayout(5, 5));
        textPanel.setBorder(BorderFactory.createTitledBorder("Text Message"));

        messageArea = new JTextArea(8, 30);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 14));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScroll = new JScrollPane(messageArea);
        textPanel.add(messageScroll, BorderLayout.CENTER);

        sendTextButton = new JButton("📤 Send Text Message");
        sendTextButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendTextButton.setBackground(new Color(34, 139, 34));
        sendTextButton.setForeground(Color.WHITE);
        sendTextButton.setFocusPainted(false);
        sendTextButton.addActionListener(e -> sendTextMessage());
        textPanel.add(sendTextButton, BorderLayout.SOUTH);

        mainPanel.add(textPanel);

        // Right Panel - Image
        JPanel imagePanel = new JPanel(new BorderLayout(5, 5));
        imagePanel.setBorder(BorderFactory.createTitledBorder("Image"));

        imagePreviewLabel = new JLabel("No image selected", SwingConstants.CENTER);
        imagePreviewLabel.setPreferredSize(new Dimension(300, 200));
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        imagePreviewLabel.setBackground(Color.WHITE);
        imagePreviewLabel.setOpaque(true);
        JScrollPane imageScroll = new JScrollPane(imagePreviewLabel);
        imagePanel.add(imageScroll, BorderLayout.CENTER);

        JPanel imageButtonPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        JButton selectImageButton = new JButton("🖼️ Select Image");
        selectImageButton.setFont(new Font("Arial", Font.BOLD, 13));
        selectImageButton.setBackground(new Color(70, 130, 180));
        selectImageButton.setForeground(Color.WHITE);
        selectImageButton.setFocusPainted(false);
        selectImageButton.addActionListener(e -> selectImage());
        imageButtonPanel.add(selectImageButton);

        sendImageButton = new JButton("📸 Send Image");
        sendImageButton.setFont(new Font("Arial", Font.BOLD, 13));
        sendImageButton.setBackground(new Color(255, 140, 0));
        sendImageButton.setForeground(Color.WHITE);
        sendImageButton.setFocusPainted(false);
        sendImageButton.setEnabled(false);
        sendImageButton.addActionListener(e -> sendImage());
        imageButtonPanel.add(sendImageButton);

        imagePanel.add(imageButtonPanel, BorderLayout.SOUTH);

        mainPanel.add(imagePanel);

        add(mainPanel, BorderLayout.CENTER);

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
        JLabel addressLabel = new JLabel("Multicast: " + MulticastConfig.MULTICAST_ADDRESS);
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
                    sendTextMessage();
                }
            }
        });

        setSize(900, 700);
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

    private void selectImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                       name.endsWith(".png") || name.endsWith(".gif");
            }

            @Override
            public String getDescription() {
                return "Image Files (*.jpg, *.png, *.gif)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();

            // Check file size
            if (selectedImageFile.length() > MulticastConfig.MAX_IMAGE_SIZE) {
                JOptionPane.showMessageDialog(this,
                    "Image file is too large! Maximum size is 5 MB.",
                    "File Too Large",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                selectedImage = ImageIO.read(selectedImageFile);
                displayImagePreview(selectedImage);
                sendImageButton.setEnabled(true);
                log("✓ Image selected: " + selectedImageFile.getName() +
                    " (" + (selectedImageFile.length() / 1024) + " KB)");
            } catch (IOException e) {
                log("✗ Error loading image: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                    "Failed to load image: " + e.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void displayImagePreview(BufferedImage image) {
        // Scale image to fit preview area
        int maxWidth = 280;
        int maxHeight = 180;

        double scaleX = (double) maxWidth / image.getWidth();
        double scaleY = (double) maxHeight / image.getHeight();
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) (image.getWidth() * scale);
        int scaledHeight = (int) (image.getHeight() * scale);

        Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        imagePreviewLabel.setIcon(new ImageIcon(scaledImage));
        imagePreviewLabel.setText("");
    }

    private void sendTextMessage() {
        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a message to send!",
                    "Empty Message",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            MulticastMessage msg = new MulticastMessage(message);
            byte[] buffer = msg.toBytes();

            if (buffer.length > MulticastConfig.BUFFER_SIZE) {
                JOptionPane.showMessageDialog(this,
                    "Message is too long!",
                    "Message Too Large",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length,
                    group,
                    MulticastConfig.PORT
            );
            socket.send(packet);

            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            log("[" + timestamp + "] Sent text: " + message);
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

    private void sendImage() {
        if (selectedImage == null || selectedImageFile == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select an image first!",
                    "No Image Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Convert image to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String format = getImageFormat(selectedImageFile.getName());
            ImageIO.write(selectedImage, format, baos);
            byte[] imageBytes = baos.toByteArray();

            // Create and send multicast message
            MulticastMessage msg = new MulticastMessage(imageBytes, format);
            byte[] buffer = msg.toBytes();

            if (buffer.length > MulticastConfig.BUFFER_SIZE) {
                JOptionPane.showMessageDialog(this,
                    "Image is too large to send! Try a smaller image or reduce quality.",
                    "Image Too Large",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length,
                    group,
                    MulticastConfig.PORT
            );
            socket.send(packet);

            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            log("[" + timestamp + "] Sent image: " + selectedImageFile.getName() +
                " (" + (buffer.length / 1024) + " KB)");

        } catch (IOException e) {
            log("✗ Error sending image: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to send image: " + e.getMessage(),
                    "Send Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getImageFormat(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "png";
        if (lower.endsWith(".gif")) return "gif";
        return "jpg"; // Default to jpg for jpeg files
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

