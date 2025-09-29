import javax.swing.*;
import java.awt.*;

public class StartScreenPanel extends JPanel {
    private JFrame mainFrame; // Stores a reference to the main frame

    public StartScreenPanel(JFrame frame) {
        this.mainFrame = frame;
        setLayout(null);
        setBackground(new Color(135, 206, 235));

        JLabel title = new JLabel("Baseball Simulator");
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        title.setBounds(300, 50, 400, 50);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title);

        JButton pitchingButton = new JButton("Pitching");
        pitchingButton.setBounds(300, 500, 200, 50);
        pitchingButton.addActionListener(e -> {
            // Call method in Main class to switch panel
            Main.showPitchSelectionScreen(mainFrame, false); // Pitching mode selects pitcher
        });
        add(pitchingButton);

        JButton hittingButton = new JButton("Hitting");
        hittingButton.setBounds(500, 500, 200, 50);
        hittingButton.addActionListener(e -> {
            // Call method in Main class to switch panel
            Main.showPitchSelectionScreen(mainFrame, true); // Hitting mode selects pitcher
        });
        add(hittingButton);

        // Add Play Mode button
        JButton playModeButton = new JButton("Play Mode");
        playModeButton.setBounds(400, 560, 200, 50); // Adjust position
        playModeButton.addActionListener(e -> {
            Main.showPlayModeScreen(mainFrame); // Enter Play Mode
        });
        add(playModeButton);
    }
}