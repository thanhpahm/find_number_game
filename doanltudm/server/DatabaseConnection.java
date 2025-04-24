package server;

import java.sql.*;
import java.util.*;
import java.io.File;

public class DatabaseManager {
    private static final String DB_URL;
    private Connection connection;

    static {
        // Create database directory if it doesn't exist
        File dbDir = new File("database");
        if (!dbDir.exists()) {
            dbDir.mkdir();
        }

        // Set database URL with absolute path
        DB_URL = "jdbc:sqlite:" + dbDir.getAbsolutePath() + File.separator + "findnumbergame.db";

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load SQLite JDBC driver: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public DatabaseManager() {
        try {
            System.out.println("Connecting to database at: " + DB_URL);
            Driver driver = new org.sqlite.JDBC();
            DriverManager.registerDriver(driver);
            connection = DriverManager.getConnection(DB_URL);
            initTables();
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void initTables() {
        try (Statement stmt = connection.createStatement()) {
            // Users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "email TEXT," +
                    "games_won INTEGER DEFAULT 0," +
                    "games_lost INTEGER DEFAULT 0," +
                    "total_score INTEGER DEFAULT 0)");

            // Game History table
            stmt.execute("CREATE TABLE IF NOT EXISTS game_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "game_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "player_count INTEGER," +
                    "winner_id INTEGER," +
                    "duration_seconds INTEGER)");

            // Game Participants table
            stmt.execute("CREATE TABLE IF NOT EXISTS game_participants (" +
                    "game_id INTEGER," +
                    "user_id INTEGER," +
                    "score INTEGER," +
                    "PRIMARY KEY (game_id, user_id))");

            System.out.println("Database tables initialized");
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
        }
    }

    public boolean addUser(String username, String password) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean verifyLogin(String username, String password) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT id FROM users WHERE username = ? AND password = ?")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Map<String, Object>> getLeaderboard() {
        List<Map<String, Object>> leaderboard = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT username, games_won, games_lost, total_score " +
                     "FROM users ORDER BY total_score DESC LIMIT 20")) {

            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("username", rs.getString("username"));
                entry.put("gamesWon", rs.getInt("games_won"));
                entry.put("gamesLost", rs.getInt("games_lost"));
                entry.put("score", rs.getInt("total_score"));
                leaderboard.add(entry);
            }
        } catch (SQLException e) {
            System.err.println("Error getting leaderboard: " + e.getMessage());
        }
        return leaderboard;
    }

    public void recordGameResult(String winner, List<String> players, int duration) {
        try {
            connection.setAutoCommit(false);

            // Insert game record
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT INTO game_history (player_count, winner_id, duration_seconds) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setInt(1, players.size());
                pstmt.setString(2, winner);
                pstmt.setInt(3, duration);

                pstmt.executeUpdate();

                // Get game ID
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int gameId = rs.getInt(1);
                        // Record participants
                        recordGameParticipants(gameId, players);
                        // Update player stats
                        updatePlayerStats(winner, players);
                    }
                }
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                System.err.println("Error rolling back: " + ex.getMessage());
            }
            System.err.println("Error recording game result: " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }
        }
    }

    private void recordGameParticipants(int gameId, List<String> players) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO game_participants (game_id, user_id) VALUES (?, ?)")) {
            for (String player : players) {
                pstmt.setInt(1, gameId);
                pstmt.setString(2, player);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private void updatePlayerStats(String winner, List<String> players) throws SQLException {
        // Update winner
        try (PreparedStatement winnerStmt = connection.prepareStatement(
                "UPDATE users SET games_won = games_won + 1, total_score = total_score + 1 WHERE username = ?")) {
            winnerStmt.setString(1, winner);
            winnerStmt.executeUpdate();
        }

        // Update losers
        try (PreparedStatement loserStmt = connection.prepareStatement(
                "UPDATE users SET games_lost = games_lost + 1 WHERE username = ? AND username != ?")) {
            for (String player : players) {
                loserStmt.setString(1, player);
                loserStmt.setString(2, winner);
                loserStmt.executeUpdate();
            }
        }
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