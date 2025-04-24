package server;

public class GameServerMain {
    public static void main(String[] args) {
        System.out.println("Starting Game Server...");
        GameServer server = new GameServer();
        server.start();
    }
}
