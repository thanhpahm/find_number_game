package client.game;

import javax.swing.*;
import java.awt.*;

public class ClientLobbyFrame extends JFrame {
    private String playerName;
    private JLabel playerLabel;
    private JButton findGameButton;
    private JButton leaderboardButton;
    private JButton profileButton;
    private GameClient gameClient;

    public ClientLobbyFrame(String playerName, GameClient gameClient) {
        this.playerName = playerName;
        this.gameClient = gameClient;

        setTitle("Game Lobby");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Player info panel (top-left)
        JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        playerLabel = new JLabel("Player: " + playerName);
        playerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        playerPanel.add(playerLabel);
        add(playerPanel, BorderLayout.NORTH);

        // Main buttons panel (center)
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Find Game Button
        findGameButton = createStyledButton("Find Game", 200, 50);
        findGameButton.addActionListener(e -> findGame());
        gbc.gridy = 0;
        mainPanel.add(findGameButton, gbc);

        // Leaderboard Button
        leaderboardButton = createStyledButton("Leaderboard", 200, 50);
        leaderboardButton.addActionListener(e -> showLeaderboard());
        gbc.gridy = 1;
        mainPanel.add(leaderboardButton, gbc);

        // Profile Button
        profileButton = createStyledButton("My Profile", 200, 50);
        profileButton.addActionListener(e -> showProfile());
        gbc.gridy = 2;
        mainPanel.add(profileButton, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text, int width, int height) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(width, height));
        button.setBackground(new Color(51, 153, 255));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setFont(new Font("Arial", Font.BOLD, 14));
        return button;
    }

    private void findGame() {
        findGameButton.setEnabled(false);
        findGameButton.setText("Finding Game...");
        System.out.println("Sending FIND_GAME request for player: " + playerName);
        gameClient.sendMessage("FIND_GAME");
    }

    public void handleMatchFound(String opponentName) {
        JOptionPane.showMessageDialog(this, "Match found! Playing against: " + opponentName);
        // Start game with opponent
        GameFrame gameFrame = new GameFrame();
        gameFrame.setVisible(true);
        this.dispose();
    }

    private void showLeaderboard() {
        new LeaderboardFrame(gameClient).setVisible(true);
    }

    private void showProfile() {
        new PlayerProfileFrame(playerName, gameClient).setVisible(true);
    }

    public void handleServerMessage(String command, Object data) {
        System.out.println("Lobby handling: " + command + " with data: " + data);

        SwingUtilities.invokeLater(() -> {
            try {
                switch (command) {
                    case "WAITING":
                        findGameButton.setText("Waiting for players...");
                        findGameButton.setEnabled(false);
                        break;

                    case "GAME_FOUND":
                        findGameButton.setText("Game found!");
                        String opponents = (String) data;
                        JOptionPane.showMessageDialog(this, "Found opponents: " + opponents);
                        break;

                    case "GAME_START":
                        Integer firstNumber = (Integer) data;
                        System.out.println("Starting game with number: " + firstNumber);
                        startGame(firstNumber);
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error handling message: " + e);
                e.printStackTrace();

                // Reset button state if there's an error
                findGameButton.setText("Find Game");
                findGameButton.setEnabled(true);
            }
        });
    }

    private void startGame(int firstNumber) {
        GameFrame gameFrame = new GameFrame();
        gameFrame.setGameClient(gameClient);
        // Add these lines to update the GameClient's frame references
        gameClient.setGameFrame(gameFrame);
        gameClient.setLobbyFrame(null); // Clear lobby frame reference
        gameFrame.setCurrentPlayer(playerName, new Color(51, 153, 255)); // Assign a default color or get from server
        gameFrame.updateCurrentNumber(firstNumber);
        gameFrame.setVisible(true);
        gameFrame.startGame();
        dispose(); // Dispose lobby frame after game frame is set up
    }
}
