import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Base64;

public class WebsocketServer {

    private static final int PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("WebSocket server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 OutputStream outputStream = clientSocket.getOutputStream()) {

                // Perform WebSocket handshake
                String data;
                StringBuilder request = new StringBuilder();
                while (!(data = reader.readLine()).isEmpty()) {
                    request.append(data).append("\r\n");
                }

                String webSocketKey = request.toString().split("Sec-WebSocket-Key: ")[1].split("\r\n")[0];
                String acceptKey = generateAcceptKey(webSocketKey);

                String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";
                outputStream.write(response.getBytes());

                // Echo messages back to the client and log them
                while (true) {
                    int firstByte = clientSocket.getInputStream().read();
                    if (firstByte == -1) break;

                    int secondByte = clientSocket.getInputStream().read();
                    int payloadLength = secondByte & 0x7F;

                    if (payloadLength == 126) {
                        byte[] extended = new byte[2];
                        clientSocket.getInputStream().read(extended, 0, 2);
                        payloadLength = ((extended[0] & 0xFF) << 8) | (extended[1] & 0xFF);
                    } else if (payloadLength == 127) {
                        byte[] extended = new byte[8];
                        clientSocket.getInputStream().read(extended, 0, 8);
                        payloadLength = 0;
                        for (int i = 0; i < 8; i++) {
                            payloadLength = (payloadLength << 8) | (extended[i] & 0xFF);
                        }
                    }

                    byte[] payload = new byte[payloadLength];
                    clientSocket.getInputStream().read(payload, 0, payloadLength);

                    // Log the received message
                    String receivedMessage = new String(payload, "UTF-8");
                    System.out.println("Received message: " + receivedMessage);

                    // Send the same message back to the client
                    outputStream.write(new byte[]{(byte) 0x81, (byte) payloadLength});
                    outputStream.write(payload);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String generateAcceptKey(String webSocketKey) throws Exception {
            String key = webSocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(key.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        }
    }
}
