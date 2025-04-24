package server;

import shared.GameFeatures;
import shared.ScoreTracker.PlayerStats;
import java.util.*;
import java.sql.SQLException;

public class RankingSystem {
    private final DatabaseManager dbManager;
    private final Map<String, Double> rankFactors;

    public RankingSystem(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.rankFactors = new HashMap<>();
        initializeRankFactors();
    }

    private void initializeRankFactors() {
        rankFactors.put("winRate", 0.4);
        rankFactors.put("avgScore", 0.3);
        rankFactors.put("luckyNumbers", 0.2);
        rankFactors.put("consistency", 0.1);
    }

    public void updatePlayerRanking(String username, PlayerStats stats) {
        try {
            // Calculate rank score
            double rankScore = calculateRankScore(stats);
            dbManager.updateRankScore(username, (int) rankScore);

            // Check and award achievements
            checkAndAwardAchievements(username, stats);

        } catch (SQLException e) {
            System.err.println("Error updating player ranking: " + e.getMessage());
        }
    }

    private double calculateRankScore(PlayerStats stats) {
        double winRateScore = stats.getWinRate() * 1000;
        double avgScoreScore = stats.getAverageScore() * 10;
        double luckyNumberScore = stats.getLuckyNumbersFound() * 50;
        double consistencyScore = calculateConsistencyScore(stats);

        return (winRateScore * rankFactors.get("winRate")) +
                (avgScoreScore * rankFactors.get("avgScore")) +
                (luckyNumberScore * rankFactors.get("luckyNumbers")) +
                (consistencyScore * rankFactors.get("consistency"));
    }

    private double calculateConsistencyScore(PlayerStats stats) {
        double avgTime = stats.getAverageTime();
        double bestTime = stats.getBestTime();
        if (avgTime == 0 || bestTime == Long.MAX_VALUE) {
            return 0;
        }

        // Calculate consistency based on average time vs best time
        double timeRatio = bestTime / avgTime;
        return Math.min(1000, timeRatio * 500);
    }

    private void checkAndAwardAchievements(String username, PlayerStats stats) throws SQLException {
        Set<String> achievements = new HashSet<>();

        // First Victory
        if (stats.getGamesWon() >= 1) {
            achievements.add("First Victory");
        }

        // Lucky Collector
        if (stats.getLuckyNumbersFound() >= 5) {
            achievements.add("Lucky Collector");
        }

        // Speed Demon
        if (stats.getBestTime() <= GameFeatures.SPEED_DEMON_TIME) {
            achievements.add("Speed Demon");
        }

        // Perfect Game - checked elsewhere when game ends

        // Veteran
        if (stats.getGamesPlayed() >= GameFeatures.VETERAN_GAMES_THRESHOLD) {
            achievements.add("Veteran");
        }

        // Champion
        if (stats.getGamesWon() >= GameFeatures.CHAMPION_WINS_THRESHOLD) {
            achievements.add("Champion");
        }

        // Lucky Master
        if (stats.getLuckyNumbersFound() >= GameFeatures.LUCKY_MASTER_THRESHOLD) {
            achievements.add("Lucky Master");
        }

        // Winning Streak
        if (stats.getBestConsecutiveWins() >= GameFeatures.WINNING_STREAK_THRESHOLD) {
            achievements.add("Winning Streak");
        }

        // Update achievements in database
        if (!achievements.isEmpty()) {
            dbManager.updateAchievements(username, achievements);
        }
    }

    public List<Map<String, Object>> getTopPlayers(int limit) {
        return dbManager.getTopRankedPlayers(limit);
    }

    public Map<String, Object> getPlayerRankInfo(String username) throws SQLException {
        Map<String, Object> rankInfo = new HashMap<>();

        // Get player's rank score and achievements
        List<Map<String, Object>> topPlayers = dbManager.getTopRankedPlayers(Integer.MAX_VALUE);
        int rank = 1;
        double score = 0;

        for (Map<String, Object> player : topPlayers) {
            if (player.get("username").equals(username)) {
                score = (double) player.get("score");
                rankInfo.put("rank", rank);
                rankInfo.put("score", score);
                break;
            }
            rank++;
        }

        // Get achievements
        Set<String> achievements = dbManager.loadAchievements(username);
        rankInfo.put("achievements", achievements);

        // Calculate percentile if we have a rank
        if (rankInfo.containsKey("rank")) {
            double percentile = 100.0 * (1.0 - ((double) rank / topPlayers.size()));
            rankInfo.put("percentile", percentile);
        }

        return rankInfo;
    }
}
