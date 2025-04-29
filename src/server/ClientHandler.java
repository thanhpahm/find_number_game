package server;

import common.ConnectionHandler;
import common.Message;
import common.User;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.List;

/**
 * Handles individual client connections
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final GameServer server;
    private ConnectionHandler connection;
    private User user;
    private Game currentGame;
    private boolean isRunning = true;

    public ClientHandler(Socket socket, GameServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            connection = new ConnectionHandler(clientSocket);
            System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

            // Process messages from the client
            while (isRunning) {
                Message message = connection.receiveMessage();
                processMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            // Clean up resources
            if (user != null && currentGame != null) {
                currentGame.removePlayer(user.getId());
            }
            if (connection != null) {
                connection.close();
            }
            server.removeClient(this);
        }
    }

    private void processMessage(Message message) throws IOException {
        String messageType = message.getType();

        switch (messageType) {
            case Message.LOGIN:
                handleLogin(message);
                break;
            case Message.REGISTER:
                handleRegister(message);
                break;
            case Message.FIND_GAME:
                handleFindGame();
                break;
            case Message.START_GAME:
                // When a player confirms ready to start, notify the game
                if (currentGame != null) {
                    currentGame.handleStartConfirmation(user.getId());
                }
                break;
            case Message.NUMBER_FOUND:
                handleNumberFound(message);
                break;
            case Message.USE_POWERUP:
                handlePowerup(message);
                break;
            case Message.DECLINE_GAME:
                if (currentGame != null) {
                    currentGame.handleDecline(user.getId());
                }
                break;
            default:
                System.err.println("Unknown message type: " + messageType);
                break;
        }
    }

    private void handleLogin(Message message) throws IOException {
        String username = message.getString("username");
        String password = message.getString("password");

        User authenticatedUser = server.getDatabaseManager().authenticateUser(username, password);

        Message response = new Message(Message.LOGIN_RESPONSE);
        if (authenticatedUser != null) {
            // Prevent concurrent logins for the same user
            if (server.isUserLoggedIn(username)) {
                response.put("success", false);
                response.put("error", "User already logged in from another client.");
            } else {
                this.user = authenticatedUser;
                response.put("success", true);
                response.put("user", authenticatedUser);

                // Send leaderboard data
                List<User> leaderboard = server.getDatabaseManager().getLeaderboard();
                response.put("leaderboard", leaderboard);

                // Remove automatic game joining - players will join only when clicking "Find
                // Game"
            }
        } else {
            response.put("success", false);
            response.put("error", "Invalid username or password.");
        }

        sendMessage(response);
    }

    private void handleRegister(Message message) throws IOException {
        String username = message.getString("username");
        String password = message.getString("password");

        // Get the additional fields if they exist
        String email = null;
        String sex = null;
        Date dateOfBirth = null;

        if (message.getData().containsKey("email")) {
            email = message.getString("email");
        }

        if (message.getData().containsKey("sex")) {
            sex = message.getString("sex");
        }

        if (message.getData().containsKey("dateOfBirth")) {
            dateOfBirth = (Date) message.get("dateOfBirth");
        }

        // Create user with all available information
        User newUser;
        if (email != null || sex != null || dateOfBirth != null) {
            newUser = new User(username, password, email, sex, dateOfBirth);
        } else {
            newUser = new User(username, password);
        }

        boolean registered = server.getDatabaseManager().registerUser(newUser);

        Message response = new Message(Message.LOGIN_RESPONSE);
        if (registered) {
            this.user = newUser;
            response.put("success", true);
            response.put("user", newUser);

            // Remove automatic game joining - players will join only when clicking "Find
            // Game"
        } else {
            response.put("success", false);
            response.put("error", "Username already exists. Please choose another.");
        }

        sendMessage(response);
    }

    private void handleFindGame() throws IOException {
        if (user == null) {
            // User must be logged in to find a game
            System.err.println("FIND_GAME request from non-authenticated user");
            return;
        }

        System.out.println("User " + user.getUsername() + " is finding a game...");

        // If the user is already in a game, leave it first
        if (currentGame != null) {
            currentGame.removePlayer(user.getId());
        }

        // Find or create a game
        Game game = server.findOrCreateGame();
        boolean joined = game.addPlayer(this);

        if (joined) {
            currentGame = game;
            System.out.println("User " + user.getUsername() + " joined game " + game.getGameId() +
                    " (Current players: " + game.getPlayerCount() + "/" + game.getMaxPlayers() + ")");

            // Server will automatically start the game when enough players join (in
            // Game.addPlayer)
            // The client will wait for PLAYER_JOINED or START_GAME messages
        } else {
            System.err.println("Could not add user " + user.getUsername() + " to game");

            // Send error message to client
            Message errorMsg = new Message(Message.ERROR);
            errorMsg.put("message", "Could not join a game at this time. Please try again.");
            sendMessage(errorMsg);
        }
    }

    private void handleNumberFound(Message message) throws IOException {
        if (user == null || currentGame == null) {
            return;
        }

        int number = message.getInt("number");
        boolean success = currentGame.processNumberFound(user.getId(), number);

        if (!success) {
            // Send feedback only when it's incorrect
            Message response = new Message("INCORRECT_NUMBER");
            response.put("number", number);
            sendMessage(response);
        }
    }

    private void handlePowerup(Message message) {
        if (user == null || currentGame == null) {
            return;
        }

        String powerupType = message.getString("type");
        currentGame.usePowerup(user.getId(), powerupType);
    }

    public void sendMessage(Message message) throws IOException {
        if (connection != null && connection.isConnected()) {
            connection.sendMessage(message);
        }
    }

    public User getUser() {
        return user;
    }

    public Game getCurrentGame() {
        return currentGame;
    }

    public void setCurrentGame(Game game) {
        this.currentGame = game;
    }

    public void stop() {
        isRunning = false;
        if (connection != null) {
            connection.close();
        }
    }
}