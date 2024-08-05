import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public class WebsocketServer {
    private static final int PORT = 8080;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final WriteToLocal writeToLocal = new WriteToLocal();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("WebSocket server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private OutputStream outputStream;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                outputStream = clientSocket.getOutputStream();

                // Perform WebSocket handshake
                String data;
                StringBuilder request = new StringBuilder();
                while (!(data = reader.readLine()).isEmpty()) {
                    request.append(data).append("\r\n");
                }

                System.out.println("Received request: \n" + request.toString());
                String webSocketKey = null;
                String[] lines = request.toString().split("\r\n");
                for (String line : lines) {
                    if (line.toLowerCase().startsWith("sec-websocket-key: ")) {
                        webSocketKey = line.split(": ")[1];
                        break;
                    }
                }

                if (webSocketKey == null) {
                    throw new IllegalArgumentException("WebSocket key not found in request.");
                }

                String acceptKey = generateAcceptKey(webSocketKey);


                String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";
                outputStream.write(response.getBytes());

                // Load and send chat history to the client
                List<String> history = writeToLocal.loadChatHistory();
                for (String message : history) {
                    sendMessage(message);
                }

                // Echo messages back to the client and log them
                while (true) {
                    int firstByte = clientSocket.getInputStream().read();
                    if (firstByte == -1) break;

                    int secondByte = clientSocket.getInputStream().read();
                    int payloadLength = secondByte & 0x7F;

                    byte[] payload = new byte[payloadLength];
                    clientSocket.getInputStream().read(payload, 0, payloadLength);

                    String receivedMessage = new String(payload, StandardCharsets.UTF_8);
                    System.out.println(receivedMessage);

                    // Save the received message
                    writeToLocal.saveMessage(receivedMessage);

                    // Broadcast the received message to all clients
                    broadcastMessage(receivedMessage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                clients.remove(this);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcastMessage(String message) {
            for (ClientHandler client : clients) {
                try {
                    client.sendMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendMessage(String message) throws Exception {
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            outputStream.write(new byte[]{(byte) 0x81, (byte) messageBytes.length});
            outputStream.write(messageBytes);
            outputStream.flush();
        }

        private String generateAcceptKey(String webSocketKey) throws Exception {
            String key = webSocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }
    }
}