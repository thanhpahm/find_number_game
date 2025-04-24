package server;

import model.User;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:database/game.db";
    private Connection connection;

    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            initializeTables();
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
    }

    private void initializeTables() throws SQLException {
        // Create or update existing tables
        try (Statement stmt = connection.createStatement()) {
            // Users table with enhanced fields
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            username TEXT UNIQUE NOT NULL,
                            email TEXT UNIQUE NOT NULL,
                            password TEXT NOT NULL,
                            sex TEXT,
                            date_of_birth DATE,
                            games_won INTEGER DEFAULT 0,
                            games_lost INTEGER DEFAULT 0,
                            total_score INTEGER DEFAULT 0,
                            rank_score INTEGER DEFAULT 0,
                            consecutive_wins INTEGER DEFAULT 0,
                            lucky_numbers_found INTEGER DEFAULT 0,
                            best_game_time INTEGER DEFAULT 2147483647
                        )
                    """);

            // Power-ups table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS power_ups (
                            user_id INTEGER,
                            power_up_type TEXT,
                            count INTEGER DEFAULT 0,
                            last_use_time INTEGER DEFAULT 0,
                            FOREIGN KEY (user_id) REFERENCES users(id),
                            PRIMARY KEY (user_id, power_up_type)
                        )
                    """);

            // Achievements table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS achievements (
                            user_id INTEGER,
                            achievement TEXT,
                            unlock_time INTEGER,
                            FOREIGN KEY (user_id) REFERENCES users(id),
                            PRIMARY KEY (user_id, achievement)
                        )
                    """);

            // Game history table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS game_history (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            game_time INTEGER NOT NULL,
                            winner_id INTEGER,
                            duration INTEGER,
                            FOREIGN KEY (winner_id) REFERENCES users(id)
                        )
                    """);

            // Game participants table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS game_participants (
                            game_id INTEGER,
                            user_id INTEGER,
                            score INTEGER,
                            power_ups_used INTEGER,
                            lucky_numbers_found INTEGER,
                            FOREIGN KEY (game_id) REFERENCES game_history(id),
                            FOREIGN KEY (user_id) REFERENCES users(id),
                            PRIMARY KEY (game_id, user_id)
                        )
                    """);
        }
    }

    public User authenticateUser(String username, String password) throws SQLException {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = createUserFromResultSet(rs);
                    loadUserPowerUps(user);
                    loadUserAchievements(user);
                    return user;
                }
            }
        }
        return null;
    }

    public boolean registerUser(User user) throws SQLException {
        String query = """
                    INSERT INTO users (username, email, password, sex, date_of_birth)
                    VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getSex());
            pstmt.setDate(5, new java.sql.Date(user.getDateOfBirth().getTime()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getInt(1));
                        initializeUserPowerUps(user);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void initializeUserPowerUps(User user) throws SQLException {
        String query = "INSERT INTO power_ups (user_id, power_up_type, count) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            for (User.PowerUpType type : User.PowerUpType.values()) {
                pstmt.setInt(1, user.getId());
                pstmt.setString(2, type.name());
                pstmt.setInt(3, 3); // Start with 3 of each power-up
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private void loadUserPowerUps(User user) throws SQLException {
        String query = "SELECT power_up_type, count, last_use_time FROM power_ups WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, user.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User.PowerUpType type = User.PowerUpType.valueOf(rs.getString("power_up_type"));
                    int count = rs.getInt("count");
                    user.getPowerUps().put(type, count);
                }
            }
        }
    }

    public void updateUserPowerUps(User user) throws SQLException {
        String query = """
                    UPDATE power_ups
                    SET count = ?, last_use_time = ?
                    WHERE user_id = ? AND power_up_type = ?
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            for (Map.Entry<User.PowerUpType, Integer> entry : user.getPowerUps().entrySet()) {
                pstmt.setInt(1, entry.getValue());
                pstmt.setLong(2, System.currentTimeMillis());
                pstmt.setInt(3, user.getId());
                pstmt.setString(4, entry.getKey().name());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public void updateUserStats(User user) throws SQLException {
        String query = """
                    UPDATE users
                    SET games_won = ?, games_lost = ?, total_score = ?,
                        consecutive_wins = ?, lucky_numbers_found = ?,
                        best_game_time = ?
                    WHERE id = ?
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, user.getGamesWon());
            pstmt.setInt(2, user.getGamesLost());
            pstmt.setInt(3, user.getTotalScore());
            pstmt.setInt(4, user.getConsecutiveWins());
            pstmt.setInt(5, user.getLuckyNumbersFound());
            pstmt.setLong(6, user.getBestGameTime());
            pstmt.setInt(7, user.getId());
            pstmt.executeUpdate();
        }
    }

    public void updateRankScore(String username, int score) throws SQLException {
        String query = "UPDATE users SET rank_score = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, score);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }
    }

    public void updateAchievements(String username, Set<String> newAchievements) throws SQLException {
        String query = """
                    INSERT OR IGNORE INTO achievements (user_id, achievement, unlock_time)
                    VALUES ((SELECT id FROM users WHERE username = ?), ?, ?)
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            for (String achievement : newAchievements) {
                pstmt.setString(1, username);
                pstmt.setString(2, achievement);
                pstmt.setLong(3, System.currentTimeMillis());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public Set<String> loadAchievements(String username) throws SQLException {
        Set<String> achievements = new HashSet<>();
        String query = """
                    SELECT achievement
                    FROM achievements a
                    JOIN users u ON a.user_id = u.id
                    WHERE u.username = ?
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    achievements.add(rs.getString("achievement"));
                }
            }
        }
        return achievements;
    }

    public List<Map<String, Object>> getTopRankedPlayers(int limit) {
        List<Map<String, Object>> players = new ArrayList<>();
        String query = """
                    SELECT username, rank_score as score, games_won, games_lost,
                           total_score, lucky_numbers_found
                    FROM users
                    ORDER BY rank_score DESC
                    LIMIT ?
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> player = new HashMap<>();
                    player.put("username", rs.getString("username"));
                    player.put("score", rs.getInt("score"));
                    player.put("gamesWon", rs.getInt("games_won"));
                    player.put("gamesLost", rs.getInt("games_lost"));
                    player.put("totalScore", rs.getInt("total_score"));
                    player.put("luckyNumbers", rs.getInt("lucky_numbers_found"));
                    players.add(player);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting top players: " + e.getMessage());
        }
        return players;
    }

    private User createUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("username"),
                rs.getString("sex"),
                rs.getDate("date_of_birth"));
        user.setId(rs.getInt("id"));
        user.setGamesWon(rs.getInt("games_won"));
        user.setGamesLost(rs.getInt("games_lost"));
        user.setTotalScore(rs.getInt("total_score"));
        return user;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}