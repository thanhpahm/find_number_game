package client.game;

import javax.swing.SwingUtilities;

public class GameMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String playerName = "Player1"; // Example player name
            GameClient client = new GameClient("localhost", 5000, playerName);
            GameFrame gameFrame = new GameFrame();
            gameFrame.setGameClient(client);
            client.setGameFrame(gameFrame);
            gameFrame.setVisible(true);
            gameFrame.startGame();
        });
    }
}
