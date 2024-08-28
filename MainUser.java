import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class MainUser {

    private final JTextArea chatArea;
    private final JTextField inputField;
    private OutputStream outputStream;
    private boolean typedSomething = false;

    public MainUser(String username, String ngrokUrl) {
        // Create the main application window
        JFrame frame = new JFrame("Chat App - " + username);

        // Define a modern font to be used in the UI
        Font modernFont = new Font("Segoe UI", Font.PLAIN, 16);

        // Initialize the chat area where messages will be displayed
        chatArea = new JTextArea();
        chatArea.setFont(modernFont);
        chatArea.setBackground(new Color(40, 44, 52));
        chatArea.setForeground(Color.WHITE);
        chatArea.setEditable(false);
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Initialize the input field where users will type their messages
        inputField = new JTextField(30);
        inputField.setFont(modernFont);
        inputField.setBackground(new Color(50, 54, 62));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create the send button
        JButton sendButton = new JButton("Send");
        sendButton.setFont(modernFont);
        sendButton.setBackground(new Color(0, 123, 255));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add mouse listener to change button color on hover
        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                sendButton.setBackground(new Color(0, 150, 255));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sendButton.setBackground(new Color(0, 123, 255));
            }
        });

        // Placeholder text handling for the input field
        inputField.setText("Type your message");
        inputField.setForeground(Color.GRAY);

        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (inputField.getText().equals("Type your message")) {
                    inputField.setText("");
                    inputField.setForeground(Color.WHITE);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (inputField.getText().isEmpty()) {
                    inputField.setText("Type your message");
                    inputField.setForeground(Color.GRAY);
                    typedSomething = false;
                }
            }
        });

        // Action listener for the send button
        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText();
                if (typedSomething && !message.isEmpty()) {
                    sendMessage(message, username);
                    inputField.setText("");
                    typedSomething = false;
                }
            }
        };

        sendButton.addActionListener(sendAction);

        // Key listener for the input field to handle Enter key press
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!inputField.getText().equals("Type your message")) {
                    typedSomething = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendAction.actionPerformed(null);
                }
            }
        });

        // Layout for the input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(30, 34, 42));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Layout for the chat panel
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(new Color(30, 34, 42));
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // Set up the main frame with the chat panel
        frame.setLayout(new BorderLayout());
        frame.add(chatPanel, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 720);
        frame.setVisible(true);

        // Connect to the server using the provided ngrok URL
        connectToServer(ngrokUrl);

        // Timer to refresh chat history every second
        Timer refreshTimer = new Timer(1000, (e) -> refreshChatHistory());
        refreshTimer.start();
    }

    // Method to connect to the server using the provided ngrok URL
    private void connectToServer(String ngrokUrl) {
        try {
            URI uri = new URI(ngrokUrl);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 80 : uri.getPort();

            Socket socket = new Socket(host, port);
            outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            // WebSocket handshake request
            String handshake = "GET / HTTP/1.1\r\n" +
                    "Host: " + host + "\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                    "Sec-WebSocket-Version: 13\r\n\r\n";

            outputStream.write(handshake.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            byte[] buffer = new byte[4096];
            int bytesRead = inputStream.read(buffer);
            String response = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);

            // Check if the server accepted the WebSocket handshake
            if (response.contains("HTTP/1.1 101 Switching Protocols")) {
                chatArea.append("Connected to server\n");

                // Start a new thread to listen for incoming messages
                new Thread(() -> {
                    try {
                        while (true) {
                            int firstByte = inputStream.read();
                            if (firstByte == -1) break;

                            int secondByte = inputStream.read();
                            int payloadLength = secondByte & 0x7F;

                            if (payloadLength == 126) {
                                byte[] extended = new byte[2];
                                inputStream.read(extended, 0, 2);
                                payloadLength = ((extended[0] & 0xFF) << 8) | (extended[1] & 0xFF);
                            } else if (payloadLength == 127) {
                                byte[] extended = new byte[8];
                                inputStream.read(extended, 0, 8);
                                payloadLength = 0;
                                for (int i = 0; i < 8; i++) {
                                    payloadLength = (payloadLength << 8) | (extended[i] & 0xFF);
                                }
                            }

                            byte[] payload = new byte[payloadLength];
                            inputStream.read(payload, 0, payloadLength);

                            String receivedMessage = new String(payload, StandardCharsets.UTF_8);
                            SwingUtilities.invokeLater(() -> chatArea.append(receivedMessage + "\n"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                chatArea.append("Failed to connect to server\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to send a message to the server
    private void sendMessage(String message, String username) {
        try {
            String formattedMessage = username + ": " + message;
            byte[] messageBytes = formattedMessage.getBytes(StandardCharsets.UTF_8);
            outputStream.write(new byte[]{(byte) 0x81, (byte) messageBytes.length});
            outputStream.write(messageBytes);
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to refresh chat history from the local storage file
    private void refreshChatHistory() {
        try {
            if (Files.exists(Paths.get("history.json"))) {
                String content = new String(Files.readAllBytes(Paths.get("history.json")), StandardCharsets.UTF_8);
                String[] messagesArray = content.substring(1, content.length() - 1).split("\",\"");
                StringBuilder historyBuilder = new StringBuilder("Connected to server\n");
                for (String message : messagesArray) {
                    message = message.replaceAll("^\"|\"$", "").replace("\\\"", "\"");
                    historyBuilder.append(message).append("\n");
                }
                chatArea.setText(historyBuilder.toString());
            } else {
                chatArea.setText("Connected to server\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter username: ");
        String username = sc.nextLine();

        // Start the WebSocket server in a new thread
        new Thread(() -> {
            WebsocketServer.main(new String[]{});
        }).start();

        System.out.print("Enter the ngrok URL: ");
        String ngrokUrl = sc.nextLine();

        // Start the main user interface
        SwingUtilities.invokeLater(() -> new MainUser(username, ngrokUrl));

        sc.close();
    }
}