package client;

import common.Message;
import common.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main game interface with number grid and controls
 */
public class GameFrame extends JFrame {
    private final GameClient client;
    private final User currentUser;
    // flag to ensure start confirmation dialog is only shown once
    private boolean confirmationRequested = false;

    // Game state
    private int targetNumber;
    private boolean gameActive = false;
    private final Map<Integer, Integer> foundNumbers = new ConcurrentHashMap<>(); // number -> player color
    private final Map<Integer, String> players = new HashMap<>(); // player ID -> username
    private final Map<Integer, Integer> playerColors = new HashMap<>(); // player ID -> color
    private final Map<Integer, Point> numberPositions = new HashMap<>(); // number -> grid position
    private int powerupsAvailable = 0;

    // Track recently clicked numbers to prevent double clicks
    private final Map<Integer, Long> recentlyClicked = new ConcurrentHashMap<>(); // number -> click timestamp
    private static final int CLICK_DEBOUNCE_MS = 500; // Prevent double clicks within 500ms

    // UI Components
    private JPanel mainPanel;
    private JPanel gamePanel;
    private JPanel waitingPanel;
    private JPanel gridPanel;
    private JLabel statusLabel;
    private JLabel timeLabel;
    private JLabel targetLabel;
    private JLabel scoreLabel;
    private JButton startButton;
    private JButton priorityButton;
    private JButton blockButton;
    private JButton[][] numberButtons;

    // Game configuration
    private int gridSize = 100; // default
    private int rows = 10;
    private int cols = 10;
    private Timer gameTimer;
    private int remainingSeconds = 120; // default

    // Blocked numbers (from powerups)
    private final Map<Integer, Long> blockedNumbers = new ConcurrentHashMap<>(); // button index -> unblock time
    private Timer blockTimer;

    public GameFrame(GameClient client, User currentUser) {
        this.client = client;
        this.currentUser = currentUser;

        setTitle("Find the Number - " + currentUser.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        initializeComponents();

        // Initialize waiting panel state from GameClient
        int initialCount = client.getInitialPlayerCount();
        int maxCount = client.getMaxPlayers();
        if (initialCount > 0 && maxCount > 0) { // Check if we have valid data
            updateWaitingLabel(initialCount, maxCount);
            startButton.setEnabled(initialCount >= 2);
        } else {
            // Default if no initial data (should ideally not happen if login worked)
            updateWaitingLabel(0, 0); // Show 0/0 initially
            startButton.setEnabled(false);
        }

        // Start the block timer to check for unblocked numbers
        blockTimer = new Timer(100, e -> checkBlockedNumbers());
        blockTimer.start();
    }

    // Helper method to update the waiting label text
    private void updateWaitingLabel(int current, int max) {
        JLabel waitingLabel = (JLabel) ((BorderLayout) waitingPanel.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
        if (waitingLabel != null) {
            if (max > 0) {
                waitingLabel.setText("Waiting for players... (" + current + "/" + max + ")");
            } else {
                // Handle case where max players isn't known yet
                waitingLabel.setText("Waiting for players...");
            }
        }
        waitingPanel.revalidate();
        waitingPanel.repaint();
    }

    private void initializeComponents() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create game panel (active game)
        gamePanel = createGamePanel();

        // Create waiting panel
        waitingPanel = createWaitingPanel();

        // Initially show waiting panel
        mainPanel.add(waitingPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createGamePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Top panel with game info
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        // Game info panel (left side)
        JPanel gameInfoPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        gameInfoPanel.setBorder(new TitledBorder("Game Info"));

        statusLabel = new JLabel("Game Status: Waiting");
        timeLabel = new JLabel("Time: 00:00");
        targetLabel = new JLabel("Find Number: ?");
        scoreLabel = new JLabel("Your Score: 0");

        gameInfoPanel.add(statusLabel);
        gameInfoPanel.add(timeLabel);
        gameInfoPanel.add(targetLabel);
        gameInfoPanel.add(scoreLabel);

        // Power-up panel (right side)
        JPanel powerupPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        powerupPanel.setBorder(new TitledBorder("Power-ups"));

        priorityButton = new JButton("Priority (0)");
        blockButton = new JButton("Block Numbers (0)");

        priorityButton.setEnabled(false);
        blockButton.setEnabled(false);

        powerupPanel.add(priorityButton);
        powerupPanel.add(blockButton);

        topPanel.add(gameInfoPanel, BorderLayout.WEST);
        topPanel.add(powerupPanel, BorderLayout.EAST);

        // Center panel with grid
        gridPanel = new JPanel();
        // Grid will be created when the game starts

        // Add all panels to main game panel
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(gridPanel, BorderLayout.CENTER);

        // Set up action listeners
        priorityButton.addActionListener(e -> usePowerup("PRIORITY"));
        blockButton.addActionListener(e -> usePowerup("BLOCK_NUMBERS"));

        return panel;
    }

    private JPanel createWaitingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel waitingLabel = new JLabel("Waiting for players...", SwingConstants.CENTER);
        waitingLabel.setFont(new Font("Arial", Font.BOLD, 24));

        startButton = new JButton("Start Game");
        startButton.setEnabled(false);
        startButton.addActionListener(e -> {
            if (!confirmationRequested) {
                int choice = JOptionPane.showConfirmDialog(
                        GameFrame.this,
                        "Are you ready to start the game?",
                        "Start Game Confirmation",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    confirmationRequested = true;
                    startButton.setText("Waiting for game to start...");
                    startButton.setEnabled(false);
                    client.sendStartGame();
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(startButton);

        panel.add(waitingLabel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    public void showWaitingScreen() {
        SwingUtilities.invokeLater(() -> {
            // Reset confirmation flag when showing waiting screen again
            confirmationRequested = false;
            mainPanel.removeAll();
            mainPanel.add(waitingPanel, BorderLayout.CENTER);
            mainPanel.revalidate();
            mainPanel.repaint();
        });
    }

    public void updateWaitingStatus(Message message) {
        int currentPlayers = message.getInt("currentPlayers");
        int maxPlayers = message.getInt("maxPlayers");

        SwingUtilities.invokeLater(() -> {
            // Add new player to the list
            int playerId = message.getInt("playerId");
            String username = message.getString("username");
            int color = message.getInt("color");

            players.put(playerId, username);
            playerColors.put(playerId, color);

            // Enable start button if we have at least 2 players
            startButton.setEnabled(currentPlayers >= 2);

            JLabel waitingLabel = (JLabel) ((BorderLayout) waitingPanel.getLayout())
                    .getLayoutComponent(BorderLayout.CENTER);
            waitingLabel.setText("Waiting for players... (" + currentPlayers + "/" + maxPlayers + ")");
            waitingPanel.revalidate();
            waitingPanel.repaint();
        });
    }

    public void startGame(Message message) {
        // Extract game configuration
        gridSize = message.getInt("gridSize");
        remainingSeconds = message.getInt("duration");
        targetNumber = message.getInt("targetNumber");

        // Initialize powerups for the player (default is 3 of each type)
        // Check if initialPowerups exists in the message, otherwise default to 3
        try {
            powerupsAvailable = message.getInt("initialPowerups");
        } catch (Exception e) {
            // If initialPowerups is not in the message or there's a casting error
            powerupsAvailable = 3;
        }

        // Set grid dimensions
        rows = (int) Math.sqrt(gridSize);
        cols = gridSize / rows;
        if (rows * cols < gridSize)
            cols++; // Adjust if not perfect square

        // Process player information
        @SuppressWarnings("unchecked") // Added to suppress warning for casting message data
        List<Map<String, Object>> playerInfo = (List<Map<String, Object>>) message.get("players");
        for (Map<String, Object> player : playerInfo) {
            int playerId = (int) player.get("id");
            String username = (String) player.get("username");
            int color = (int) player.get("color");

            players.put(playerId, username);
            playerColors.put(playerId, color);
        }

        SwingUtilities.invokeLater(() -> {
            // Switch to game panel
            mainPanel.removeAll();
            mainPanel.add(gamePanel, BorderLayout.CENTER);

            // Create grid using the start game message
            createNumberGrid(rows, cols, message);

            // Update UI
            statusLabel.setText("Game Status: Active");
            targetLabel.setText("Find Number: " + targetNumber);
            formatTimeLabel(remainingSeconds);

            // Update powerup buttons with initial count
            updatePowerups(powerupsAvailable);

            mainPanel.revalidate();
            mainPanel.repaint();

            // Set game as active
            gameActive = true;

            // Start timer
            startGameTimer();
        });
    }

    private void createNumberGrid(int rows, int cols, Message message) {
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(rows, cols, 2, 2));

        numberButtons = new JButton[rows][cols];

        // Get the shuffled numbers from the server's message
        @SuppressWarnings("unchecked")
        List<Integer> numbers = (List<Integer>) message.get("shuffledNumbers");

        int numberIndex = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (numberIndex < numbers.size()) {
                    final int number = numbers.get(numberIndex++);
                    JButton button = new JButton(String.valueOf(number));
                    button.setFont(new Font("Arial", Font.BOLD, 14));
                    button.setPreferredSize(new Dimension(60, 60));
                    button.addActionListener(e -> onNumberClick(number));

                    numberButtons[i][j] = button;
                    gridPanel.add(button);

                    // Store the position of the number
                    numberPositions.put(number, new Point(i, j));
                } else {
                    // Add empty button if grid isn't a perfect square
                    JButton emptyButton = new JButton("");
                    emptyButton.setEnabled(false);
                    gridPanel.add(emptyButton);
                }
            }
        }
    }

    private void onNumberClick(int number) {
        if (!gameActive)
            return;

        // Check if the number is blocked
        int buttonIndex = number - 1;
        if (blockedNumbers.containsKey(buttonIndex) &&
                blockedNumbers.get(buttonIndex) > System.currentTimeMillis()) {
            return; // Number is still blocked
        }

        // Adding a debounce mechanism to prevent double clicks from sending multiple
        // requests
        long currentTime = System.currentTimeMillis();
        if (recentlyClicked.containsKey(number) &&
                (currentTime - recentlyClicked.get(number)) < CLICK_DEBOUNCE_MS) {
            return; // Click ignored due to debounce
        }
        recentlyClicked.put(number, currentTime);

        // Send number found message to server
        client.sendNumberFound(number);
    }

    private void startGameTimer() {
        if (gameTimer != null) {
            gameTimer.stop();
        }

        gameTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remainingSeconds--;
                if (remainingSeconds <= 0) {
                    remainingSeconds = 0;
                    gameTimer.stop();
                }
                formatTimeLabel(remainingSeconds);
            }
        });

        gameTimer.start();
    }

    private void formatTimeLabel(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        timeLabel.setText(String.format("Time: %02d:%02d", minutes, secs));
    }

    public void handleNumberFound(Message message) {
        int number = message.getInt("number");
        int playerId = message.getInt("playerId");
        targetNumber = message.getInt("nextTarget");

        // Record the found number
        foundNumbers.put(number, playerColors.get(playerId));

        SwingUtilities.invokeLater(() -> {
            // Update UI
            targetLabel.setText("Find Number: " + targetNumber);

            // Get the correct button position from our stored positions
            Point pos = numberPositions.get(number);
            if (pos != null) {
                JButton button = numberButtons[pos.x][pos.y];
                // Create an opaque color from the RGB value
                Color playerColor = new Color(playerColors.get(playerId), true);
                button.setBackground(playerColor);
                button.setEnabled(false);

                // Update score if current player found the number
                if (playerId == currentUser.getId()) {
                    int currentScore = Integer.parseInt(
                            scoreLabel.getText().substring(scoreLabel.getText().lastIndexOf(" ") + 1));
                    scoreLabel.setText("Your Score: " + (currentScore + 1));
                }
            }
        });
    }

    public void handleIncorrectNumber(int number) {
        // Visual feedback for incorrect number
        SwingUtilities.invokeLater(() -> {
            Point pos = numberPositions.get(number);
            if (pos != null) {
                final JButton button = numberButtons[pos.x][pos.y];
                final Color originalColor = button.getBackground();

                button.setBackground(Color.ORANGE);

                // Reset color after a brief delay
                new Timer(300, e -> {
                    button.setBackground(originalColor);
                    ((Timer) e.getSource()).stop();
                }).start();
            }
        });
    }

    public void updateTime(int remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
        SwingUtilities.invokeLater(() -> formatTimeLabel(remainingSeconds));
    }

    public void updatePowerups(int count) {
        powerupsAvailable = count;
        SwingUtilities.invokeLater(() -> {
            priorityButton.setText("Priority (" + powerupsAvailable + ")");
            blockButton.setText("Block Numbers (" + powerupsAvailable + ")");

            priorityButton.setEnabled(powerupsAvailable > 0);
            blockButton.setEnabled(powerupsAvailable > 0);
        });
    }

    private void usePowerup(String powerupType) {
        if (powerupsAvailable <= 0 || !gameActive) {
            return;
        }

        // Send powerup usage to server
        client.usePowerup(powerupType);

        // Update UI (will be confirmed via server message)
        powerupsAvailable--;
        SwingUtilities.invokeLater(() -> {
            priorityButton.setText("Priority (" + powerupsAvailable + ")");
            blockButton.setText("Block Numbers (" + powerupsAvailable + ")");

            priorityButton.setEnabled(powerupsAvailable > 0);
            blockButton.setEnabled(powerupsAvailable > 0);
        });
    }

    public void handlePowerupEffect(Message message) {
        String type = message.getString("type");
        int playerId = message.getInt("playerId");
        int durationMs = message.getInt("durationMs");

        if ("BLOCK_NUMBERS".equals(type) && playerId != currentUser.getId()) {
            SwingUtilities.invokeLater(() -> {
                long endTime = System.currentTimeMillis() + durationMs;
                java.util.List<Integer> availableNumbers = new java.util.ArrayList<>();
                for (int num = 1; num <= gridSize; num++) {
                    if (!foundNumbers.containsKey(num)) {
                        availableNumbers.add(num);
                    }
                }
                // Block all available numbers
                for (Integer number : availableNumbers) {
                    blockedNumbers.put(number - 1, endTime);
                    Point pos = numberPositions.get(number);
                    if (pos != null) {
                        JButton button = numberButtons[pos.x][pos.y];
                        button.setVisible(false);
                    }
                }

                statusLabel.setText("Game Status: All Numbers Blocked!");
                // Ensure UI updates reflect hidden buttons
                gridPanel.revalidate();
                gridPanel.repaint();

                new Timer(durationMs, e -> {
                    statusLabel.setText("Game Status: Active");
                    ((Timer) e.getSource()).stop();
                }).start();
            });
        } else if ("PRIORITY".equals(type) && playerId != currentUser.getId()) {
            // Other player has priority
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Game Status: " + players.get(playerId) + " has priority!");

                // Reset status after the duration
                new Timer(durationMs, e -> {
                    statusLabel.setText("Game Status: Active");
                    ((Timer) e.getSource()).stop();
                }).start();
            });
        } else if (playerId == currentUser.getId()) {
            // Our powerup was activated
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Game Status: Your power-up activated!");

                // Reset status after the duration
                new Timer(durationMs, e -> {
                    statusLabel.setText("Game Status: Active");
                    ((Timer) e.getSource()).stop();
                }).start();
            });
        }
    }

    private void checkBlockedNumbers() {
        long now = System.currentTimeMillis();
        boolean update = false;

        for (Map.Entry<Integer, Long> entry : blockedNumbers.entrySet()) {
            if (entry.getValue() < now) {
                int number = entry.getKey() + 1;
                blockedNumbers.remove(entry.getKey());
                Point pos = numberPositions.get(number);
                if (pos != null) {
                    JButton button = numberButtons[pos.x][pos.y];
                    // Restore the button visibility and text
                    button.setVisible(true);
                    button.setText(String.valueOf(number));
                }
                update = true;
            }
        }

        if (update) {
            gridPanel.revalidate();
            gridPanel.repaint();
        }
    }

    public void handleGameOver(Message message) {
        // Stop game
        gameActive = false;
        if (gameTimer != null) {
            gameTimer.stop();
        }

        // Get game results
        int winnerId = message.getInt("winnerId");
        @SuppressWarnings("unchecked") // Added to suppress warning for casting message data
        Map<Integer, Integer> scores = (Map<Integer, Integer>) message.get("scores");

        SwingUtilities.invokeLater(() -> {
            // Update UI
            statusLabel.setText("Game Status: Game Over");

            // Show game over dialog
            StringBuilder result = new StringBuilder(); // Define result here
            result.append("Game Over!\\n\\n");
            result.append("Results:\\n");

            // Restore the loop to iterate through players and scores
            for (Map.Entry<Integer, String> player : players.entrySet()) {
                int playerId = player.getKey();
                String username = player.getValue();
                int score = scores.getOrDefault(playerId, 0);

                result.append(username).append(": ").append(score).append(" points");
                if (playerId == winnerId) {
                    result.append(" (Winner!)");
                }
                result.append("\\n");
            } // End of the restored loop

            JOptionPane.showMessageDialog(this, result.toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);

            // Disable the grid
            for (int i = 0; i < numberButtons.length; i++) {
                for (int j = 0; j < numberButtons[i].length; j++) {
                    if (numberButtons[i][j] != null) {
                        numberButtons[i][j].setEnabled(false);
                    }
                }
            }
        });
    }
}