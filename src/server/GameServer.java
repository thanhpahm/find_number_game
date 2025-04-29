package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main server class that manages client connections and games
 */
public class GameServer {
    private static final int DEFAULT_PORT = 12345;
    private final int port;
    private final DatabaseManager dbManager;
    private ServerSocket serverSocket;
    private final ExecutorService clientThreadPool;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Integer, Game> activeGames = new ConcurrentHashMap<>();
    private final AtomicInteger nextGameId = new AtomicInteger(1);
    private boolean isRunning = true;

    public GameServer() {
        this(DEFAULT_PORT);
    }

    public GameServer(int port) {
        this.port = port;
        this.dbManager = new DatabaseManager();
        this.clientThreadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            // Bind to all available network interfaces (0.0.0.0) instead of just localhost
            serverSocket = new ServerSocket(port, 50, null);

            System.out.println("Server started on port " + port);
            System.out.println("Server IP address(es):");

            // Print all server IP addresses for connection information
            try {
                java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface
                        .getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    java.net.NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp())
                        continue;

                    java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        java.net.InetAddress addr = addresses.nextElement();
                        if (addr instanceof java.net.Inet4Address) {
                            System.out.println("  - " + iface.getDisplayName() + ": " + addr.getHostAddress());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("  - Unable to determine IP addresses");
            }

            while (isRunning) {
                try {
                    // Wait for new client connections
                    Socket clientSocket = serverSocket.accept();

                    // Create a handler for the new client
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    clients.add(clientHandler);

                    // Start handling the client in a separate thread
                    clientThreadPool.submit(clientHandler);
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port " + port + ": " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public synchronized Game findOrCreateGame() {
        // First, try to find an existing game that isn't full or active
        for (Game game : activeGames.values()) {
            if (!game.isActive() && game.getPlayerCount() < 3) {
                return game;
            }
        }

        // If none found, create a new game
        int gameId = nextGameId.getAndIncrement();
        Game newGame = new Game(gameId, dbManager);
        activeGames.put(gameId, newGame);
        return newGame;
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    // New method to get currently connected clients
    public List<ClientHandler> getClients() {
        return clients;
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    public void shutdown() {
        isRunning = false;

        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        // Stop all client handlers
        for (ClientHandler client : clients) {
            client.stop();
        }
        clients.clear();

        // Shutdown thread pool
        clientThreadPool.shutdown();

        // Close database connection
        dbManager.close();

        System.out.println("Server shutdown complete");
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // Allow port to be specified as a command line argument
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0] + ". Using default port " + DEFAULT_PORT);
            }
        }

        GameServer server = new GameServer(port);
        server.start();
    }

    // Checks if a user is already logged in
    public boolean isUserLoggedIn(String username) {
        for (ClientHandler client : clients) {
            if (client.getUser() != null && client.getUser().getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }
}