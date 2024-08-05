package Components;
import javax.swing.*;
import java.awt.*;

public class ChatBubble extends JPanel {
    public ChatBubble(String message, boolean isSent) {
        setLayout(new BorderLayout());
        JTextArea messageArea = new JTextArea(message);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setEditable(false);
        messageArea.setBackground(isSent ? new Color(173, 216, 230) : new Color(240, 240, 240));
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(messageArea, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setAlignmentX(isSent ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
    }
}