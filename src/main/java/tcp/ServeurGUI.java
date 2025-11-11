package tcp;

import tcp.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class ServeurGUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_PORT = 9999;

    // Réseau
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread acceptThread;
    private final Set<ClientHandler> clients = new CopyOnWriteArraySet<>();

    // UI
    private JTextArea logArea;
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;
    private JButton btnStart, btnStop;
    private JTextField txtPort;
    private JLabel statusLabel;

    public ServeurGUI() {
        super("🖥️ Serveur TCP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        // Top bar
        JPanel top = new JPanel(new GridBagLayout());
        top.setBackground(new Color(21, 64, 160));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.gridy = 0;

        JLabel lpt = new JLabel("Port:"); lpt.setForeground(Color.WHITE);
        c.gridx = 0; top.add(lpt, c);
        txtPort = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        c.gridx = 1; top.add(txtPort, c);

        btnStart = new JButton("Démarrer");
        btnStart.setBackground(new Color(0,150,110));
        btnStart.setForeground(Color.WHITE);
        btnStop = new JButton("Arrêter");
        btnStop.setBackground(new Color(220,20,60));
        btnStop.setForeground(Color.WHITE);
        c.gridx = 2; top.add(btnStart, c);
        c.gridx = 3; top.add(btnStop, c);

        root.add(top, BorderLayout.NORTH);

        // Split: clients + logs
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.3);

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Clients connectés"));
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        left.add(new JScrollPane(clientList), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Logs"));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        right.add(new JScrollPane(logArea), BorderLayout.CENTER);

        split.setLeftComponent(left);
        split.setRightComponent(right);
        root.add(split, BorderLayout.CENTER);

        // Status
        statusLabel = new JLabel("Hors ligne");
        statusLabel.setForeground(new Color(160,0,0));
        root.add(statusLabel, BorderLayout.SOUTH);

        // Actions
        btnStart.addActionListener(e -> startServer());
        btnStop.addActionListener(e -> stopServer());
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e){ stopServer(); }});

        updateButtons();
    }

    private void startServer() {
        if (running) return;
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
            if (port <= 0 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            alert("Port invalide."); return;
        }
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            acceptThread = new Thread(this::acceptLoop, "TCP-Acceptor");
            acceptThread.start();
            append("✅ Serveur démarré sur le port " + port);
            setStatus(true, "En ligne — port " + port);
            updateButtons();
        } catch (IOException ex) {
            alert("Erreur ouverture serveur : " + ex.getMessage());
        }
    }

    private void stopServer() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignore) {}
        for (ClientHandler c : clients) c.close();
        clients.clear();
        refreshClientList();
        if (acceptThread != null && acceptThread.isAlive()) {
            try { acceptThread.join(200); } catch (InterruptedException ignore) {}
        }
        append("⏹️ Serveur arrêté.");
        setStatus(false, "Hors ligne");
        updateButtons();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = serverSocket.accept();
                ClientHandler h = new ClientHandler(s);
                clients.add(h);
                new Thread(h, "Client-" + s.getPort()).start();
            } catch (IOException e) {
                if (running) append("⚠️ Accept échoué : " + e.getMessage());
            }
        }
    }

    private void broadcastList() {
        String list = clients.stream().map(c -> c.nickname).collect(Collectors.joining(","));
        Message listMsg = new Message(Message.Type.LISTE, "Serveur", "Tous", list);
        for (ClientHandler c : clients) c.send(listMsg);
    }

    private void sendToTarget(Message msg, ClientHandler from) {
        if ("Tous".equalsIgnoreCase(msg.target)) {
            for (ClientHandler c : clients) if (c != from) c.send(msg);
            append(msg.sender + " a envoyé '" + msg.text + "' à tous");
        } else {
            for (ClientHandler c : clients) {
                if (c.nickname.equals(msg.target)) { c.send(msg); break; }
            }
            append(msg.sender + " a envoyé '" + (msg.text != null ? msg.text : msg.filename) + "' à " + msg.target);
        }
    }

    private void refreshClientList() {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (ClientHandler c : clients) clientListModel.addElement(c.nickname);
        });
    }

    private void append(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setStatus(boolean on, String text) {
        statusLabel.setText(text);
        statusLabel.setForeground(on ? new Color(0,128,0) : new Color(160,0,0));
    }

    private void updateButtons() {
        btnStart.setEnabled(!running);
        btnStop.setEnabled(running);
        txtPort.setEnabled(!running);
    }

    private void alert(String m) { JOptionPane.showMessageDialog(this, m, "Info", JOptionPane.INFORMATION_MESSAGE); }

    // ───────────── Client handler ─────────────
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String nickname = "?";

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream()); out.flush();
                in  = new ObjectInputStream(socket.getInputStream());

                // HELLO avec pseudo
                Object hello = in.readObject();
                if (hello instanceof Message hm && hm.type == Message.Type.TEXTE && "HELLO".equals(hm.target)) {
                    nickname = (hm.sender != null && !hm.sender.isBlank()) ? hm.sender : ("User@" + socket.getPort());
                    append("➕ " + nickname + " connecté (" + socket.getInetAddress().getHostAddress() + ")");
                    refreshClientList();
                    broadcastList(); // informe tout le monde (y compris le nouveau)
                }

                while (true) {
                    Object o = in.readObject();
                    if (!(o instanceof Message msg)) continue;

                    switch (msg.type) {
                        case TEXTE -> sendToTarget(msg, this);
                        case FICHIER -> {
                            append(msg.sender + " a envoyé le fichier '" + msg.filename + "' à " +
                                    ("Tous".equalsIgnoreCase(msg.target) ? "tous" : msg.target) +
                                    " (" + (msg.fileBytes == null ? 0 : msg.fileBytes.length) + " octets)");
                            sendToTarget(msg, this);
                        }
                        case LISTE -> { /* pas utilisé côté client */ }
                    }
                }
            } catch (EOFException eof) {
                append("➖ Déconnexion de " + nickname);
            } catch (Exception ex) {
                append("⚠️ Client " + nickname + " : " + ex.getMessage());
            } finally {
                close();
                clients.remove(this);
                refreshClientList();
                broadcastList();
            }
        }

        void send(Message msg) { try { out.writeObject(msg); out.flush(); } catch (IOException ignore) {} }

        void close() {
            try { if (in != null) in.close(); } catch (IOException ignore) {}
            try { if (out != null) out.close(); } catch (IOException ignore) {}
            try { if (socket != null) socket.close(); } catch (IOException ignore) {}
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServeurGUI().setVisible(true));
    }
}
