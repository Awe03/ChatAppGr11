import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdminConsole {

    private final JTextArea historyArea;
    private final JTextArea usersArea;
    private final WriteToLocal writeToLocal;

    public AdminConsole() {
        writeToLocal = new WriteToLocal();

        // Create the main application window
        JFrame frame = new JFrame("Admin Console");

        // Define a modern font to be used in the UI
        Font modernFont = new Font("Segoe UI", Font.PLAIN, 16);

        // Initialize the history area where chat messages will be displayed
        historyArea = new JTextArea();
        historyArea.setFont(modernFont);
        historyArea.setBackground(new Color(40, 44, 52));
        historyArea.setForeground(Color.WHITE);
        historyArea.setEditable(false);
        historyArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Initialize the users area where the list of users will be displayed
        usersArea = new JTextArea();
        usersArea.setFont(modernFont);
        usersArea.setBackground(new Color(40, 44, 52));
        usersArea.setForeground(Color.WHITE);
        usersArea.setEditable(false);
        usersArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create a button to reset the chat history
        JButton resetButton = new JButton("Reset Chat History");
        resetButton.setFont(modernFont);
        resetButton.setBackground(new Color(0, 123, 255));
        resetButton.setForeground(Color.WHITE);
        resetButton.setFocusPainted(false);
        resetButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        resetButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add mouse listener to change button color on hover
        resetButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                resetButton.setBackground(new Color(0, 150, 255));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                resetButton.setBackground(new Color(0, 123, 255));
            }
        });

        // Add action listener to reset chat history when the button is clicked
        resetButton.addActionListener((e) -> resetChatHistory());

        // Create the main panel with a border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(30, 34, 42));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create the history panel to hold the history area
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBackground(new Color(30, 34, 42));
        historyPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "Message History", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, modernFont, Color.WHITE));
        historyPanel.add(new JScrollPane(historyArea), BorderLayout.CENTER);

        // Create the users panel to hold the users area
        JPanel usersPanel = new JPanel(new BorderLayout());
        usersPanel.setBackground(new Color(30, 34, 42));
        usersPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "Users", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, modernFont, Color.WHITE));
        usersPanel.add(new JScrollPane(usersArea), BorderLayout.CENTER);
        usersPanel.setPreferredSize(new Dimension(300, 0)); // Set preferred width for users panel

        // Create the button panel to hold the reset button
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(30, 34, 42));
        buttonPanel.add(resetButton);

        // Add the history panel, users panel, and button panel to the main panel
        mainPanel.add(historyPanel, BorderLayout.CENTER);
        mainPanel.add(usersPanel, BorderLayout.EAST);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Set up the main frame with the main panel
        frame.setLayout(new BorderLayout());
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);

        // Load the chat history from history.json
        loadChatHistory();

        // Timer to refresh chat history every second
        Timer refreshTimer = new Timer(1000, (e) -> refreshChatHistory());
        refreshTimer.start();
    }

    // Method to load chat history from local storage and update the UI
    private void loadChatHistory() {
        List<String> messages = writeToLocal.loadChatHistory();
        Set<String> users = new HashSet<>();

        StringBuilder historyBuilder = new StringBuilder();
        for (String message : messages) {
            historyBuilder.append(message).append("\n");
            String[] parts = message.split(":");
            if (parts.length > 1) {
                users.add(parts[0].trim());
            }
        }

        historyArea.setText(historyBuilder.toString());

        StringBuilder usersBuilder = new StringBuilder();
        for (String user : users) {
            usersBuilder.append(user).append("\n");
        }

        usersArea.setText(usersBuilder.toString());
    }

    // Method to reset chat history by deleting the local storage file and clearing the UI
    private void resetChatHistory() {
        try {
            Files.deleteIfExists(Paths.get("history.json"));
            historyArea.setText("");
            usersArea.setText("");
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
                StringBuilder historyBuilder = new StringBuilder();
                Set<String> users = new HashSet<>();
                for (String message : messagesArray) {
                    message = message.replaceAll("^\"|\"$", "").replace("\\\"", "\"");
                    historyBuilder.append(message).append("\n");
                    String[] parts = message.split(":");
                    if (parts.length > 1) {
                        users.add(parts[0].trim());
                    }
                }
                historyArea.setText(historyBuilder.toString());

                StringBuilder usersBuilder = new StringBuilder();
                for (String user : users) {
                    usersBuilder.append(user).append("\n");
                }
                usersArea.setText(usersBuilder.toString());
            } else {
                historyArea.setText("");
                usersArea.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Main method that starts the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(AdminConsole::new);
    }
}