import javax.swing.*;
import data.Pitcher;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Baseball Simulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);

            showStartScreen(frame);

            frame.setVisible(true);
        });
    }

    public static void showStartScreen(JFrame frame) {
        frame.getContentPane().removeAll();
        StartScreenPanel startScreen = new StartScreenPanel(frame);
        frame.add(startScreen);
        frame.revalidate();
        frame.repaint();
    }

    // Modify showPitchSelectionScreen to receive a boolean parameter
    public static void showPitchSelectionScreen(JFrame frame, boolean isHittingModeSelection) {
        frame.getContentPane().removeAll();
        PitchSelectionPanel selectionPanel = new PitchSelectionPanel(frame, isHittingModeSelection); // Pass the parameter to PitchSelectionPanel
        frame.add(selectionPanel);
        frame.revalidate();
        frame.repaint();
        selectionPanel.requestFocusInWindow();
    }

    // Modify showGamePanel to receive Pitcher object (this method does not need to change, but its calls above have been modified)
    // Add an isPlayMode parameter
    public static void showGamePanel(JFrame frame, boolean isHittingMode, Pitcher selectedPitcher, boolean isPlayMode) {
        frame.getContentPane().removeAll();
        GamePanel gamePanel = new GamePanel(isHittingMode, frame, selectedPitcher, isPlayMode);
        frame.add(gamePanel);
        frame.revalidate();
        frame.repaint();
        gamePanel.requestFocusInWindow();
    }

    // Add showPlayModeScreen method
    public static void showPlayModeScreen(JFrame frame) {
        frame.getContentPane().removeAll();
        // Play Mode directly enters GamePanel, set isHittingMode to true, selectedPitcher to null (because it's randomly selected), and isPlayMode to true
        GamePanel gamePanel = new GamePanel(true, frame, null, true);
        frame.add(gamePanel);
        frame.revalidate();
        frame.repaint();
        gamePanel.requestFocusInWindow();
    }
}