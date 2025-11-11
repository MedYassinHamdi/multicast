package tcp;



import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class Client extends JFrame {
    private static final long serialVersionUID = 1L;

    // Réseau
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread readerThread;
    private volatile boolean connected = false;

    // UI
    private JTextPane chatPane;
    private JTextField inputField;
    private JButton btnSend, btnFile, btnConnect, btnDisconnect;
    private JTextField txtHost, txtPort, txtPseudo;
    private DefaultComboBoxModel<String> targetModel;
    private JComboBox<String> targetCombo;
    private JLabel statusLabel;

    public Client() {
        super("💬 Client TCP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));
        setContentPane(root);

        // Top: connexion
        JPanel top = new JPanel(new GridBagLayout());
        top.setBackground(new Color(21, 64, 160));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.gridy = 0;

        JLabel lp = new JLabel("Pseudo:"); lp.setForeground(Color.WHITE);
        c.gridx = 0; top.add(lp, c);
        txtPseudo = new JTextField("User", 10);
        c.gridx = 1; top.add(txtPseudo, c);

        JLabel lh = new JLabel("Hôte:"); lh.setForeground(Color.WHITE);
        c.gridx = 2; top.add(lh, c);
        txtHost = new JTextField("127.0.0.1", 10);
        c.gridx = 3; top.add(txtHost, c);

        JLabel lpt = new JLabel("Port:"); lpt.setForeground(Color.WHITE);
        c.gridx = 4; top.add(lpt, c);
        txtPort = new JTextField("9999", 6);
        c.gridx = 5; top.add(txtPort, c);

        btnConnect = new JButton("Se connecter");
        btnConnect.setBackground(new Color(0, 150, 110));
        btnConnect.setForeground(Color.WHITE);
        btnDisconnect = new JButton("Déconnexion");
        btnDisconnect.setBackground(new Color(220, 20, 60));
        btnDisconnect.setForeground(Color.WHITE);
        c.gridx = 6; top.add(btnConnect, c);
        c.gridx = 7; top.add(btnDisconnect, c);

        root.add(top, BorderLayout.NORTH);

        // Centre: Destinataire + Discussion
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.25);

        JPanel left = new JPanel(new GridBagLayout());
        left.setBorder(BorderFactory.createTitledBorder("Destinataire"));
        GridBagConstraints lc = new GridBagConstraints();
        lc.insets = new Insets(6,6,6,6);
        lc.gridx = 0; lc.gridy = 0; lc.fill = GridBagConstraints.HORIZONTAL; lc.weightx = 1.0;
        targetModel = new DefaultComboBoxModel<>();
        targetModel.addElement("Tous");
        targetCombo = new JComboBox<>(targetModel);
        left.add(targetCombo, lc);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Discussion"));
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setContentType("text/plain");
        right.add(new JScrollPane(chatPane), BorderLayout.CENTER);

        split.setLeftComponent(left);
        split.setRightComponent(right);
        root.add(split, BorderLayout.CENTER);

        // Bas: (1) champ + boutons, (2) barre d'état empilée => pas de conflit BorderLayout
        JPanel south = new JPanel(new BorderLayout(8,8));

        JPanel bottom = new JPanel(new BorderLayout(8,8));
        inputField = new JTextField();
        JPanel buttons = new JPanel();
        btnFile = new JButton("📎 Image / Fichier");
        btnSend = new JButton("Envoyer");
        btnFile.setBackground(new Color(99,102,241));
        btnFile.setForeground(Color.WHITE);
        btnSend.setBackground(new Color(59,130,246));
        btnSend.setForeground(Color.WHITE);
        buttons.add(btnFile);
        buttons.add(btnSend);
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);

        statusLabel = new JLabel("Hors ligne");
        statusLabel.setForeground(new Color(160,0,0));

        south.add(bottom, BorderLayout.CENTER);
        south.add(statusLabel, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        // Actions
        btnConnect.addActionListener(e -> connect());
        btnDisconnect.addActionListener(e -> disconnect());
        btnSend.addActionListener(e -> sendText());
        btnFile.addActionListener(e -> sendFile());
        inputField.addActionListener(e -> sendText());

        updateButtons();

        // Fermer proprement
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { disconnect(); }});
    }

    // ─────────── Connexion ───────────
    private void connect() {
        if (connected) { info("Déjà connecté."); return; }
        String host = txtHost.getText().trim();
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
            if (port <= 0 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) { warn("Port invalide."); return; }

        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream()); out.flush();
            in  = new ObjectInputStream(socket.getInputStream());

            // Présenter le pseudo au serveur
            Message hello = new Message(Message.Type.TEXTE, safePseudo(), "HELLO", "hello");
            out.writeObject(hello); out.flush();

            connected = true;
            readerThread = new Thread(this::readLoop, "TCP-Reader");
            readerThread.start();

            setStatus(true, "Connecté — " + host + ":" + port);
            info("Connecté au serveur.");
            updateButtons();
        } catch (IOException ex) {
            warn("Connexion échouée : " + ex.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
        try { if (in != null) in.close(); } catch (IOException ignore) {}
        try { if (out != null) out.close(); } catch (IOException ignore) {}
        try { if (socket != null) socket.close(); } catch (IOException ignore) {}
        socket = null; in = null; out = null;
        if (readerThread != null && readerThread.isAlive()) {
            try { readerThread.join(200); } catch (InterruptedException ignore) {}
        }
        setStatus(false, "Hors ligne");
        updateButtons();
    }

    private void readLoop() {
        while (connected) {
            try {
                Object o = in.readObject();
                if (!(o instanceof Message msg)) continue;

                switch (msg.type) {
                    case TEXTE -> appendText("💬 " + msg.sender + " → " + msg.target + " : " + msg.text + "\n");
                    case FICHIER -> {
                        if (msg.fileBytes != null && msg.filename != null) {
                            String lower = msg.filename.toLowerCase();
                            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")) {
                                appendImage(new ImageIcon(msg.fileBytes));
                            }
                            appendText("🗂️ Fichier reçu de " + msg.sender + " : " + msg.filename +
                                       " (" + msg.fileBytes.length + " octets)\n");
                        }
                    }
                    case LISTE -> SwingUtilities.invokeLater(() -> {
                        targetModel.removeAllElements();
                        targetModel.addElement("Tous");
                        String[] users = (msg.text == null ? "" : msg.text).split(",");
                        for (String u : users) if (u != null && !u.isBlank()) targetModel.addElement(u.trim());
                    });
                }

            } catch (EOFException eof) {
                info("Serveur fermé.");
                break;
            } catch (Exception ex) {
                if (connected) warn("Erreur réception : " + ex.getMessage());
                break;
            }
        }
        disconnect();
    }

    // ─────────── Envoi ───────────
    private void sendText() {
        if (!connected) { warn("Connectez-vous d'abord."); return; }
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        String target = (String) targetCombo.getSelectedItem();
        if (target == null || target.isBlank()) target = "Tous";
        Message msg = new Message(Message.Type.TEXTE, safePseudo(), target, text);
        try {
            out.writeObject(msg); out.flush();
            appendText("↗️ (" + target + ") " + text + "\n");
            inputField.setText("");
        } catch (IOException e) { warn("Erreur envoi : " + e.getMessage()); }
    }

    private void sendFile() {
        if (!connected) { warn("Connectez-vous d'abord."); return; }
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File f = chooser.getSelectedFile();
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            String target = (String) targetCombo.getSelectedItem();
            if (target == null || target.isBlank()) target = "Tous";
            Message msg = new Message(Message.Type.FICHIER, safePseudo(), target, f.getName(), bytes);
            out.writeObject(msg); out.flush();
            appendText("↗️ Fichier envoyé : " + f.getName() + " (" + bytes.length + " octets)\n");
        } catch (IOException e) { warn("Erreur envoi fichier : " + e.getMessage()); }
    }

    // ─────────── Helpers UI ───────────
    private void appendText(String text) {
        SwingUtilities.invokeLater(() -> {
            Document doc = chatPane.getDocument();
            try {
                doc.insertString(doc.getLength(), text, null);
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    private void appendImage(ImageIcon icon) {
        SwingUtilities.invokeLater(() -> {
            chatPane.setCaretPosition(chatPane.getDocument().getLength());
            chatPane.insertIcon(icon);
            appendText("\n");
        });
    }

    private void setStatus(boolean on, String text) {
        statusLabel.setText(text);
        statusLabel.setForeground(on ? new Color(0,128,0) : new Color(160,0,0));
    }

    private void updateButtons() {
        btnConnect.setEnabled(!connected);
        btnDisconnect.setEnabled(connected);
        txtHost.setEnabled(!connected);
        txtPort.setEnabled(!connected);
        txtPseudo.setEnabled(!connected);
        btnSend.setEnabled(connected);
        btnFile.setEnabled(connected);
        targetCombo.setEnabled(connected);
    }

    private String safePseudo() {
        String p = txtPseudo.getText();
        if (p == null) p = "";
        p = p.trim();
        if (p.isEmpty()) p = "User";
        return p;
    }

    private void info(String m){ JOptionPane.showMessageDialog(this, m, "Info", JOptionPane.INFORMATION_MESSAGE); }
    private void warn(String m){ JOptionPane.showMessageDialog(this, m, "Attention", JOptionPane.WARNING_MESSAGE); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client().setVisible(true));
    }
}
