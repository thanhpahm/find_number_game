package shared;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Integer> scores;
    private final Map<String, List<Integer>> playerNumbers;
    private final Map<String, Set<Integer>> blockedNumbers;
    private int[] gameNumbers;
    private boolean gameComplete;
    private final Map<String, Long> lastMoveTime;
    private final Map<String, Integer> consecutiveMatches;

    public GameState() {
        this.scores = new ConcurrentHashMap<>();
        this.playerNumbers = new ConcurrentHashMap<>();
        this.blockedNumbers = new ConcurrentHashMap<>();
        this.lastMoveTime = new ConcurrentHashMap<>();
        this.consecutiveMatches = new ConcurrentHashMap<>();
        this.gameComplete = false;
    }

    public void initializeGame(int playerCount) {
        gameNumbers = GameFeatures.generateGameNumbers(playerCount);
        gameComplete = false;
        scores.clear();
        playerNumbers.clear();
        blockedNumbers.clear();
        lastMoveTime.clear();
        consecutiveMatches.clear();
    }

    public synchronized void addPlayer(String username) {
        scores.put(username, 0);
        playerNumbers.put(username, new ArrayList<>());
        blockedNumbers.put(username, new HashSet<>());
        lastMoveTime.put(username, System.currentTimeMillis());
        consecutiveMatches.put(username, 0);
    }

    public synchronized boolean isValidNumber(int number) {
        if (number < GameFeatures.MIN_NUMBER || number > GameFeatures.MAX_NUMBER) {
            return false;
        }

        for (int gameNumber : gameNumbers) {
            if (gameNumber == number) {
                return true;
            }
        }
        return false;
    }

    public synchronized void updateScore(String username, int points) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastMove = currentTime - lastMoveTime.getOrDefault(username, currentTime);

        // Apply speed bonus if applicable
        if (timeSinceLastMove < GameFeatures.SPEED_BONUS_THRESHOLD_MS) {
            points += GameFeatures.SPEED_BONUS_POINTS;
        }

        // Apply consecutive match bonus if applicable
        int currentConsecutive = consecutiveMatches.getOrDefault(username, 0);
        if (currentConsecutive > 0) {
            points *= GameFeatures.CONSECUTIVE_MATCH_BONUS;
        }
        consecutiveMatches.put(username, currentConsecutive + 1);

        // Update score
        scores.merge(username, points, Integer::sum);
        lastMoveTime.put(username, currentTime);

        // Check if game is complete
        checkGameCompletion();
    }

    private synchronized void checkGameCompletion() {
        int totalFound = playerNumbers.values().stream()
                .mapToInt(List::size)
                .sum();

        gameComplete = totalFound >= gameNumbers.length;
    }

    public synchronized void blockNumbers(String username, Set<Integer> numbers) {
        blockedNumbers.get(username).addAll(numbers);
    }

    public synchronized void unblockNumbers(String username) {
        blockedNumbers.get(username).clear();
    }

    public synchronized boolean isNumberBlocked(int number, String username) {
        return blockedNumbers.get(username).contains(number);
    }

    public synchronized String getWinner() {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public synchronized Map<String, Integer> getScores() {
        return new HashMap<>(scores);
    }

    public synchronized boolean isGameComplete() {
        return gameComplete;
    }

    public synchronized int[] getGameNumbers() {
        return Arrays.copyOf(gameNumbers, gameNumbers.length);
    }

    public synchronized List<Integer> getPlayerNumbers(String username) {
        return new ArrayList<>(playerNumbers.getOrDefault(username, new ArrayList<>()));
    }

    public synchronized Set<Integer> getBlockedNumbers(String username) {
        return new HashSet<>(blockedNumbers.getOrDefault(username, new HashSet<>()));
    }

    public synchronized int getTotalNumbers() {
        return gameNumbers.length;
    }

    public synchronized Map<String, Integer> getConsecutiveMatches() {
        return new HashMap<>(consecutiveMatches);
    }

    public synchronized long getLastMoveTime(String username) {
        return lastMoveTime.getOrDefault(username, 0L);
    }

    public synchronized void resetConsecutiveMatches(String username) {
        consecutiveMatches.put(username, 0);
    }
}
