import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class MainClient {

    private final JTextArea chatArea;
    private final JTextField inputField;
    private OutputStream outputStream;

    public MainClient(String username, String ngrokUrl) {
        JFrame frame = new JFrame("Chat App - " + username);
        chatArea = new JTextArea();
        inputField = new JTextField(30);
        JButton sendButton = new JButton("Send as " + username);

        // Set placeholder text
        inputField.setText("Type your message");
        inputField.setForeground(Color.GRAY);

        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (inputField.getText().equals("Type your message")) {
                    inputField.setText("");
                    inputField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (inputField.getText().isEmpty()) {
                    inputField.setText("Type your message");
                    inputField.setForeground(Color.GRAY);
                }
            }
        });

        chatArea.setEditable(false);

        // Create a panel to hold the chat area and input panel
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        frame.setLayout(new BorderLayout());
        frame.add(chatPanel, BorderLayout.CENTER);

        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText();
                if (!message.isEmpty()) {
                    sendMessage(message, username);
                    inputField.setText("");
                }
            }
        };

        sendButton.addActionListener(sendAction);

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendAction.actionPerformed(null);
                }
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 720);
        frame.setVisible(true);

        connectToServer(ngrokUrl);
    }

    private void connectToServer(String ngrokUrl) {
        try {
            URI uri = new URI(ngrokUrl);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 80 : uri.getPort(); // Default to port 80 if not specified

            Socket socket = new Socket(host, port);
            outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            // Perform WebSocket handshake
            String handshake = "GET / HTTP/1.1\r\n" +
                    "Host: " + host + "\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                    "Sec-WebSocket-Version: 13\r\n\r\n";

            outputStream.write(handshake.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            // Read the server response
            byte[] buffer = new byte[4096];
            int bytesRead = inputStream.read(buffer);
            String response = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            System.out.println(response);
            if (response.contains("HTTP/1.1 101 Switching Protocols")) {
                chatArea.append("Connected to server\n");

                // Start a thread to read messages from the server
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

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter username: ");
        String username = sc.nextLine();

        System.out.print("Enter the ngrok URL: ");
        String ngrokUrl = sc.nextLine();

        SwingUtilities.invokeLater(() -> new MainClient(username, ngrokUrl));

        sc.close();
    }
}