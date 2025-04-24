package server;

import java.net.*;
import java.io.*;
import model.User;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private GameServer gameServer;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String playerName;
    private int score;
    private boolean isRunning;

    public ClientHandler(Socket socket, GameServer gameServer) {
        this.socket = socket;
        this.gameServer = gameServer;
        this.score = 0;
        this.isRunning = true;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (isRunning) {
                Object message = in.readObject();
                handleClientMessage(message);
            }
        } catch (Exception e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handleClientMessage(Object message) throws IOException {
        if (message instanceof String) {
            String command = (String) message;
            try {
                switch (command) {
                    case "REGISTER":
                        handleRegister();
                        break;
                    case "LOGIN":
                        handleLogin();
                        break;
                    case "FIND_GAME":
                        handleFindGame();
                        break;
                    case "NUMBER_CLICK":
                        handleNumberClick();
                        break;
                    case "GET_LEADERBOARD":
                        sendLeaderboard();
                        break;
                    case "GET_PLAYER_STATS":
                        sendPlayerStats();
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error handling message: " + e.getMessage());
                sendError("Internal server error");
            }
        }
    }

    private void handleRegister() throws IOException, ClassNotFoundException {
        User newUser = (User) in.readObject();
        boolean success = gameServer.addNewUser(newUser.getEmail(), newUser.getPasswordHash());
        sendMessage(success ? "REGISTER_SUCCESS" : "REGISTER_FAILED:Email already exists");
    }

    private void handleLogin() throws IOException, ClassNotFoundException {
        String email = (String) in.readObject();
        String password = (String) in.readObject();

        if (gameServer.verifyLogin(email, password)) {
            this.playerName = email;
            sendMessage("LOGIN_SUCCESS");
        } else {
            sendMessage("LOGIN_FAILED");
        }
    }

    private void handleFindGame() {
        gameServer.findGame(this);
    }

    private void handleNumberClick() throws IOException, ClassNotFoundException {
        int number = (Integer) in.readObject();
        String clickingPlayer = (String) in.readObject();
        if (clickingPlayer.equals(playerName)) {
            gameServer.handleNumberClick(number, this);
        }
    }

    private void sendLeaderboard() throws IOException {
        sendMessage("LEADERBOARD_DATA");
        sendObject(gameServer.getDatabaseManager().getLeaderboard());
    }

    private void sendPlayerStats() throws IOException, ClassNotFoundException {
        String targetPlayer = (String) in.readObject();
        // Get player stats from database and send
        // Implementation depends on your database structure
        // This is a placeholder response
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGames", 0);
        stats.put("winRate", 0.0);
        stats.put("rank", 1);
        sendMessage("PLAYER_STATS");
        sendObject(stats);
    }

    public void sendMessage(String message) throws IOException {
        out.writeObject(message);
        out.flush();
    }

    public void sendObject(Object obj) throws IOException {
        out.writeObject(obj);
        out.flush();
    }

    private void sendError(String error) {
        try {
            sendMessage("ERROR");
            sendObject(error);
        } catch (IOException e) {
            System.err.println("Error sending error message: " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            isRunning = false;
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null)
                socket.close();
            gameServer.removeClient(this);
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public void incrementScore() {
        score++;
    }
}
