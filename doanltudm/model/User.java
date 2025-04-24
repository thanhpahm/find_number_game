package model;

import java.io.Serializable;
import java.util.*;
import shared.GameFeatures;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum PowerUpType {
        PRIORITY_MODE,
        BLOCK_NUMBERS,
        SPEED_BOOST,
        SHIELD
    }

    private int id;
    private String email;
    private String password;
    private String username;
    private String sex;
    private Date dateOfBirth;
    private int gamesWon;
    private int gamesLost;
    private int totalScore;
    private int playerColor;

    // Power-up related fields
    private Map<PowerUpType, Integer> powerUps;
    private long lastPriorityModeUse;
    private long lastBlockNumbersUse;

    // Achievement related fields
    private Set<String> achievements;
    private int consecutiveWins;
    private int luckyNumbersFound;
    private long bestGameTime;
    private Map<PowerUpType, Long> lastPowerUpUse;

    public User() {
        this.gamesWon = 0;
        this.gamesLost = 0;
        this.totalScore = 0;
        this.powerUps = new EnumMap<>(PowerUpType.class);
        this.achievements = new HashSet<>();
        this.consecutiveWins = 0;
        this.luckyNumbersFound = 0;
        this.bestGameTime = Long.MAX_VALUE;
        this.lastPowerUpUse = new EnumMap<>(PowerUpType.class);
        initializePowerUps();
    }

    public User(String email, String password, String username, String sex, Date dateOfBirth) {
        this();
        this.email = email;
        this.password = password;
        this.username = username;
        this.sex = sex;
        this.dateOfBirth = dateOfBirth;
    }

    private void initializePowerUps() {
        for (PowerUpType type : PowerUpType.values()) {
            powerUps.put(type, 3); // Start with 3 of each power-up
            lastPowerUpUse.put(type, 0L);
        }
    }

    public boolean isPowerUpAvailable(PowerUpType type) {
        int count = powerUps.getOrDefault(type, 0);
        if (count <= 0)
            return false;

        // Check cooldown for certain power-ups
        long currentTime = System.currentTimeMillis();
        switch (type) {
            case PRIORITY_MODE:
                return currentTime - lastPriorityModeUse >= 30000; // 30 second cooldown
            case BLOCK_NUMBERS:
                return currentTime - lastBlockNumbersUse >= 45000; // 45 second cooldown
            default:
                return true;
        }
    }

    public boolean usePowerUp(PowerUpType type) {
        if (!isPowerUpAvailable(type)) {
            return false;
        }

        powerUps.compute(type, (k, v) -> v - 1);

        // Update last use time for cooldown power-ups
        long currentTime = System.currentTimeMillis();
        switch (type) {
            case PRIORITY_MODE:
                lastPriorityModeUse = currentTime;
                break;
            case BLOCK_NUMBERS:
                lastBlockNumbersUse = currentTime;
                break;
        }

        return true;
    }

    public void addPowerUp(PowerUpType type, int count) {
        powerUps.compute(type, (k, v) -> v + count);
    }

    public void recordGameResult(boolean won, int score, long gameTime) {
        if (won) {
            gamesWon++;
            consecutiveWins++;
            if (gameTime < bestGameTime) {
                bestGameTime = gameTime;
            }
            // Award power-up for winning
            addPowerUp(PowerUpType.PRIORITY_MODE, 1);
        } else {
            gamesLost++;
            consecutiveWins = 0;
        }
        totalScore += score;
    }

    public void addLuckyNumber() {
        luckyNumbersFound++;
        if (luckyNumbersFound % 5 == 0) {
            // Award power-up every 5 lucky numbers
            addPowerUp(PowerUpType.BLOCK_NUMBERS, 1);
        }
    }

    public void addAchievement(String achievement) {
        if (achievements.add(achievement)) {
            // Reward power-ups for achievements
            switch (achievement) {
                case "Perfect Game":
                    addPowerUp(PowerUpType.PRIORITY_MODE, 1);
                    addPowerUp(PowerUpType.BLOCK_NUMBERS, 1);
                    break;
                case "Speed Demon":
                case "Lucky Master":
                    addPowerUp(PowerUpType.PRIORITY_MODE, 1);
                    break;
                case "Champion":
                case "Winning Streak":
                    addPowerUp(PowerUpType.BLOCK_NUMBERS, 1);
                    break;
            }
        }
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public int getGamesLost() {
        return gamesLost;
    }

    public void setGamesLost(int gamesLost) {
        this.gamesLost = gamesLost;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public void incrementGamesWon() {
        this.gamesWon++;
    }

    public void incrementGamesLost() {
        this.gamesLost++;
    }

    public void addScore(int points) {
        this.totalScore += points;
    }

    public double getWinRate() {
        int totalGames = gamesWon + gamesLost;
        if (totalGames == 0)
            return 0;
        return (double) gamesWon / totalGames;
    }

    public int getPlayerColor() {
        return playerColor;
    }

    public void setPlayerColor(int playerColor) {
        this.playerColor = playerColor;
    }

    public Map<PowerUpType, Integer> getPowerUps() {
        return new EnumMap<>(powerUps);
    }

    public Set<String> getAchievements() {
        return new HashSet<>(achievements);
    }

    public void addAchievement(String achievement) {
        achievements.add(achievement);
    }

    public int getConsecutiveWins() {
        return consecutiveWins;
    }

    public int getLuckyNumbersFound() {
        return luckyNumbersFound;
    }

    public long getBestGameTime() {
        return bestGameTime;
    }

    @Override
    public String toString() {
        return username;
    }

    public UserStats getStats() {
        return new UserStats(this);
    }

    public static class UserStats implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int gamesWon;
        private final int gamesLost;
        private final int totalScore;
        private final int consecutiveWins;
        private final int luckyNumbersFound;
        private final long bestGameTime;

        private UserStats(User user) {
            this.gamesWon = user.gamesWon;
            this.gamesLost = user.gamesLost;
            this.totalScore = user.totalScore;
            this.consecutiveWins = user.consecutiveWins;
            this.luckyNumbersFound = user.luckyNumbersFound;
            this.bestGameTime = user.bestGameTime;
        }

        public int getGamesPlayed() {
            return gamesWon + gamesLost;
        }

        public double getWinRate() {
            int total = getGamesPlayed();
            return total > 0 ? (double) gamesWon / total : 0.0;
        }

        public double getAverageScore() {
            int total = getGamesPlayed();
            return total > 0 ? (double) totalScore / total : 0.0;
        }

        // Getters
        public int getGamesWon() {
            return gamesWon;
        }

        public int getGamesLost() {
            return gamesLost;
        }

        public int getTotalScore() {
            return totalScore;
        }

        public int getConsecutiveWins() {
            return consecutiveWins;
        }

        public int getLuckyNumbersFound() {
            return luckyNumbersFound;
        }

        public long getBestGameTime() {
            return bestGameTime;
        }
    }
}
