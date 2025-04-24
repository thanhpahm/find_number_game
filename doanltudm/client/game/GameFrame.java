package client.game;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import shared.GameState;
import shared.GameFeatures;

public class GameFrame extends JFrame implements GameFeatures.GameView {
    private static final int GRID_SIZE = 100;
    private static final int ROWS = 10;
    private static final int COLS = 10;

    private final GameClient client;
    private final Map<Integer, JButton> numberButtons;
    private final Map<String, JLabel> playerLabels;
    private JPanel gamePanel;
    private JLabel targetLabel;
    private JLabel timerLabel;
    private JPanel scorePanel;
    private Timer gameTimer;
    private Set<Integer> blockedNumbers;
    private boolean isPriorityMode;

    public GameFrame(GameClient client) {
        this.client = client;
        this.numberButtons = new HashMap<>();
        this.playerLabels = new HashMap<>();
        this.blockedNumbers = new HashSet<>();
        this.isPriorityMode = false;

        setupFrame();
        initializeComponents();
        client.setGameFrame(this);
    }

    private void setupFrame() {
        setTitle("Find Number Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        // Game info panel (top)
        JPanel infoPanel = new JPanel(new BorderLayout(10, 0));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        targetLabel = new JLabel("Find: ?", SwingConstants.CENTER);
        targetLabel.setFont(new Font("Arial", Font.BOLD, 24));
        infoPanel.add(targetLabel, BorderLayout.CENTER);

        timerLabel = new JLabel("Time: 2:00", SwingConstants.RIGHT);
        timerLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        infoPanel.add(timerLabel, BorderLayout.EAST);

        add(infoPanel, BorderLayout.NORTH);

        // Game grid (center)
        gamePanel = createGameGrid();
        JScrollPane scrollPane = new JScrollPane(gamePanel);
        add(scrollPane, BorderLayout.CENTER);

        // Score panel (right)
        scorePanel = new JPanel();
        scorePanel.setLayout(new BoxLayout(scorePanel, BoxLayout.Y_AXIS));
        scorePanel.setBorder(BorderFactory.createTitledBorder("Scores"));
        scorePanel.setPreferredSize(new Dimension(150, 0));
        add(scorePanel, BorderLayout.EAST);

        // Power-ups panel (bottom)
        JPanel powerUpsPanel = createPowerUpsPanel();
        add(powerUpsPanel, BorderLayout.SOUTH);
    }

    private JPanel createGameGrid() {
        JPanel panel = new JPanel(new GridLayout(ROWS, COLS, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        for (int i = 1; i <= GRID_SIZE; i++) {
            JButton button = createNumberButton(i);
            numberButtons.put(i, button);
            panel.add(button);
        }

        return panel;
    }

    private JButton createNumberButton(int number) {
        JButton button = new JButton(String.valueOf(number));
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.addActionListener(e -> handleNumberClick(number));
        return button;
    }

    private JPanel createPowerUpsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Power-ups"));

        JButton priorityButton = new JButton("Priority Mode");
        priorityButton.addActionListener(e -> activatePriorityMode());
        panel.add(priorityButton);

        JButton blockButton = new JButton("Block Numbers");
        blockButton.addActionListener(e -> activateBlockNumbers());
        panel.add(blockButton);

        return panel;
    }

    private void handleNumberClick(int number) {
        if (!isPriorityMode && isBlocked(number)) {
            showMessage("This number is blocked!");
            return;
        }

        client.sendNumberClick(number);
    }

    public void startGame(GameState gameState) {
        SwingUtilities.invokeLater(() -> {
            resetGame();
            updateGameState(gameState);
            startTimer();
        });
    }

    private void resetGame() {
        numberButtons.values().forEach(button -> {
            button.setEnabled(true);
            button.setBackground(null);
        });
        blockedNumbers.clear();
        isPriorityMode = false;
    }

    public void updateGameState(GameState state) {
        SwingUtilities.invokeLater(() -> {
            setTargetNumber(state.getCurrentNumber());
            updateScores(state.getPlayerScores());
            updateFoundNumbers(state.getFoundNumbers());
            updateTimeRemaining(state.getTimeRemaining());
        });
    }

    public void setTargetNumber(int number) {
        targetLabel.setText("Find: " + number);
    }

    public void updateNumber(int number, String playerName) {
        JButton button = numberButtons.get(number);
        if (button != null) {
            button.setEnabled(false);
            button.setBackground(getPlayerColor(playerName));
            if (GameFeatures.isLuckyNumber(number)) {
                button.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2));
            }
        }
    }

    private void updateScores(Map<String, Integer> scores) {
        scorePanel.removeAll();
        scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    JLabel label = playerLabels.computeIfAbsent(entry.getKey(), k -> new JLabel());
                    label.setText(entry.getKey() + ": " + entry.getValue());
                    scorePanel.add(label);
                });
        scorePanel.revalidate();
        scorePanel.repaint();
    }

    private void updateFoundNumbers(Map<Integer, String> foundNumbers) {
        foundNumbers.forEach((number, playerName) -> {
            JButton button = numberButtons.get(number);
            if (button != null && button.isEnabled()) {
                updateNumber(number, playerName);
            }
        });
    }

    private void startTimer() {
        if (gameTimer != null) {
            gameTimer.stop();
        }

        gameTimer = new Timer(1000, e -> {
            String timeText = timerLabel.getText();
            int seconds = parseTimeToSeconds(timeText);
            if (seconds > 0) {
                seconds--;
                updateTimer(seconds);
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        gameTimer.start();
    }

    public void updateTimer(int seconds) {
        timerLabel.setText(String.format("Time: %d:%02d", seconds / 60, seconds % 60));
    }

    private int parseTimeToSeconds(String timeText) {
        String[] parts = timeText.substring(6).split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public void showGameResults(Map<String, Object> results) {
        String winner = (String) results.get("winner");
        Map<String, Integer> finalScores = (Map<String, Integer>) results.get("scores");

        StringBuilder message = new StringBuilder();
        message.append("Game Over!\n\n");
        message.append("Winner: ").append(winner).append("\n\n");
        message.append("Final Scores:\n");
        finalScores.forEach((player, score) -> message.append(player).append(": ").append(score).append("\n"));

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message.toString(), "Game Results",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void hideOtherPlayersNumbers() {
        isPriorityMode = true;
        String currentPlayer = client.getCurrentUser().getUsername();
        numberButtons.values().forEach(button -> {
            if (button.isEnabled()) {
                button.setVisible(button.getBackground() == null ||
                        button.getBackground().equals(getPlayerColor(currentPlayer)));
            }
        });
    }

    @Override
    public void showAllNumbers() {
        isPriorityMode = false;
        numberButtons.values().forEach(button -> button.setVisible(true));
    }

    @Override
    public void blockNumbers(String playerId, int duration) {
        Random random = new Random();
        int numbersToBlock = (GRID_SIZE * GameFeatures.BLOCK_NUMBERS_PERCENTAGE) / 100;

        List<Integer> availableNumbers = numberButtons.entrySet().stream()
                .filter(e -> e.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .toList();

        blockedNumbers.clear();
        for (int i = 0; i < Math.min(numbersToBlock, availableNumbers.size()); i++) {
            int index = random.nextInt(availableNumbers.size());
            int number = availableNumbers.get(index);
            blockedNumbers.add(number);
            JButton button = numberButtons.get(number);
            button.setBackground(Color.GRAY);
        }

        Timer unblockTimer = new Timer(duration, e -> {
            unblockNumbers();
            ((Timer) e.getSource()).stop();
        });
        unblockTimer.setRepeats(false);
        unblockTimer.start();
    }

    @Override
    public void unblockNumbers() {
        blockedNumbers.forEach(number -> {
            JButton button = numberButtons.get(number);
            if (button != null && button.isEnabled()) {
                button.setBackground(null);
            }
        });
        blockedNumbers.clear();
    }

    private void activatePriorityMode() {
        if (!client.getCurrentUser().isPowerUpAvailable(PowerUpType.PRIORITY_MODE)) {
            showError("Priority Mode power-up not available");
            return;
        }

        client.usePowerUp(PowerUpType.PRIORITY_MODE);
        hideOtherPlayersNumbers();

        // Schedule to show all numbers after duration
        Timer priorityTimer = new Timer(GameFeatures.PRIORITY_MODE_DURATION, e -> {
            showAllNumbers();
            ((Timer) e.getSource()).stop();
        });
        priorityTimer.setRepeats(false);
        priorityTimer.start();
    }

    private void activateBlockNumbers() {
        if (!client.getCurrentUser().isPowerUpAvailable(PowerUpType.BLOCK_NUMBERS)) {
            showError("Block Numbers power-up not available");
            return;
        }

        client.usePowerUp(PowerUpType.BLOCK_NUMBERS);
        String currentPlayer = client.getCurrentUser().getUsername();
        blockNumbers(currentPlayer, GameFeatures.BLOCK_NUMBERS_DURATION);
    }

    private boolean isBlocked(int number) {
        return blockedNumbers.contains(number);
    }

    private Color getPlayerColor(String playerName) {
        Player player = client.getPlayers().get(playerName);
        return player != null ? player.getColor() : Color.GRAY;
    }

    public void showLuckyNumberBonus(int count) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Lucky Number Bonus!\nYou've found " + count + " lucky numbers!",
                    "Lucky Number", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public void showTimeWarning(int secondsLeft) {
        SwingUtilities.invokeLater(() -> {
            if (secondsLeft <= 10) {
                timerLabel.setForeground(Color.RED);
            }
            JOptionPane.showMessageDialog(this,
                    secondsLeft + " seconds remaining!",
                    "Time Warning", JOptionPane.WARNING_MESSAGE);
        });
    }

    public void showError(String error) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    public void handleDisconnection() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Lost connection to server!",
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        });
    }

    public void addPlayer(Player player) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = new JLabel(player.getUsername() + ": 0");
            playerLabels.put(player.getUsername(), label);
            scorePanel.add(label);
            scorePanel.revalidate();
            scorePanel.repaint();
        });
    }

    public void removePlayer(String playerName) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = playerLabels.remove(playerName);
            if (label != null) {
                scorePanel.remove(label);
                scorePanel.revalidate();
                scorePanel.repaint();
            }
        });
    }
}
