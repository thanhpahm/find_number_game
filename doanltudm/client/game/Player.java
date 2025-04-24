package client.game;

import java.io.Serializable;
import java.awt.Color;
import shared.GameFeatures;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private Color color;
    private int score;
    private int luckyNumbersFound;
    private long lastClickTime;
    private boolean isPriorityMode;
    private boolean isBlocked;
    private long blockEndTime;

    public Player(String username, Color color) {
        this.username = username;
        this.color = color;
        this.score = 0;
        this.luckyNumbersFound = 0;
        this.lastClickTime = 0;
        this.isPriorityMode = false;
        this.isBlocked = false;
    }

    public synchronized void incrementScore() {
        score++;
    }

    public synchronized void addLuckyNumber() {
        luckyNumbersFound++;
        score += GameFeatures.LUCKY_NUMBER_BONUS;
    }

    public synchronized boolean canClick() {
        if (isBlocked && System.currentTimeMillis() < blockEndTime) {
            return false;
        }
        isBlocked = false;
        return true;
    }

    public void activatePriorityMode() {
        isPriorityMode = true;
        new Thread(() -> {
            try {
                Thread.sleep(GameFeatures.PRIORITY_MODE_DURATION);
                isPriorityMode = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void block(int duration) {
        isBlocked = true;
        blockEndTime = System.currentTimeMillis() + duration;
    }

    public void unblock() {
        isBlocked = false;
    }

    public void updateClickTime() {
        lastClickTime = System.currentTimeMillis();
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public Color getColor() {
        return color;
    }

    public int getScore() {
        return score;
    }

    public int getLuckyNumbersFound() {
        return luckyNumbersFound;
    }

    public boolean isPriorityMode() {
        return isPriorityMode;
    }

    public boolean isBlocked() {
        return isBlocked && System.currentTimeMillis() < blockEndTime;
    }

    public long getLastClickTime() {
        return lastClickTime;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Player player = (Player) o;
        return username.equals(player.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public String toString() {
        return username + " (Score: " + score + ")";
    }
}
