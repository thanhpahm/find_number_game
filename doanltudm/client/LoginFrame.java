package client;

import javax.swing.*;
import java.awt.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import client.game.ClientLobbyFrame;
import client.game.GameClient; // Add this import

public class LoginFrame extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;

    public LoginFrame() {
        setTitle("LOG IN");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);
        getContentPane().setBackground(Color.WHITE);

        // Title
        JLabel titleLabel = new JLabel("LOG IN");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBounds(0, 50, 400, 40);
        add(titleLabel);

        JPanel formPanel = new JPanel(null);
        formPanel.setBackground(Color.WHITE);
        formPanel.setBounds(50, 120, 300, 300);

        // Username label and field
        JLabel userLabel = new JLabel("User Name");
        userLabel.setBounds(0, 0, 300, 20);
        formPanel.add(userLabel);

        emailField = new JTextField();
        styleTextField(emailField); // Add this line
        emailField.setBounds(0, 25, 300, 35);
        formPanel.add(emailField);

        // Password label and field
        JLabel passLabel = new JLabel("Password");
        passLabel.setBounds(0, 70, 300, 20);
        formPanel.add(passLabel);

        passwordField = new JPasswordField();
        styleTextField(passwordField); // Add this line
        passwordField.setBounds(0, 95, 300, 35);
        formPanel.add(passwordField);

        // Login button
        loginButton = new JButton("Login");
        styleButton(loginButton);
        loginButton.setBounds(0, 150, 300, 35);
        formPanel.add(loginButton);

        // Register link
        JLabel registerLink = new JLabel("Register Now");
        registerLink.setForeground(new Color(51, 153, 255));
        registerLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerLink.setHorizontalAlignment(SwingConstants.CENTER);
        registerLink.setBounds(0, 200, 300, 20);
        registerLink.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                openRegisterFrame();
            }
        });
        formPanel.add(registerLink);

        add(formPanel);

        loginButton.addActionListener(e -> handleLogin());
    }

    private void styleTextField(JTextField field) {
        field.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    }

    private void styleButton(JButton button) {
        button.setBackground(new Color(51, 153, 255));
        button.setForeground(Color.WHITE);
        button.setBorder(null);
        button.setFocusPainted(false);
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all fields!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Verify login with server
        try {
            Socket socket = new Socket("localhost", 5000);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Send login request
            out.writeObject("LOGIN");
            out.writeObject(email);
            out.writeObject(password);

            // Get response
            String response = (String) in.readObject();

            if (response.equals("LOGIN_SUCCESS")) {
                SwingUtilities.invokeLater(() -> {
                    GameClient gameClient = new GameClient("localhost", 5000, email);
                    ClientLobbyFrame lobbyFrame = new ClientLobbyFrame(email, gameClient);
                    gameClient.setLobbyFrame(lobbyFrame);
                    lobbyFrame.setVisible(true);
                    this.dispose();
                });
            } else {
                JOptionPane.showMessageDialog(this, "Login failed!");
            }

            socket.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage());
        }
    }

    private void openRegisterFrame() {
        new RegisterFrame().setVisible(true);
        this.dispose();
    }

    public static void main(String[] args) {
        // Run GUI on Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
