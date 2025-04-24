package client;

import common.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Login frame for user authentication
 */
public class LoginFrame extends JFrame {
    private final GameClient client;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;

    public LoginFrame(GameClient client) {
        this.client = client;

        setTitle("Find the Number - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        initializeComponents();
    }

    private void initializeComponents() {
        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title
        JLabel titleLabel = new JLabel("Find the Number Game", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Form panel
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        // Form components
        JLabel usernameLabel = new JLabel("Username:", SwingConstants.RIGHT);
        usernameField = new JTextField(15);

        JLabel passwordLabel = new JLabel("Password:", SwingConstants.RIGHT);
        passwordField = new JPasswordField(15);

        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));

        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        // Set preferred size for buttons to ensure they're visible
        loginButton.setPreferredSize(new Dimension(100, 30));
        registerButton.setPreferredSize(new Dimension(100, 30));

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        // Status label
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components to main panel
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing
        mainPanel.add(formPanel);
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing
        mainPanel.add(statusLabel);

        // Add main panel to frame
        add(mainPanel);

        // Set up action listeners
        setupListeners();
    }

    private void setupListeners() {
        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> handleRegisterClick());

        // Allow pressing Enter to login
        passwordField.addActionListener(e -> handleLogin());
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Basic validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password cannot be empty");
            return;
        }

        // Disable buttons while logging in
        loginButton.setEnabled(false);
        registerButton.setEnabled(false);
        statusLabel.setText("Logging in...");
        statusLabel.setForeground(Color.BLUE);

        // Send login request
        client.sendLogin(username, password);
    }

    private void handleRegisterClick() {
        // Show registration dialog
        RegistrationDialog dialog = new RegistrationDialog(this, client);
        dialog.setVisible(true);

        // Check if registration was completed
        if (dialog.isRegistered()) {
            // Get the registration data
            String username = dialog.getUsername();
            String password = dialog.getPassword();
            String email = dialog.getEmail();
            String sex = dialog.getSex();
            java.util.Date dateOfBirth = dialog.getDateOfBirth();

            // Disable buttons while registering
            loginButton.setEnabled(false);
            registerButton.setEnabled(false);
            statusLabel.setText("Registering...");
            statusLabel.setForeground(Color.BLUE);

            // Send register request with all data
            client.sendRegister(username, password, email, sex, dateOfBirth);
        }
    }

    public void showError(String error) {
        statusLabel.setText(error);
        statusLabel.setForeground(Color.RED);

        // Re-enable buttons
        loginButton.setEnabled(true);
        registerButton.setEnabled(true);
    }
}