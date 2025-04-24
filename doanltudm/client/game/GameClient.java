package client.game;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import shared.GameState;
import shared.GameFeatures;
import model.User;

public class GameClient {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User currentUser;
    private GameFrame gameFrame;
    private boolean isConnected;
    private ExecutorService messageHandler;
    private GameState currentGameState;
    private Map<String, Player> players;
    private BlockingQueue<Object> messageQueue;

    public GameClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.isConnected = true;
        this.players = new ConcurrentHashMap<>();
        this.messageQueue = new LinkedBlockingQueue<>();
        this.messageHandler = Executors.newSingleThreadExecutor();
        startMessageProcessor();
    }

    private void startMessageProcessor() {
        messageHandler.submit(() -> {
            while (isConnected) {
                try {
                    Object message = in.readObject();
                    messageQueue.put(message);
                    processMessage(message);
                } catch (Exception e) {
                    handleDisconnection(e);
                }
            }
        });
    }

    private void processMessage(Object message) {
        try {
            if (message instanceof String) {
                String command = (String) message;
                switch (command) {
                    case "GAME_START":
                        handleGameStart();
                        break;
                    case "GAME_END":
                        handleGameEnd();
                        break;
                    case "NUMBER_FOUND":
                        handleNumberFound();
                        break;
                    case "PLAYER_JOINED":
                        handlePlayerJoined();
                        break;
                    case "PLAYER_LEFT":
                        handlePlayerLeft();
                        break;
                    case "LUCKY_NUMBER":
                        handleLuckyNumber();
                        break;
                    case "TIME_WARNING":
                        handleTimeWarning();
                        break;
                    case "TIME_UPDATE":
                        handleTimeUpdate();
                        break;
                    case "ERROR":
                        handleError();
                        break;
                }
            } else if (message instanceof GameState) {
                updateGameState((GameState) message);
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    private void handleGameStart() throws IOException, ClassNotFoundException {
        GameState gameState = (GameState) in.readObject();
        currentGameState = gameState;
        if (gameFrame != null) {
            gameFrame.startGame(gameState);
        }
    }

    private void handleGameEnd() throws IOException, ClassNotFoundException {
        Map<String, Object> results = (Map<String, Object>) in.readObject();
        if (gameFrame != null) {
            gameFrame.showGameResults(results);
        }
    }

    private void handleNumberFound() throws IOException, ClassNotFoundException {
        Map<String, Object> data = (Map<String, Object>) in.readObject();
        int number = (Integer) data.get("number");
        String playerName = (String) data.get("playerName");
        int nextTarget = (Integer) data.get("nextTarget");

        Player player = players.get(playerName);
        if (player != null) {
            player.incrementScore();
            if (GameFeatures.isLuckyNumber(number)) {
                player.addLuckyNumber();
            }
        }

        if (gameFrame != null) {
            gameFrame.updateNumber(number, playerName);
            gameFrame.setTargetNumber(nextTarget);
        }
    }

    private void handlePlayerJoined() throws IOException, ClassNotFoundException {
        Map<String, Object> data = (Map<String, Object>) in.readObject();
        String playerName = (String) data.get("playerName");
        int colorRGB = (Integer) data.get("color");

        Player newPlayer = new Player(playerName, new java.awt.Color(colorRGB));
        players.put(playerName, newPlayer);

        if (gameFrame != null) {
            gameFrame.addPlayer(newPlayer);
        }
    }

    private void handlePlayerLeft() throws IOException, ClassNotFoundException {
        String playerName = (String) in.readObject();
        players.remove(playerName);
        if (gameFrame != null) {
            gameFrame.removePlayer(playerName);
        }
    }

    private void handleLuckyNumber() throws IOException, ClassNotFoundException {
        int count = (Integer) in.readObject();
        if (gameFrame != null) {
            gameFrame.showLuckyNumberBonus(count);
        }
    }

    private void handleTimeWarning() throws IOException, ClassNotFoundException {
        int secondsLeft = (Integer) in.readObject();
        if (gameFrame != null) {
            gameFrame.showTimeWarning(secondsLeft);
        }
    }

    private void handleTimeUpdate() throws IOException, ClassNotFoundException {
        int remainingTime = (Integer) in.readObject();
        if (gameFrame != null) {
            gameFrame.updateTimer(remainingTime);
        }
    }

    private void handleError() throws IOException, ClassNotFoundException {
        String error = (String) in.readObject();
        if (gameFrame != null) {
            gameFrame.showError(error);
        }
    }

    private void updateGameState(GameState newState) {
        currentGameState = newState;
        if (gameFrame != null) {
            gameFrame.updateGameState(newState);
        }
    }

    public void sendNumberClick(int number) {
        try {
            out.writeObject("NUMBER_CLICK");
            out.writeObject(number);
            out.writeObject(currentUser.getUsername());
            out.flush();
        } catch (IOException e) {
            handleDisconnection(e);
        }
    }

    public void findGame() {
        try {
            out.writeObject("FIND_GAME");
            out.flush();
        } catch (IOException e) {
            handleDisconnection(e);
        }
    }

    public void login(String email, String password) {
        try {
            out.writeObject("LOGIN");
            out.writeObject(email);
            out.writeObject(password);
            out.flush();
        } catch (IOException e) {
            handleDisconnection(e);
        }
    }

    public void register(User user) {
        try {
            out.writeObject("REGISTER");
            out.writeObject(user);
            out.flush();
        } catch (IOException e) {
            handleDisconnection(e);
        }
    }

    public void requestLeaderboard() {
        try {
            out.writeObject("GET_LEADERBOARD");
            out.flush();
        } catch (IOException e) {
            handleDisconnection(e);
        }
    }

    public void requestPlayerStats(String playerName) {
        try {
            out.writeObject("GET_PLAYER_STATS");
            out.writeObject(playerName);
            out.flush();
        } catch (IOException e) {
            handleDisconnection(e);
        }
    }

    private void handleDisconnection(Exception e) {
        System.err.println("Connection lost: " + e.getMessage());
        isConnected = false;
        cleanup();
        if (gameFrame != null) {
            gameFrame.handleDisconnection();
        }
    }

    private void cleanup() {
        messageHandler.shutdown();
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    public void setGameFrame(GameFrame frame) {
        this.gameFrame = frame;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public Map<String, Player> getPlayers() {
        return new HashMap<>(players);
    }

    public GameState getCurrentGameState() {
        return currentGameState;
    }
}
