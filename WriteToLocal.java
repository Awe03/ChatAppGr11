import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WriteToLocal {
    public void saveMessage(String message) {
        try {
            File file = new File("history.json");
            String messagesJson;
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get("history.json")), StandardCharsets.UTF_8);
                // Check if the file is not empty
                if (!content.equals("[]")) {
                    messagesJson = content.substring(0, content.length() - 1) + ",\"" + message.replace("\"", "\\\"") + "\"]";
                } else {
                    messagesJson = "[\"" + message.replace("\"", "\\\"") + "\"]";
                }
            } else {
                messagesJson = "[\"" + message.replace("\"", "\\\"") + "\"]";
            }
            Files.write(Paths.get("history.json"), messagesJson.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> loadChatHistory() {
        List<String> messages = new ArrayList<>();
        try {
            File file = new File("history.json");
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get("history.json")), StandardCharsets.UTF_8);
                // Remove the brackets at the beginning and the end
                String[] messagesArray = content.substring(1, content.length() - 1).split("\",\"");
                for (String message : messagesArray) {
                    // Remove leading and trailing quotes
                    message = message.replaceAll("^\"|\"$", "");
                    messages.add(message.replace("\\\"", "\""));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }
}