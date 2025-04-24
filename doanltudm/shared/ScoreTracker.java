package shared;

import java.io.Serializable;
import java.util.*;

public class ScoreTracker implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, PlayerStats> playerStats;
    private Map<String, List<GameRecord>> recentGames;
    private int totalGamesPlayed;
    private long totalGameTime;

    public ScoreTracker() {
        this.playerStats = new HashMap<>();
        this.recentGames = new HashMap<>();
        this.totalGamesPlayed = 0;
        this.totalGameTime = 0;
    }

    public synchronized void recordGame(Map<String, Integer> finalScores, String winner, long gameDuration) {
        totalGamesPlayed++;
        totalGameTime += gameDuration;

        GameRecord gameRecord = new GameRecord(
                finalScores,
                winner,
                gameDuration,
                System.currentTimeMillis());

        // Update stats for each player
        finalScores.forEach((playerName, score) -> {
            PlayerStats stats = playerStats.computeIfAbsent(playerName, k -> new PlayerStats());
            stats.updateStats(score, playerName.equals(winner), gameDuration);

            // Add to recent games
            List<GameRecord> playerGames = recentGames.computeIfAbsent(playerName, k -> new ArrayList<>());
            playerGames.add(0, gameRecord);
            if (playerGames.size() > 10) { // Keep only last 10 games
                playerGames.remove(playerGames.size() - 1);
            }
        });
    }

    public synchronized PlayerStats getPlayerStats(String playerName) {
        return playerStats.getOrDefault(playerName, new PlayerStats());
    }

    public synchronized List<GameRecord> getRecentGames(String playerName) {
        return new ArrayList<>(recentGames.getOrDefault(playerName, new ArrayList<>()));
    }

    public synchronized Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGames", totalGamesPlayed);
        stats.put("activePlayers", playerStats.size());
        stats.put("avgGameDuration", totalGamesPlayed > 0 ? totalGameTime / totalGamesPlayed : 0);

        // Calculate overall average score
        double overallAverageScore = playerStats.values().stream()
                .mapToDouble(PlayerStats::getAverageScore)
                .average()
                .orElse(0.0);
        stats.put("avgScore", overallAverageScore);

        return stats;
    }

    public synchronized List<Map<String, Object>> getLeaderboard() {
        return playerStats.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> playerData = new HashMap<>();
                    PlayerStats stats = entry.getValue();
                    playerData.put("username", entry.getKey());
                    playerData.put("score", stats.getScore());
                    playerData.put("winRate", stats.getWinRate());
                    playerData.put("gamesWon", stats.getGamesWon());
                    playerData.put("luckyNumbers", stats.getLuckyNumbersFound());
                    return playerData;
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get("score"), (Integer) a.get("score")))
                .limit(20)
                .toList();
    }

    public static class PlayerStats implements Serializable {
        private static final long serialVersionUID = 1L;

        private int gamesPlayed;
        private int gamesWon;
        private int totalScore;
        private int luckyNumbersFound;
        private long bestTime;
        private long totalGameTime;
        private int consecutiveWins;
        private int bestConsecutiveWins;

        public PlayerStats() {
            this.gamesPlayed = 0;
            this.gamesWon = 0;
            this.totalScore = 0;
            this.luckyNumbersFound = 0;
            this.bestTime = Long.MAX_VALUE;
            this.totalGameTime = 0;
            this.consecutiveWins = 0;
            this.bestConsecutiveWins = 0;
        }

        public synchronized void updateStats(int score, boolean won, long gameTime) {
            gamesPlayed++;
            totalScore += score;
            totalGameTime += gameTime;

            if (won) {
                gamesWon++;
                consecutiveWins++;
                if (consecutiveWins > bestConsecutiveWins) {
                    bestConsecutiveWins = consecutiveWins;
                }
                if (gameTime < bestTime) {
                    bestTime = gameTime;
                }
            } else {
                consecutiveWins = 0;
            }
        }

        public void addLuckyNumber() {
            luckyNumbersFound++;
        }

        // Getters
        public int getGamesPlayed() {
            return gamesPlayed;
        }

        public int getGamesWon() {
            return gamesWon;
        }

        public int getScore() {
            return totalScore;
        }

        public double getWinRate() {
            return gamesPlayed > 0 ? (double) gamesWon / gamesPlayed : 0;
        }

        public double getAverageScore() {
            return gamesPlayed > 0 ? (double) totalScore / gamesPlayed : 0;
        }

        public int getLuckyNumbersFound() {
            return luckyNumbersFound;
        }

        public long getBestTime() {
            return bestTime;
        }

        public double getAverageTime() {
            return gamesPlayed > 0 ? (double) totalGameTime / gamesPlayed : 0;
        }

        public int getConsecutiveWins() {
            return consecutiveWins;
        }

        public int getBestConsecutiveWins() {
            return bestConsecutiveWins;
        }
    }

    public static class GameRecord implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Map<String, Integer> scores;
        private final String winner;
        private final long duration;
        private final long timestamp;

        public GameRecord(Map<String, Integer> scores, String winner, long duration, long timestamp) {
            this.scores = new HashMap<>(scores);
            this.winner = winner;
            this.duration = duration;
            this.timestamp = timestamp;
        }

        public Map<String, Integer> getScores() {
            return new HashMap<>(scores);
        }

        public String getWinner() {
            return winner;
        }

        public long getDuration() {
            return duration;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getTimeAgo() {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            if (diff < 60000) { // Less than a minute
                return "just now";
            } else if (diff < 3600000) { // Less than an hour
                return (diff / 60000) + " minutes ago";
            } else if (diff < 86400000) { // Less than a day
                return (diff / 3600000) + " hours ago";
            } else {
                return (diff / 86400000) + " days ago";
            }
        }
    }
}
