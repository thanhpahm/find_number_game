package server;

import shared.GameFeatures;
import shared.GameState;
import java.util.*;
import java.util.concurrent.*;

public class GameMatch {
    private final GameState gameState;
    private final Map<String, ClientHandler> players;
    private final ScheduledExecutorService scheduler;
    private final RankingSystem rankingSystem;
    private final Map<String, Long> powerUpCooldowns;
    private ScheduledFuture<?> gameTimer;
    private final long startTime;

    public GameMatch(RankingSystem rankingSystem) {
        this.gameState = new GameState();
        this.players = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.rankingSystem = rankingSystem;
        this.powerUpCooldowns = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
    }

    public synchronized void addPlayer(String username, ClientHandler handler) {
        if (players.size() >= GameFeatures.MAX_PLAYERS) {
            throw new IllegalStateException("Game is full");
        }

        players.put(username, handler);
        gameState.addPlayer(username);
        powerUpCooldowns.put(username, 0L);

        if (players.size() >= GameFeatures.MIN_PLAYERS) {
            startGameIfReady();
        }
    }

    private void startGameIfReady() {
        if (!gameState.isGameComplete() && gameTimer == null) {
            gameState.initializeGame(players.size());
            broadcastGameState();

            // Schedule game end after duration
            gameTimer = scheduler.schedule(() -> {
                endGame("Time's up!");
            }, GameFeatures.GAME_DURATION_SECONDS, TimeUnit.SECONDS);
        }
    }

    public synchronized boolean processNumber(String username, int number) {
        if (gameState.isGameComplete() || !gameState.isValidNumber(number) ||
                gameState.isNumberBlocked(number, username)) {
            return false;
        }

        int points = GameFeatures.BASE_POINTS;
        if (GameFeatures.isLuckyNumber(number)) {
            points += GameFeatures.LUCKY_NUMBER_BONUS;
        }

        gameState.updateScore(username, points);
        broadcastGameState();

        if (gameState.isGameComplete()) {
            endGame("Game Complete!");
        }

        return true;
    }

    public synchronized void activatePowerUp(String username, String powerUpType) {
        long currentTime = System.currentTimeMillis();
        long lastUsed = powerUpCooldowns.getOrDefault(username, 0L);

        if (currentTime - lastUsed < GameFeatures.POWER_UP_COOLDOWN) {
            return;
        }

        powerUpCooldowns.put(username, currentTime);

        switch (powerUpType) {
            case "PRIORITY":
                handlePriorityMode(username);
                break;
            case "BLOCK":
                handleBlockNumbers(username);
                break;
        }

        broadcastGameState();
    }

    private void handlePriorityMode(String username) {
        // Give player exclusive access for a short duration
        scheduler.schedule(() -> {
            broadcastToAll("POWER_UP_END", Map.of(
                    "type", "PRIORITY",
                    "username", username));
        }, GameFeatures.PRIORITY_MODE_DURATION, TimeUnit.MILLISECONDS);

        broadcastToAll("POWER_UP_START", Map.of(
                "type", "PRIORITY",
                "username", username));
    }

    private void handleBlockNumbers(String username) {
        // Block a random set of numbers for other players
        Random random = new Random();
        Set<Integer> numbersToBlock = new HashSet<>();
        int[] gameNumbers = gameState.getGameNumbers();

        for (int i = 0; i < 5; i++) {
            numbersToBlock.add(gameNumbers[random.nextInt(gameNumbers.length)]);
        }

        players.keySet().stream()
                .filter(player -> !player.equals(username))
                .forEach(player -> gameState.blockNumbers(player, numbersToBlock));

        scheduler.schedule(() -> {
            players.keySet().forEach(gameState::unblockNumbers);
            broadcastToAll("POWER_UP_END", Map.of(
                    "type", "BLOCK",
                    "username", username));
            broadcastGameState();
        }, GameFeatures.BLOCK_NUMBERS_DURATION, TimeUnit.MILLISECONDS);

        broadcastToAll("POWER_UP_START", Map.of(
                "type", "BLOCK",
                "username", username,
                "numbers", numbersToBlock));
    }

    private synchronized void endGame(String reason) {
        if (gameTimer != null) {
            gameTimer.cancel(false);
        }

        String winner = gameState.getWinner();
        Map<String, Integer> finalScores = gameState.getScores();

        // Update rankings
        finalScores.forEach((username, score) -> {
            boolean isWinner = username.equals(winner);
            rankingSystem.updatePlayerRanking(username, score, isWinner);
        });

        // Check and award achievements
        checkAchievements(finalScores, winner);

        broadcastToAll("GAME_OVER", Map.of(
                "reason", reason,
                "winner", winner,
                "scores", finalScores));

        cleanup();
    }

    private void checkAchievements(Map<String, Integer> finalScores, String winner) {
        // Perfect game achievement
        if (finalScores.get(winner) >= GameFeatures.PERFECT_GAME_THRESHOLD) {
            notifyAchievement(winner, "PERFECT_GAME");
        }

        // Speed demon achievement
        long gameDuration = System.currentTimeMillis() - startTime;
        if (gameDuration <= GameFeatures.SPEED_DEMON_TIME) {
            notifyAchievement(winner, "SPEED_DEMON");
        }

        // Lucky master achievement
        Map<String, Integer> consecutiveMatches = gameState.getConsecutiveMatches();
        if (consecutiveMatches.get(winner) >= GameFeatures.LUCKY_MASTER_THRESHOLD) {
            notifyAchievement(winner, "LUCKY_MASTER");
        }
    }

    private void notifyAchievement(String username, String achievement) {
        ClientHandler handler = players.get(username);
        if (handler != null) {
            handler.sendMessage("ACHIEVEMENT_UNLOCKED", Map.of(
                    "achievement", achievement));
        }
    }

    private void broadcastGameState() {
        broadcastToAll("GAME_STATE", Map.of(
                "state", gameState,
                "scores", gameState.getScores(),
                "blocked", gameState.getBlockedNumbers(null)));
    }

    private void broadcastToAll(String type, Map<String, Object> data) {
        players.values().forEach(handler -> handler.sendMessage(type, data));
    }

    private void cleanup() {
        scheduler.shutdown();
        players.clear();
        powerUpCooldowns.clear();
    }

    public boolean isComplete() {
        return gameState.isGameComplete();
    }

    public int getPlayerCount() {
        return players.size();
    }
}
