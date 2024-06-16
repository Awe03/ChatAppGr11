import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private Socket socket;
    private OutputStream outputStream;

    public Main() {
        frame = new JFrame("Chat App");
        chatArea = new JTextArea();
        inputField = new JTextField(30);
        sendButton = new JButton("Send");

        chatArea.setEditable(false);
        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.add(inputField);
        inputPanel.add(sendButton);
        frame.add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText();
                if (!message.isEmpty()) {
                    sendMessage(message);
                    inputField.setText("");
                }
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setVisible(true);

        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 8080);
            outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            // Perform WebSocket handshake
            String handshake = "GET / HTTP/1.1\r\n" +
                    "Host: localhost:8080\r\n" +
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
                            SwingUtilities.invokeLater(() -> chatArea.append("Received: " + receivedMessage + "\n"));
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

    private void sendMessage(String message) {
        try {
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            outputStream.write(new byte[]{(byte) 0x81, (byte) messageBytes.length});
            outputStream.write(messageBytes);
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Start the WebSocket server in a separate thread
        new Thread(() -> {
            WebsocketServer.main(new String[]{});
        }).start();

        // Start the Swing application
        SwingUtilities.invokeLater(() -> new Main());
    }
}
