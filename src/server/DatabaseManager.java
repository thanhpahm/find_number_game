package server;

import common.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.Date;

/**
 * Manages database operations for the game
 */
public class DatabaseManager {
    // Change the path to use absolute path for more reliability
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
            // Load SQLite JDBC driver explicitly
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite JDBC driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load SQLite JDBC driver: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public DatabaseManager() {
        try {
            System.out.println("Connecting to database at: " + DB_URL);

            // Create a new instance of the SQLite JDBC driver manually
            Driver driver = new org.sqlite.JDBC();
            DriverManager.registerDriver(driver);

            // Create database if it doesn't exist
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Database connection established successfully.");
            initTables();
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initTables() {
        try (Statement stmt = connection.createStatement()) {
            // Create Users table with new fields
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "email TEXT," +
                    "sex TEXT," +
                    "date_of_birth DATE," +
                    "games_won INTEGER DEFAULT 0," +
                    "games_lost INTEGER DEFAULT 0," +
                    "total_score INTEGER DEFAULT 0)");

            // Create GameHistory table
            stmt.execute("CREATE TABLE IF NOT EXISTS game_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "game_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "player_count INTEGER," +
                    "winner_id INTEGER," +
                    "duration_seconds INTEGER," +
                    "FOREIGN KEY (winner_id) REFERENCES users(id))");

            // Create GameParticipants table
            stmt.execute("CREATE TABLE IF NOT EXISTS game_participants (" +
                    "game_id INTEGER," +
                    "user_id INTEGER," +
                    "score INTEGER," +
                    "PRIMARY KEY (game_id, user_id)," +
                    "FOREIGN KEY (game_id) REFERENCES game_history(id)," +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

            System.out.println("Database tables initialized");
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
        }
    }

    public boolean registerUser(User user) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO users (username, password, email, sex, date_of_birth) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getSex());

            // Handle date of birth (could be null)
            if (user.getDateOfBirth() != null) {
                pstmt.setDate(5, new java.sql.Date(user.getDateOfBirth().getTime()));
            } else {
                pstmt.setNull(5, Types.DATE);
            }

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
        }
        return false;
    }

    public User authenticateUser(String username, String password) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM users WHERE username = ? AND password = ?")) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(username, password);
                    user.setId(rs.getInt("id"));
                    user.setGamesWon(rs.getInt("games_won"));
                    user.setGamesLost(rs.getInt("games_lost"));
                    user.setTotalScore(rs.getInt("total_score"));

                    // Get the new fields
                    user.setEmail(rs.getString("email"));
                    user.setSex(rs.getString("sex"));

                    // Handle date of birth
                    java.sql.Date dobSql = rs.getDate("date_of_birth");
                    if (dobSql != null) {
                        user.setDateOfBirth(new Date(dobSql.getTime()));
                    }

                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
        }
        return null;
    }

    public List<User> getLeaderboard() {
        List<User> leaderboard = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM users ORDER BY total_score DESC, games_won DESC LIMIT 20")) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setGamesWon(rs.getInt("games_won"));
                user.setGamesLost(rs.getInt("games_lost"));
                user.setTotalScore(rs.getInt("total_score"));
                leaderboard.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving leaderboard: " + e.getMessage());
        }
        return leaderboard;
    }

    public void updateUserStats(User user) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE users SET games_won = ?, games_lost = ?, total_score = ? WHERE id = ?")) {

            pstmt.setInt(1, user.getGamesWon());
            pstmt.setInt(2, user.getGamesLost());
            pstmt.setInt(3, user.getTotalScore());
            pstmt.setInt(4, user.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating user stats: " + e.getMessage());
        }
    }

    public void recordGameResult(int winnerId, List<Integer> playerIds, int durationSeconds) {
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT INTO game_history (player_count, winner_id, duration_seconds) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setInt(1, playerIds.size());
                pstmt.setInt(2, winnerId);
                pstmt.setInt(3, durationSeconds);

                pstmt.executeUpdate();

                int gameId;
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        gameId = rs.getInt(1);

                        // Record participants
                        try (PreparedStatement pstmt2 = connection.prepareStatement(
                                "INSERT INTO game_participants (game_id, user_id, score) VALUES (?, ?, ?)")) {

                            for (int playerId : playerIds) {
                                pstmt2.setInt(1, gameId);
                                pstmt2.setInt(2, playerId);
                                pstmt2.setInt(3, 0); // Default score, can be updated later
                                pstmt2.addBatch();
                            }

                            pstmt2.executeBatch();
                        }
                    }
                }

                connection.commit();
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
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