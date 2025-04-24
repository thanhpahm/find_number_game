package server;

import common.Message;
import common.User;
import java.awt.Color;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages game state and logic
 */
public class Game {
    // Game configuration
    private static final int DEFAULT_GRID_SIZE = 100;
    private static final int DEFAULT_GAME_DURATION_SECONDS = 120; // 2 minutes
    private static final int[] PLAYER_COLORS = {
            Color.RED.getRGB(),
            Color.BLUE.getRGB(),
            Color.GREEN.getRGB()
    };

    private final int gameId;
    private final int gridSize;
    private final int gameDurationSeconds;
    private final Random random = new Random();

    // Game state
    private boolean isActive = false;
    private long startTime = 0;
    private int targetNumber = -1;
    private Map<Integer, Integer> foundNumbers = new ConcurrentHashMap<>(); // number -> player ID who found it
    private final Map<Integer, ClientHandler> players = new ConcurrentHashMap<>();
    private final AtomicInteger playerCount = new AtomicInteger(0);
    private final int maxPlayers;
    private final DatabaseManager dbManager;

    // Power-ups tracking
    private final Map<Integer, Long> priorityPowerupEndTime = new ConcurrentHashMap<>(); // userId -> end time
    private final Map<Integer, Integer> luckyNumberCounts = new ConcurrentHashMap<>(); // userId -> count

    public Game(int gameId, DatabaseManager dbManager) {
        this(gameId, DEFAULT_GRID_SIZE, DEFAULT_GAME_DURATION_SECONDS, 3, dbManager); // Default: 3 players max
    }

    public Game(int gameId, int gridSize, int gameDurationSeconds, int maxPlayers, DatabaseManager dbManager) {
        this.gameId = gameId;
        this.gridSize = gridSize;
        this.gameDurationSeconds = gameDurationSeconds;
        this.maxPlayers = maxPlayers;
        this.dbManager = dbManager;
    }

    public synchronized boolean addPlayer(ClientHandler client) {
        if (players.size() >= maxPlayers || isActive) {
            return false;
        }

        int playerId = client.getUser().getId();
        players.put(playerId, client);

        // Assign a color to the player
        int colorIndex = players.size() - 1;
        if (colorIndex < PLAYER_COLORS.length) {
            client.getUser().setPlayerColor(PLAYER_COLORS[colorIndex]);
        } else {
            // Generate a random color if we've run out of predefined colors
            client.getUser()
                    .setPlayerColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)).getRGB());
        }

        // Initialize player tracking
        luckyNumberCounts.put(playerId, 0);

        // Check if we have enough players to start
        if (players.size() >= 2) {
            // We have at least 2 players, can start the game
            notifyPlayersOfJoin(client.getUser());
        }

        return true;
    }

    public synchronized void removePlayer(int playerId) {
        players.remove(playerId);
        if (players.isEmpty() && isActive) {
            endGame();
        }
    }

    public synchronized void startGame() {
        if (players.size() < 2) {
            // Need at least 2 players
            return;
        }

        isActive = true;
        startTime = System.currentTimeMillis();

        // Generate the grid (1 to gridSize)
        List<Integer> numbers = new ArrayList<>(gridSize);
        for (int i = 1; i <= gridSize; i++) {
            numbers.add(i);
        }

        // Pick the first target number
        generateNextTarget();

        // Send start game message to all players
        Message startMessage = new Message(Message.START_GAME);
        startMessage.put("gridSize", gridSize);
        startMessage.put("duration", gameDurationSeconds);
        startMessage.put("targetNumber", targetNumber);
        startMessage.put("players", getPlayerInfo());

        broadcastToAllPlayers(startMessage);

        // Start game timer
        new Thread(this::gameTimerTask).start();
    }

    private void gameTimerTask() {
        long endTime = startTime + (gameDurationSeconds * 1000);

        while (isActive && System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(1000); // Check every second

                // Send time update every 5 seconds
                long timeRemaining = (endTime - System.currentTimeMillis()) / 1000;
                if (timeRemaining % 5 == 0 && timeRemaining > 0) {
                    Message timeMessage = new Message("TIME_UPDATE");
                    timeMessage.put("remainingSeconds", timeRemaining);
                    broadcastToAllPlayers(timeMessage);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (isActive) {
            endGame();
        }
    }

    private List<Map<String, Object>> getPlayerInfo() {
        List<Map<String, Object>> playerInfo = new ArrayList<>();
        for (Map.Entry<Integer, ClientHandler> entry : players.entrySet()) {
            User user = entry.getValue().getUser();
            Map<String, Object> info = new HashMap<>();
            info.put("id", user.getId());
            info.put("username", user.getUsername());
            info.put("color", user.getPlayerColor());
            playerInfo.add(info);
        }
        return playerInfo;
    }

    public synchronized boolean processNumberFound(int playerId, int number) {
        if (!isActive || number != targetNumber) {
            return false;
        }

        // Check for priority power-up effect
        for (Map.Entry<Integer, Long> entry : priorityPowerupEndTime.entrySet()) {
            if (entry.getValue() > System.currentTimeMillis() && entry.getKey() != playerId) {
                // Another player has priority, reject this find
                return false;
            }
        }

        // Mark the number as found by this player
        foundNumbers.put(number, playerId);

        // Update player score
        User user = players.get(playerId).getUser();
        user.addScore(1);

        // Check for lucky number (every 10th number)
        if (foundNumbers.size() % 10 == 0) {
            luckyNumberCounts.put(playerId, luckyNumberCounts.getOrDefault(playerId, 0) + 1);

            // Notify player of lucky number
            Message luckyMessage = new Message("LUCKY_NUMBER");
            luckyMessage.put("count", luckyNumberCounts.get(playerId));
            try {
                players.get(playerId).sendMessage(luckyMessage);
            } catch (IOException e) {
                System.err.println("Error sending lucky number message: " + e.getMessage());
            }
        }

        // Generate next target
        generateNextTarget();

        // Notify all players
        Message foundMessage = new Message(Message.NUMBER_FOUND);
        foundMessage.put("number", number);
        foundMessage.put("playerId", playerId);
        foundMessage.put("nextTarget", targetNumber);

        broadcastToAllPlayers(foundMessage);

        // Check if all numbers have been found
        if (foundNumbers.size() >= gridSize) {
            endGame();
        }

        return true;
    }

    public void usePowerup(int playerId, String powerupType) {
        if (!isActive)
            return;

        switch (powerupType) {
            case "PRIORITY":
                if (luckyNumberCounts.getOrDefault(playerId, 0) > 0) {
                    // Use one lucky number count
                    luckyNumberCounts.put(playerId, luckyNumberCounts.get(playerId) - 1);

                    // Give player priority for 3 seconds
                    priorityPowerupEndTime.put(playerId, System.currentTimeMillis() + 3000);

                    // Notify all players
                    Message powerupMessage = new Message(Message.POWERUP_EFFECT);
                    powerupMessage.put("type", "PRIORITY");
                    powerupMessage.put("playerId", playerId);
                    powerupMessage.put("durationMs", 3000);

                    broadcastToAllPlayers(powerupMessage);
                }
                break;
            case "BLOCK_NUMBERS":
                if (luckyNumberCounts.getOrDefault(playerId, 0) > 0) {
                    // Use one lucky number count
                    luckyNumberCounts.put(playerId, luckyNumberCounts.get(playerId) - 1);

                    // Notify all players to block numbers from other players
                    Message powerupMessage = new Message(Message.POWERUP_EFFECT);
                    powerupMessage.put("type", "BLOCK_NUMBERS");
                    powerupMessage.put("playerId", playerId);
                    powerupMessage.put("durationMs", 3000);

                    broadcastToAllPlayers(powerupMessage);
                }
                break;
        }
    }

    private void generateNextTarget() {
        // Generate a new target number that hasn't been found yet
        List<Integer> availableNumbers = new ArrayList<>();
        for (int i = 1; i <= gridSize; i++) {
            if (!foundNumbers.containsKey(i)) {
                availableNumbers.add(i);
            }
        }

        if (!availableNumbers.isEmpty()) {
            targetNumber = availableNumbers.get(random.nextInt(availableNumbers.size()));
        } else {
            // All numbers found, end game
            endGame();
        }
    }

    private synchronized void endGame() {
        if (!isActive)
            return;
        isActive = false;

        // Find the winner (player with most numbers found)
        Map<Integer, Integer> playerScores = new HashMap<>();
        for (Integer playerId : foundNumbers.values()) {
            playerScores.put(playerId, playerScores.getOrDefault(playerId, 0) + 1);
        }

        // Find the winner
        int winnerId = -1;
        int highestScore = -1;

        for (Map.Entry<Integer, Integer> entry : playerScores.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                winnerId = entry.getKey();
            }
        }

        // Calculate game duration
        int durationSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);

        // Create game over message
        Message gameOverMsg = new Message(Message.GAME_OVER);
        gameOverMsg.put("winnerId", winnerId);
        gameOverMsg.put("scores", playerScores);
        gameOverMsg.put("duration", durationSeconds);

        // Update player statistics
        for (Map.Entry<Integer, ClientHandler> entry : players.entrySet()) {
            int playerId = entry.getKey();
            User user = entry.getValue().getUser();

            if (playerId == winnerId) {
                user.incrementGamesWon();
            } else {
                user.incrementGamesLost();
            }

            // Add score from this game
            int playerScore = playerScores.getOrDefault(playerId, 0);
            user.addScore(playerScore);

            // Update in database
            dbManager.updateUserStats(user);
        }

        // Record game in database
        List<Integer> playerIds = new ArrayList<>(players.keySet());
        dbManager.recordGameResult(winnerId, playerIds, durationSeconds);

        // Send game over message to all players
        broadcastToAllPlayers(gameOverMsg);
    }

    private void broadcastToAllPlayers(Message message) {
        for (ClientHandler client : players.values()) {
            try {
                client.sendMessage(message);
            } catch (IOException e) {
                System.err.println("Error broadcasting message: " + e.getMessage());
            }
        }
    }

    private void notifyPlayersOfJoin(User newUser) {
        Message joinMessage = new Message("PLAYER_JOINED");
        joinMessage.put("playerId", newUser.getId());
        joinMessage.put("username", newUser.getUsername());
        joinMessage.put("color", newUser.getPlayerColor());
        joinMessage.put("currentPlayers", players.size());
        joinMessage.put("maxPlayers", maxPlayers);

        broadcastToAllPlayers(joinMessage);
    }

    public boolean isActive() {
        return isActive;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getGameId() {
        return gameId;
    }
}