package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import model.User;
import shared.GameFeatures;
import shared.GameState;

public class GameServer {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private Map<String, GameMatch> matches;
    private DatabaseManager dbManager;
    private RankingSystem rankingSystem;
    private ExecutorService executor;
    private ScheduledExecutorService scheduler;

    public GameServer() {
        this.clients = new CopyOnWriteArrayList<>();
        this.matches = new ConcurrentHashMap<>();
        this.dbManager = new DatabaseManager();
        this.rankingSystem = new RankingSystem(dbManager);
        this.executor = Executors.newCachedThreadPool();
        this.scheduler = Executors.newScheduledThreadPool(1);
        startMaintenanceTasks();
    }

    private void startMaintenanceTasks() {
        // Clean up abandoned matches every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            matches.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }, 5, 5, TimeUnit.MINUTES);

        // Update rankings every minute
        scheduler.scheduleAtFixedRate(() -> {
            updateGlobalRankings();
        }, 1, 1, TimeUnit.MINUTES);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Game server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler client = new ClientHandler(clientSocket, this);
                clients.add(client);
                executor.execute(client);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public void findGame(ClientHandler client) {
        // First check if player is already in a match
        for (GameMatch match : matches.values()) {
            if (match.hasPlayer(client)) {
                return;
            }
        }

        // Look for available match or create new one
        GameMatch availableMatch = matches.values().stream()
                .filter(m -> !m.isStarted() && m.getPlayerCount() < GameFeatures.MAX_PLAYERS)
                .findFirst()
                .orElseGet(() -> {
                    GameMatch newMatch = new GameMatch();
                    matches.put(UUID.randomUUID().toString(), newMatch);
                    return newMatch;
                });

        if (availableMatch.addPlayer(client)) {
            if (availableMatch.getPlayerCount() >= GameFeatures.MIN_PLAYERS) {
                availableMatch.startGame();
            }
        }
    }

    public void handleNumberClick(int number, ClientHandler client) {
        GameMatch match = findPlayerMatch(client);
        if (match != null) {
            if (match.processNumberFound(number, client)) {
                // Check for lucky number bonus
                if (GameFeatures.isLuckyNumber(number)) {
                    client.getUser().addLuckyNumber();
                    updateUserPowerUps(client.getUser());
                }
            }
        }
    }

    public void handlePowerUpUse(ClientHandler client, User.PowerUpType type) {
        if (!client.getUser().usePowerUp(type)) {
            client.sendError("Power-up not available");
            return;
        }

        GameMatch match = findPlayerMatch(client);
        if (match != null) {
            match.handlePowerUp(type, client);
            updateUserPowerUps(client.getUser());
            dbManager.updatePowerUpUseTime(client.getUser().getId(), type.name(), System.currentTimeMillis());
        }
    }

    private void updateUserPowerUps(User user) {
        dbManager.updateUserPowerUps(user.getId(), user.getPowerUps().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue)));
    }

    private GameMatch findPlayerMatch(ClientHandler client) {
        return matches.values().stream()
                .filter(m -> m.hasPlayer(client))
                .findFirst()
                .orElse(null);
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        GameMatch match = findPlayerMatch(client);
        if (match != null) {
            match.handlePlayerDisconnect(client);
        }
    }

    public boolean addNewUser(String email, String password) {
        return dbManager.addUser(email, password);
    }

    public boolean verifyLogin(String email, String password) {
        return dbManager.verifyLogin(email, password);
    }

    private void updateGlobalRankings() {
        for (ClientHandler client : clients) {
            User user = client.getUser();
            if (user != null) {
                rankingSystem.updatePlayerRanking(user.getUsername(), user.getStats());
            }
        }
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    public RankingSystem getRankingSystem() {
        return rankingSystem;
    }

    private void shutdown() {
        try {
            for (ClientHandler client : clients) {
                client.stop();
            }
            executor.shutdown();
            scheduler.shutdown();
            if (serverSocket != null) {
                serverSocket.close();
            }
            dbManager.close();
        } catch (IOException e) {
            System.err.println("Error shutting down server: " + e.getMessage());
        }
    }
}
