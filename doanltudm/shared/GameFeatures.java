package shared;

import java.awt.Color;
import java.io.Serializable;
import java.util.Random;

public interface GameFeatures {
    // Game configuration
    int MIN_PLAYERS = 2;
    int MAX_PLAYERS = 4;
    int MIN_NUMBER = 1;
    int MAX_NUMBER = 100;
    int NUMBERS_PER_PLAYER = 25;
    long GAME_DURATION_SECONDS = 300; // 5 minutes

    // Scoring system
    int BASE_POINTS = 1;
    int LUCKY_NUMBER_BONUS = 5;
    int CONSECUTIVE_MATCH_BONUS = 2;
    int SPEED_BONUS_THRESHOLD_MS = 1000;
    int SPEED_BONUS_POINTS = 2;

    // Power-up configuration
    long PRIORITY_MODE_DURATION = 30000; // 30 seconds
    long BLOCK_NUMBERS_DURATION = 15000; // 15 seconds
    int POWER_UP_COOLDOWN = 60000; // 1 minute

    // Achievement thresholds
    int PERFECT_GAME_THRESHOLD = NUMBERS_PER_PLAYER;
    int SPEED_DEMON_TIME = 180000; // 3 minutes
    int LUCKY_MASTER_THRESHOLD = 10;
    int WINNING_STREAK_THRESHOLD = 5;

    // Default player colors
    Color[] PLAYER_COLORS = {
            new Color(46, 204, 113), // Green
            new Color(52, 152, 219), // Blue
            new Color(155, 89, 182), // Purple
            new Color(231, 76, 60) // Red
    };

    // UI Constants
    int BUTTON_SIZE = 60;
    int BUTTON_MARGIN = 5;
    int GRID_COLUMNS = 5;
    int GRID_ROWS = 5;

    // Default colors for different game states
    Color FOUND_NUMBER_COLOR = new Color(200, 200, 200);
    Color TARGET_NUMBER_COLOR = new Color(255, 235, 59);
    Color BLOCKED_NUMBER_COLOR = new Color(128, 128, 128);

    // Game mechanics
    static boolean isLuckyNumber(int number) {
        if (number < MIN_NUMBER || number > MAX_NUMBER) {
            return false;
        }

        // A number is lucky if it's prime or a perfect square
        return isPrime(number) || isPerfectSquare(number);
    }

    static boolean isPrime(int number) {
        if (number <= 1) {
            return false;
        }
        for (int i = 2; i <= Math.sqrt(number); i++) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }

    static boolean isPerfectSquare(int number) {
        int sqrt = (int) Math.sqrt(number);
        return sqrt * sqrt == number;
    }

    static int[] generateGameNumbers(int playerCount) {
        int totalNumbers = playerCount * NUMBERS_PER_PLAYER;
        int[] numbers = new int[totalNumbers];
        Random random = new Random();

        for (int i = 0; i < totalNumbers; i++) {
            numbers[i] = random.nextInt(MAX_NUMBER - MIN_NUMBER + 1) + MIN_NUMBER;
        }

        // Ensure at least 20% of numbers are lucky numbers
        int minLuckyNumbers = totalNumbers / 5;
        int luckyCount = 0;

        for (int i = 0; i < totalNumbers; i++) {
            if (isLuckyNumber(numbers[i])) {
                luckyCount++;
            }
        }

        while (luckyCount < minLuckyNumbers) {
            int index = random.nextInt(totalNumbers);
            int candidate = random.nextInt(MAX_NUMBER - MIN_NUMBER + 1) + MIN_NUMBER;
            if (isLuckyNumber(candidate)) {
                numbers[index] = candidate;
                luckyCount++;
            }
        }

        return numbers;
    }

    static Color getDefaultPlayerColor(int playerIndex) {
        return PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];
    }

    static String formatTime(long seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    interface GameView extends Serializable {
        void updateDisplay(GameState gameState);

        void showPowerUpEffect(String username, String powerUpType);

        void hidePowerUpEffect(String username, String powerUpType);

        void showGameOver(String winner, java.util.Map<String, Integer> finalScores);

        void showAchievementUnlocked(String achievement);
    }
}
