package client.game;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import shared.ScoreTracker.PlayerStats;

public class PlayerProfileFrame extends JFrame {
    private final GameClient client;
    private final String playerName;
    private JPanel statsPanel;
    private JPanel achievementsPanel;
    private JPanel recentGamesPanel;
    private JLabel rankLabel;
    private JProgressBar nextRankProgress;

    public PlayerProfileFrame(GameClient client, String playerName) {
        this.client = client;
        this.playerName = playerName;

        setTitle(playerName + "'s Profile");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initializeUI();
        loadPlayerData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        // Header panel with player info
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left side - Stats and Recent Games
        JPanel leftPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        statsPanel = createStatsPanel();
        recentGamesPanel = createRecentGamesPanel();
        leftPanel.add(statsPanel);
        leftPanel.add(recentGamesPanel);

        // Right side - Achievements
        achievementsPanel = createAchievementsPanel();

        mainPanel.add(leftPanel);
        mainPanel.add(achievementsPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(10, 10, 10, 10),
                new LineBorder(Color.GRAY)));

        // Player name and rank
        JPanel nameRankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nameRankPanel.add(new JLabel(playerName, SwingConstants.LEFT));
        rankLabel = new JLabel("Rank: Loading...");
        nameRankPanel.add(rankLabel);

        // Progress to next rank
        JPanel progressPanel = new JPanel(new BorderLayout(5, 0));
        progressPanel.add(new JLabel("Progress to next rank:"), BorderLayout.WEST);
        nextRankProgress = new JProgressBar(0, 100);
        nextRankProgress.setStringPainted(true);
        progressPanel.add(nextRankProgress, BorderLayout.CENTER);

        panel.add(nameRankPanel, BorderLayout.NORTH);
        panel.add(progressPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Statistics"));
        panel.setLayout(new GridLayout(0, 2, 5, 5));

        // These will be populated when data is loaded
        panel.add(new JLabel("Games Played:"));
        panel.add(new JLabel("Loading..."));
        panel.add(new JLabel("Win Rate:"));
        panel.add(new JLabel("Loading..."));
        panel.add(new JLabel("Total Score:"));
        panel.add(new JLabel("Loading..."));
        panel.add(new JLabel("Lucky Numbers Found:"));
        panel.add(new JLabel("Loading..."));
        panel.add(new JLabel("Best Time:"));
        panel.add(new JLabel("Loading..."));
        panel.add(new JLabel("Average Score:"));
        panel.add(new JLabel("Loading..."));

        return panel;
    }

    private JPanel createAchievementsPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Achievements"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Will be populated when data is loaded
        panel.add(new JLabel("Loading achievements..."));

        return panel;
    }

    private JPanel createRecentGamesPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Recent Games"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Will be populated when data is loaded
        panel.add(new JLabel("Loading recent games..."));

        return panel;
    }

    private void loadPlayerData() {
        // Request player stats from server
        client.requestPlayerStats(playerName);
    }

    public void updateProfile(Map<String, Object> data) {
        PlayerStats stats = (PlayerStats) data.get("stats");
        Set<String> achievements = (Set<String>) data.get("achievements");
        java.util.List<Map<String, Object>> recentGames = (java.util.List<Map<String, Object>>) data.get("recentGames");
        int rank = (Integer) data.get("rank");
        double rankProgress = (Double) data.get("rankProgress");

        SwingUtilities.invokeLater(() -> {
            updateStats(stats);
            updateAchievements(achievements);
            updateRecentGames(recentGames);
            updateRank(rank, rankProgress);
        });
    }

    private void updateStats(PlayerStats stats) {
        // Clear and repopulate stats panel
        statsPanel.removeAll();
        statsPanel.setLayout(new GridLayout(0, 2, 5, 5));

        addStatRow("Games Played:", String.valueOf(stats.getGamesPlayed()));
        addStatRow("Win Rate:", String.format("%.1f%%", stats.getWinRate() * 100));
        addStatRow("Total Score:", String.valueOf(stats.getScore()));
        addStatRow("Lucky Numbers Found:", String.valueOf(stats.getLuckyNumbersFound()));
        addStatRow("Best Time:", formatTime(stats.getBestTime()));
        addStatRow("Average Score:", String.format("%.1f", stats.getAverageScore()));

        statsPanel.revalidate();
        statsPanel.repaint();
    }

    private void addStatRow(String label, String value) {
        statsPanel.add(new JLabel(label));
        statsPanel.add(new JLabel(value));
    }

    private void updateAchievements(Set<String> achievements) {
        achievementsPanel.removeAll();
        achievementsPanel.setLayout(new BoxLayout(achievementsPanel, BoxLayout.Y_AXIS));

        if (achievements.isEmpty()) {
            achievementsPanel.add(new JLabel("No achievements yet"));
        } else {
            for (String achievement : achievements) {
                JPanel achievementPanel = new JPanel(new BorderLayout());
                achievementPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                JLabel iconLabel = new JLabel("🏆");
                iconLabel.setFont(new Font("Dialog", Font.PLAIN, 20));
                achievementPanel.add(iconLabel, BorderLayout.WEST);

                JLabel achievementLabel = new JLabel(achievement);
                achievementPanel.add(achievementLabel, BorderLayout.CENTER);

                achievementsPanel.add(achievementPanel);
            }
        }

        achievementsPanel.revalidate();
        achievementsPanel.repaint();
    }

    private void updateRecentGames(List<Map<String, Object>> recentGames) {
        recentGamesPanel.removeAll();
        recentGamesPanel.setLayout(new BoxLayout(recentGamesPanel, BoxLayout.Y_AXIS));

        if (recentGames.isEmpty()) {
            recentGamesPanel.add(new JLabel("No recent games"));
        } else {
            for (Map<String, Object> game : recentGames) {
                JPanel gamePanel = createGamePanel(game);
                recentGamesPanel.add(gamePanel);
            }
        }

        recentGamesPanel.revalidate();
        recentGamesPanel.repaint();
    }

    private JPanel createGamePanel(Map<String, Object> game) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String result = (Boolean) game.get("won") ? "Won" : "Lost";
        int score = (Integer) game.get("score");
        String date = (String) game.get("date");

        panel.add(new JLabel(date), BorderLayout.WEST);
        panel.add(new JLabel(result), BorderLayout.CENTER);
        panel.add(new JLabel("Score: " + score), BorderLayout.EAST);

        return panel;
    }

    private void updateRank(int rank, double progress) {
        rankLabel.setText("Rank: " + rank);
        nextRankProgress.setValue((int) (progress * 100));
        nextRankProgress.setString(String.format("%.1f%%", progress * 100));
    }

    private String formatTime(long milliseconds) {
        if (milliseconds == Long.MAX_VALUE) {
            return "N/A";
        }
        long seconds = milliseconds / 1000;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
