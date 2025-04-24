package client;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.net.*;
import java.io.*;
import java.util.Date;
import model.User;

public class RegisterFrame extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField birthDateField;
    private JComboBox<String> genderCombo;
    private JButton registerButton;

    public RegisterFrame() {
        setTitle("Register");
        setSize(400, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);
        getContentPane().setBackground(Color.WHITE);

        // Title
        JLabel titleLabel = new JLabel("Register");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBounds(0, 30, 400, 40);
        add(titleLabel);

        JPanel formPanel = new JPanel(null);
        formPanel.setBackground(Color.WHITE);
        formPanel.setBounds(50, 100, 300, 510);

        // Email (as username)
        JLabel emailLabel = new JLabel("Email");
        emailLabel.setBounds(0, 0, 300, 20);
        formPanel.add(emailLabel);

        emailField = new JTextField();
        styleTextField(emailField);
        emailField.setBounds(0, 25, 300, 35);
        formPanel.add(emailField);

        // Password
        JLabel passLabel = new JLabel("Password");
        passLabel.setBounds(0, 70, 300, 20);
        formPanel.add(passLabel);

        passwordField = new JPasswordField();
        styleTextField(passwordField);
        passwordField.setBounds(0, 95, 300, 35);
        formPanel.add(passwordField);

        // Confirm Password
        JLabel confirmPassLabel = new JLabel("Confirm Password");
        confirmPassLabel.setBounds(0, 140, 300, 20);
        formPanel.add(confirmPassLabel);

        confirmPasswordField = new JPasswordField();
        styleTextField(confirmPasswordField);
        confirmPasswordField.setBounds(0, 165, 300, 35);
        formPanel.add(confirmPasswordField);

        // Birth Date
        JLabel birthLabel = new JLabel("Birth Date (dd/MM/yyyy)");
        birthLabel.setBounds(0, 210, 300, 20);
        formPanel.add(birthLabel);

        birthDateField = new JTextField();
        styleTextField(birthDateField);
        birthDateField.setBounds(0, 235, 300, 35);
        formPanel.add(birthDateField);

        // Gender
        JLabel genderLabel = new JLabel("Gender");
        genderLabel.setBounds(0, 280, 300, 20);
        formPanel.add(genderLabel);

        genderCombo = new JComboBox<>(new String[] { "Male", "Female" });
        genderCombo.setBounds(0, 305, 300, 35);
        genderCombo.setBackground(Color.WHITE);
        formPanel.add(genderCombo);

        // Register button
        registerButton = new JButton("Register");
        styleButton(registerButton);
        registerButton.setBounds(0, 360, 300, 35);
        registerButton.addActionListener(e -> handleRegister());
        formPanel.add(registerButton);

        // Back to Login link
        JLabel loginLink = new JLabel("Back to Login");
        loginLink.setForeground(new Color(51, 153, 255));
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.setHorizontalAlignment(SwingConstants.CENTER);
        loginLink.setBounds(0, 405, 300, 20);
        loginLink.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                new LoginFrame().setVisible(true);
                dispose();
            }
        });
        formPanel.add(loginLink);

        add(formPanel);
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

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    private void handleRegister() {
        try {
            // Get values and trim whitespace
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String confirmPassword = new String(confirmPasswordField.getPassword()).trim();
            String birthDate = birthDateField.getText().trim();
            String gender = (String) genderCombo.getSelectedItem();

            // Validate empty fields
            if (email.isEmpty() || password.isEmpty() ||
                    confirmPassword.isEmpty() || birthDate.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please fill in all fields!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate email format
            if (!isValidEmail(email)) {
                JOptionPane.showMessageDialog(this,
                        "Email không đúng định dạng!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Continue with existing validation
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Mật khẩu không khớp!");
                return;
            }

            // Parse date
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date birthDateParsed = dateFormat.parse(birthDate);

            registerButton.setEnabled(false);
            registerButton.setText("Processing...");

            // Connect to server
            Socket socket = null;
            try {
                socket = new Socket("localhost", 5000);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                // Create User object
                User newUser = new User(email, password, email, gender, birthDateParsed);

                // Send register request
                out.writeObject("REGISTER");
                out.writeObject(newUser);

                // Get response
                String response = (String) in.readObject();

                if (response.equals("REGISTER_SUCCESS")) {
                    JOptionPane.showMessageDialog(this,
                            "Registration successful!\nPlease login with your new account.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Switch to login screen
                    SwingUtilities.invokeLater(() -> {
                        new LoginFrame().setVisible(true);
                        this.dispose();
                    });
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Registration failed: " + response.replace("REGISTER_FAILED:", ""),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }

                socket.close();
            } finally {
                if (socket != null)
                    socket.close();
                registerButton.setEnabled(true);
                registerButton.setText("Register");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
