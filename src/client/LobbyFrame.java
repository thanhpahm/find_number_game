package client;

import common.User;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class LobbyFrame extends JFrame {
    private final GameClient client;
    private final User currentUser;
    private JButton findGameButton;
    private JButton userInfoButton;
    private JTable leaderboardTable;
    private DefaultTableModel leaderboardModel;

    public LobbyFrame(GameClient client, User currentUser) {
        this.client = client;
        this.currentUser = currentUser;

        setTitle("Find the Number - Lobby");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        initializeComponents();
    }

    private void initializeComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Welcome panel at the top
        JPanel welcomePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getUsername() + "!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        welcomePanel.add(welcomeLabel);

        // Button panel on the left
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(new EmptyBorder(0, 0, 0, 20));

        findGameButton = new JButton("Find Game");
        userInfoButton = new JButton("User Info");

        // Set preferred size for buttons
        Dimension buttonSize = new Dimension(150, 40);
        findGameButton.setPreferredSize(buttonSize);
        userInfoButton.setPreferredSize(buttonSize);
        findGameButton.setMaximumSize(buttonSize);
        userInfoButton.setMaximumSize(buttonSize);

        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add(findGameButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(userInfoButton);
        buttonPanel.add(Box.createVerticalGlue());

        // Leaderboard panel on the right
        JPanel leaderboardPanel = new JPanel(new BorderLayout());
        leaderboardPanel.setBorder(BorderFactory.createTitledBorder("Leaderboard"));

        String[] columnNames = { "Player", "W/L Ratio", "Score" };
        leaderboardModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        leaderboardTable = new JTable(leaderboardModel);
        JScrollPane scrollPane = new JScrollPane(leaderboardTable);
        leaderboardPanel.add(scrollPane, BorderLayout.CENTER);

        // Add components to main panel
        mainPanel.add(welcomePanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.WEST);
        mainPanel.add(leaderboardPanel, BorderLayout.CENTER);

        add(mainPanel);

        // Set up action listeners
        setupListeners();
    }

    private void setupListeners() {
        findGameButton.addActionListener(e -> startFindingGame());
        userInfoButton.addActionListener(e -> showUserInfo());
    }

    private void startFindingGame() {
        findGameButton.setEnabled(false);
        findGameButton.setText("Finding game...");

        // Send FIND_GAME message to server instead of creating GameFrame directly
        client.sendFindGame();

        // Game frame will be created by GameClient when server confirms the match
    }

    private void showUserInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Username: ").append(currentUser.getUsername()).append("\n");
        info.append("Games Won: ").append(currentUser.getGamesWon()).append("\n");
        info.append("Games Lost: ").append(currentUser.getGamesLost()).append("\n");
        info.append("Total Score: ").append(currentUser.getTotalScore()).append("\n");
        info.append("Win Rate: ").append(String.format("%.2f", currentUser.getWinRate())).append("\n");

        if (currentUser.getEmail() != null) {
            info.append("Email: ").append(currentUser.getEmail()).append("\n");
        }

        JOptionPane.showMessageDialog(this, info.toString(), "User Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public void updateLeaderboard(List<User> leaderboard) {
        SwingUtilities.invokeLater(() -> {
            // Clear current table
            while (leaderboardModel.getRowCount() > 0) {
                leaderboardModel.removeRow(0);
            }

            // Add users to leaderboard
            for (User user : leaderboard) {
                String winRatio = String.format("%.2f", user.getWinRate());
                Object[] row = { user.getUsername(), winRatio, user.getTotalScore() };
                leaderboardModel.addRow(row);
            }
        });
    }

    public void resetFindGameButton() {
        SwingUtilities.invokeLater(() -> {
            findGameButton.setEnabled(true);
            findGameButton.setText("Find Game");
        });
    }
}