package common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Message class for communication between client and server
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    // Message types
    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";
    public static final String LOGIN_RESPONSE = "LOGIN_RESPONSE";
    public static final String FIND_GAME = "FIND_GAME"; // New message type for finding a game
    public static final String START_GAME = "START_GAME";
    public static final String NUMBER_FOUND = "NUMBER_FOUND";
    public static final String NEXT_NUMBER = "NEXT_NUMBER";
    public static final String GAME_OVER = "GAME_OVER";
    public static final String USE_POWERUP = "USE_POWERUP";
    public static final String POWERUP_EFFECT = "POWERUP_EFFECT";
    public static final String UPDATE_LEADERBOARD = "UPDATE_LEADERBOARD";
    public static final String ERROR = "ERROR";

    private String type;
    private Map<String, Object> data;

    public Message(String type) {
        this.type = type;
        this.data = new HashMap<>();
    }

    public String getType() {
        return type;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public boolean getBoolean(String key) {
        return (Boolean) data.get(key);
    }

    public int getInt(String key) {
        return (Integer) data.get(key);
    }

    public String getString(String key) {
        return (String) data.get(key);
    }

    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Message [type=" + type + ", data=" + data + "]";
    }
}