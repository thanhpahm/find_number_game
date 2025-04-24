package client;

import common.ConnectionHandler;
import common.Message;
import common.User;

import javax.swing.*;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main client class that manages connection to server and UI
 */
public class GameClient {
    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final int DEFAULT_SERVER_PORT = 12345;

    private final String serverHost;
    private final int serverPort;

    private ConnectionHandler connection;
    private User currentUser;
    private boolean isConnected = false;
    private ExecutorService messageProcessor;

    // Store game details received on login
    private int initialPlayerCount = 0;
    private int maxPlayers = 0;

    private LoginFrame loginFrame;
    private LobbyFrame lobbyFrame;
    private GameFrame gameFrame;

    public GameClient() {
        this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
    }

    public GameClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.messageProcessor = Executors.newSingleThreadExecutor();
    }

    public void start() {
        try {
            // Connect to the server
            System.out.println("Connecting to server at " + serverHost + ":" + serverPort);
            Socket socket = new Socket(serverHost, serverPort);
            connection = new ConnectionHandler(socket);
            isConnected = true;
            System.out.println("Connected successfully to " + serverHost);

            // Start message listening thread
            messageProcessor.submit(this::processServerMessages);

            // Show login frame
            SwingUtilities.invokeLater(() -> {
                loginFrame = new LoginFrame(this);
                loginFrame.setVisible(true);
            });

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Could not connect to server at " + serverHost + ":" + serverPort + "\n" + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void processServerMessages() {
        try {
            while (isConnected) {
                Message message = connection.receiveMessage();
                handleServerMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Lost connection to server: " + e.getMessage());
            disconnect("Lost connection to server: " + e.getMessage());
        }
    }

    private void handleLoginResponse(Message message) {
        boolean success = message.getBoolean("success");

        if (success) {
            currentUser = (User) message.get("user");

            // Store initial game state if joined
            if (message.getData().containsKey("initialPlayerCount")) {
                initialPlayerCount = message.getInt("initialPlayerCount");
                maxPlayers = message.getInt("maxPlayers");
            }

            // Close login frame
            if (loginFrame != null) {
                loginFrame.dispose();
            }

            // Create and show the lobby frame
            lobbyFrame = new LobbyFrame(this, currentUser);

            // Update leaderboard
            List<User> leaderboard = (List<User>) message.get("leaderboard");
            lobbyFrame.updateLeaderboard(leaderboard);

            lobbyFrame.setVisible(true);
        } else {
            // Show error message
            String error = message.getString("error");
            if (loginFrame != null) {
                loginFrame.showError(error);
            }
        }
    }

    private void handleServerMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            String type = message.getType();

            switch (type) {
                case Message.LOGIN_RESPONSE:
                    handleLoginResponse(message);
                    break;
                case "PLAYER_JOINED":
                    if (gameFrame != null) {
                        gameFrame.updateWaitingStatus(message);
                    }
                    break;
                case Message.START_GAME:
                    // Create the game frame when the START_GAME message is received
                    gameFrame = new GameFrame(this, currentUser);
                    if (lobbyFrame != null) {
                        lobbyFrame.setVisible(false); // Hide lobby frame
                    }
                    gameFrame.startGame(message);
                    gameFrame.setVisible(true);
                    break;
                case Message.NUMBER_FOUND:
                    if (gameFrame != null) {
                        gameFrame.handleNumberFound(message);
                    }
                    break;
                case "TIME_UPDATE":
                    if (gameFrame != null) {
                        gameFrame.updateTime(message.getInt("remainingSeconds"));
                    }
                    break;
                case "LUCKY_NUMBER":
                    if (gameFrame != null) {
                        gameFrame.updatePowerups(message.getInt("count"));
                    }
                    break;
                case Message.POWERUP_EFFECT:
                    if (gameFrame != null) {
                        gameFrame.handlePowerupEffect(message);
                    }
                    break;
                case Message.GAME_OVER:
                    if (gameFrame != null) {
                        // Handle game over in the game frame first (shows results dialog)
                        gameFrame.handleGameOver(message);

                        // Close the game frame
                        gameFrame.dispose();
                        gameFrame = null;

                        // Update lobby with latest leaderboard data from the GAME_OVER message
                        if (lobbyFrame != null) {
                            // Get updated leaderboard from the message
                            @SuppressWarnings("unchecked")
                            List<User> updatedLeaderboard = (List<User>) message.get("leaderboard");
                            if (updatedLeaderboard != null) {
                                lobbyFrame.updateLeaderboard(updatedLeaderboard);
                            }

                            // Reset the Find Game button and show the lobby
                            lobbyFrame.resetFindGameButton();
                            lobbyFrame.setVisible(true);
                        }
                    }
                    break;
                case "INCORRECT_NUMBER":
                    if (gameFrame != null) {
                        gameFrame.handleIncorrectNumber(message.getInt("number"));
                    }
                    break;
                case Message.UPDATE_LEADERBOARD:
                    // Update leaderboard in lobby if visible
                    if (lobbyFrame != null && lobbyFrame.isVisible()) {
                        lobbyFrame.updateLeaderboard((List<User>) message.get("leaderboard"));
                    }
                    break;
            }
        });
    }

    public void sendLogin(String username, String password) {
        try {
            Message loginMsg = new Message(Message.LOGIN);
            loginMsg.put("username", username);
            loginMsg.put("password", password);
            connection.sendMessage(loginMsg);
        } catch (IOException e) {
            System.err.println("Error sending login request: " + e.getMessage());
        }
    }

    public void sendRegister(String username, String password) {
        sendRegister(username, password, null, null, null);
    }

    public void sendRegister(String username, String password, String email, String sex, Date dateOfBirth) {
        try {
            Message registerMsg = new Message(Message.REGISTER);
            registerMsg.put("username", username);
            registerMsg.put("password", password);

            // Add additional fields if provided
            if (email != null && !email.trim().isEmpty()) {
                registerMsg.put("email", email);
            }

            if (sex != null) {
                registerMsg.put("sex", sex);
            }

            if (dateOfBirth != null) {
                registerMsg.put("dateOfBirth", dateOfBirth);
            }

            connection.sendMessage(registerMsg);
        } catch (IOException e) {
            System.err.println("Error sending register request: " + e.getMessage());
        }
    }

    public void sendStartGame() {
        try {
            Message startMsg = new Message(Message.START_GAME);
            connection.sendMessage(startMsg);
        } catch (IOException e) {
            System.err.println("Error sending start game request: " + e.getMessage());
        }
    }

    public void sendNumberFound(int number) {
        try {
            Message foundMsg = new Message(Message.NUMBER_FOUND);
            foundMsg.put("number", number);
            connection.sendMessage(foundMsg);
        } catch (IOException e) {
            System.err.println("Error sending number found: " + e.getMessage());
        }
    }

    public void sendFindGame() {
        try {
            Message findGameMsg = new Message(Message.FIND_GAME);
            connection.sendMessage(findGameMsg);
        } catch (IOException e) {
            System.err.println("Error sending find game request: " + e.getMessage());
        }
    }

    public void usePowerup(String powerupType) {
        try {
            Message powerupMsg = new Message(Message.USE_POWERUP);
            powerupMsg.put("type", powerupType);
            connection.sendMessage(powerupMsg);
        } catch (IOException e) {
            System.err.println("Error sending powerup usage: " + e.getMessage());
        }
    }

    public void disconnect(String message) {
        isConnected = false;

        // Close the connection
        if (connection != null) {
            connection.close();
        }

        // Shut down the message processor
        if (messageProcessor != null) {
            messageProcessor.shutdown();
        }

        // Show error message and exit
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(gameFrame != null ? gameFrame : loginFrame,
                    message,
                    "Connection Lost",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        });
    }

    public User getCurrentUser() {
        return currentUser;
    }

    // Add getters for the stored game details
    public int getInitialPlayerCount() {
        return initialPlayerCount;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }

        // Process command-line arguments for server connection
        String host = DEFAULT_SERVER_HOST;
        int port = DEFAULT_SERVER_PORT;

        if (args.length >= 1) {
            host = args[0];
        }

        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1] + ". Using default port " + DEFAULT_SERVER_PORT);
            }
        }

        // Create and start the client with the specified connection parameters
        final String serverHost = host;
        final int serverPort = port;

        SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient(serverHost, serverPort);
            client.start();
        });
    }
}