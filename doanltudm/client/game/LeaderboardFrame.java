package client.game;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.Map;


public class LeaderboardFrame extends JFrame {
    private final GameClient client;
    private JTable leaderboardTable;
    private JPanel achievementsPanel;
    private JComboBox<String> timeFilterComboBox;
    private Timer refreshTimer;
    private JLabel totalGamesLabel;
    private JLabel activePlayersLabel;
    private JLabel avgDurationLabel;


    public LeaderboardFrame(GameClient client) {
        this.client = client;

        setTitle("Game Leaderboard");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initializeUI();
        setupRefreshTimer();
        loadLeaderboardData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        add(createControlPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        timeFilterComboBox = new JComboBox<>(new String[] { "All Time", "This Month", "This Week", "Today" });
        timeFilterComboBox.addActionListener(e -> loadLeaderboardData());
        panel.add(new JLabel("Show:"));
        panel.add(timeFilterComboBox);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadLeaderboardData());
        panel.add(refreshButton);

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left side - Leaderboard table
        panel.add(createLeaderboardPanel());

        // Right side - Statistics and achievements
        panel.add(createStatsPanel());

        return panel;
    }

    private JPanel createLeaderboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Top Players"));

        String[] columnNames = { "Rank", "Player", "Score", "Win Rate", "Games Won", "Lucky Numbers" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        leaderboardTable = new JTable(model);
        leaderboardTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leaderboardTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = leaderboardTable.getSelectedRow();
                if (row != -1) {
                    String playerName = (String) leaderboardTable.getValueAt(row, 1);
                    showPlayerProfile(playerName);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(leaderboardTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel statsPanel = new JPanel();
        statsPanel.setBorder(BorderFactory.createTitledBorder("Global Statistics"));

        // These will be populated when data is loaded
        statsPanel.add(new JLabel("Total Games Played:"));
        totalGamesLabel = new JLabel("0");
        statsPanel.add(totalGamesLabel);

        statsPanel.add(new JLabel("Active Players:"));
        activePlayersLabel = new JLabel("0");
        statsPanel.add(activePlayersLabel);

        statsPanel.add(new JLabel("Average Game Duration:"));
        avgDurationLabel = new JLabel("0:00");
        statsPanel.add(avgDurationLabel);

        panel.add(statsPanel, BorderLayout.NORTH);
        statsPanel.add(new JLabel("Average Game Duration:"));
        statsPanel.add(new JLabel("0:00"));

        panel.add(statsPanel, BorderLayout.NORTH);

        // Achievements list in the center
        achievementsPanel = new JPanel();
        achievementsPanel.setLayout(new BoxLayout(achievementsPanel, BoxLayout.Y_AXIS));
        achievementsPanel.setBorder(BorderFactory.createTitledBorder("Recent Achievements"));

        JScrollPane achievementsScroll = new JScrollPane(achievementsPanel);
        achievementsScroll.setPreferredSize(new Dimension(300, 200));
        panel.add(achievementsScroll, BorderLayout.CENTER);

        return panel;
    }

    private void setupRefreshTimer() {
        refreshTimer = new Timer(30000, e -> loadLeaderboardData()); // Refresh every 30 seconds
        refreshTimer.start();
    }

    private void loadLeaderboardData() {
        client.requestLeaderboard();
    }

    public void updateLeaderboard(List<Map<String, Object>> leaderboardData) {
        SwingUtilities.invokeLater(() -> {
            DefaultTableModel model = (DefaultTableModel) leaderboardTable.getModel();
            model.setRowCount(0);

            int rank = 1;
            for (Map<String, Object> entry : leaderboardData) {
                model.addRow(new Object[] {
                    rank++,
                    entry.get("username"),
                    entry.get("score"),
                    entry.get("winRate"),
                    entry.get("gamesWon"),
                    entry.get("luckyNumbers")
                });
            }
        });
    }

    public void updateGlobalStats(Map<String, Object> stats) {
        SwingUtilities.invokeLater(() -> {
            if (totalGamesLabel != null) {
                totalGamesLabel.setText(stats.get("totalGames").toString());
            }
            if (activePlayersLabel != null) {
                activePlayersLabel.setText(stats.get("activePlayers").toString());
            }
            if (avgDurationLabel != null) {
                avgDurationLabel.setText(formatDuration((Long) stats.get("avgGameDuration")));
            }
        });
    }

    // The updateStatLabel method is no longer needed and can be removed.

    public void updateRecentAchievements(List<Map<String, Object>> achievements) {
        SwingUtilities.invokeLater(() -> {
            achievementsPanel.removeAll();

            for (Map<String, Object> achievement : achievements) {
                JPanel achievementPanel = new JPanel(new BorderLayout());
                achievementPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                String playerName = (String) achievement.get("player");
                String achievementName = (String) achievement.get("name");
                String timeAgo = (String) achievement.get("timeAgo");

                JLabel nameLabel = new JLabel(playerName + " earned " + achievementName);
                JLabel timeLabel = new JLabel(timeAgo);
                timeLabel.setFont(timeLabel.getFont().deriveFont(Font.ITALIC));

                achievementPanel.add(nameLabel, BorderLayout.CENTER);
                achievementPanel.add(timeLabel, BorderLayout.EAST);

                achievementsPanel.add(achievementPanel);
            }

            achievementsPanel.revalidate();
            achievementsPanel.repaint();
        });
    }

    private void showPlayerProfile(String playerName) {
        PlayerProfileFrame profileFrame = new PlayerProfileFrame(client, playerName);
        profileFrame.setVisible(true);
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }
}
