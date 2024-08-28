import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WriteToLocal {

    private static final Path HISTORY_PATH = Paths.get("history.json");

    // Method to save a message to the local storage file (history.json)
    public void saveMessage(String message) {
        try {
            File file = HISTORY_PATH.toFile();
            String messagesJson;
            if (file.exists()) {
                // Read the existing content of the file
                String content = new String(Files.readAllBytes(HISTORY_PATH), StandardCharsets.UTF_8);
                // Check if the file is not empty
                if (!content.equals("[]")) {
                    // Append the new message to the existing JSON array
                    messagesJson = content.substring(0, content.length() - 1) + ",\"" + message.replace("\"", "\\\"") + "\"]";
                } else {
                    // If the file is empty, initialize the JSON array with the new message
                    messagesJson = "[\"" + message.replace("\"", "\\\"") + "\"]";
                }
            } else {
                // If the file does not exist, create a new JSON array with the message
                messagesJson = "[\"" + message.replace("\"", "\\\"") + "\"]";
            }
            // Write the updated JSON array back to the file
            Files.write(HISTORY_PATH, messagesJson.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to load chat history from the local storage file (history.json)
    public List<String> loadChatHistory() {
        List<String> messages = new ArrayList<>();
        try {
            File file = HISTORY_PATH.toFile();
            if (file.exists()) {
                // Read the content of the file
                String content = new String(Files.readAllBytes(HISTORY_PATH), StandardCharsets.UTF_8);
                // Remove the brackets at the beginning and the end of the JSON array
                String[] messagesArray = content.substring(1, content.length() - 1).split("\",\"");
                for (String message : messagesArray) {
                    // Remove leading and trailing quotes and unescape any escaped quotes
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