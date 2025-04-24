package common;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a user in the game system
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String password;
    private String email;
    private String sex; // "M" for male, "F" for female, "O" for other
    private Date dateOfBirth;
    private int gamesWon;
    private int gamesLost;
    private int totalScore;
    private int playerColor; // Color code for the player in-game

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.gamesWon = 0;
        this.gamesLost = 0;
        this.totalScore = 0;
    }

    public User(String username, String password, String email, String sex, Date dateOfBirth) {
        this(username, password);
        this.email = email;
        this.sex = sex;
        this.dateOfBirth = dateOfBirth;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public int getGamesLost() {
        return gamesLost;
    }

    public void setGamesLost(int gamesLost) {
        this.gamesLost = gamesLost;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public void incrementGamesWon() {
        this.gamesWon++;
    }

    public void incrementGamesLost() {
        this.gamesLost++;
    }

    public void addScore(int points) {
        this.totalScore += points;
    }

    public double getWinRate() {
        int totalGames = gamesWon + gamesLost;
        if (totalGames == 0)
            return 0;
        return (double) gamesWon / totalGames;
    }

    public int getPlayerColor() {
        return playerColor;
    }

    public void setPlayerColor(int playerColor) {
        this.playerColor = playerColor;
    }

    @Override
    public String toString() {
        return username;
    }
}