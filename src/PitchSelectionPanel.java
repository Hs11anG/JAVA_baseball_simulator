import data.Pitcher;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PitchSelectionPanel extends JPanel {
    private JFrame mainFrame;
    private DatabaseManager dbManager;
    private JList<Pitcher> pitcherList;
    private DefaultListModel<Pitcher> listModel;
    private boolean isHittingModeSelection; // New variable to determine if it's pitcher selection for hitting mode

    // Modified constructor to receive isHittingModeSelection parameter
    public PitchSelectionPanel(JFrame frame, boolean isHittingModeSelection) {
        this.mainFrame = frame;
        this.isHittingModeSelection = isHittingModeSelection; // Initialize
        this.dbManager = new DatabaseManager(); // Initialize database manager
        setLayout(new BorderLayout());
        setBackground(new Color(135, 206, 235));

        JLabel title = new JLabel("choosing");
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        pitcherList = new JList<>(listModel);
        pitcherList.setFont(new Font("Arial", Font.PLAIN, 20));
        pitcherList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Single selection mode
        pitcherList.setCellRenderer(new DefaultListCellRenderer() { // Custom renderer for better list display
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Pitcher) {
                    Pitcher p = (Pitcher) value;
                    label.setText(p.getPname() ); // Display pitcher name and PID
                }
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(pitcherList);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        add(scrollPane, BorderLayout.CENTER);

        // Select button
        JButton selectButton = new JButton("choose this pitcher");
        selectButton.setFont(new Font("Arial", Font.BOLD, 20));
        selectButton.addActionListener(e -> {
            Pitcher selectedPitcher = pitcherList.getSelectedValue();
            if (selectedPitcher != null) {
                System.out.println("Selected Pitcher: " + selectedPitcher.getPname());
                // Determine which GamePanel mode to enter based on isHittingModeSelection
                // Note: A fourth parameter false is added here, indicating not Play Mode
                if (isHittingModeSelection) {
                    Main.showGamePanel(mainFrame, true, selectedPitcher, false); // Enter hitting mode
                } else {
                    Main.showGamePanel(mainFrame, false, selectedPitcher, false); // Enter pitching mode
                }
            } else {
                JOptionPane.showMessageDialog(this, "choose one pitch！", "no choose", JOptionPane.WARNING_MESSAGE);
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(selectButton);

        // Back to menu button
        JButton backButton = new JButton("back to menu (M)");
        backButton.setFont(new Font("Arial", Font.BOLD, 20));
        backButton.addActionListener(e -> Main.showStartScreen(mainFrame));
        buttonPanel.add(backButton);

        add(buttonPanel, BorderLayout.SOUTH);

        loadPitchers(); // Load pitcher list
    }

    private void loadPitchers() {
        listModel.clear(); // Clear old data
        List<Pitcher> pitchers = dbManager.getAllPitchers();
        for (Pitcher p : pitchers) {
            listModel.addElement(p);
        }
        if (!pitchers.isEmpty()) {
            pitcherList.setSelectedIndex(0); // Select the first one by default
        }
    }
}