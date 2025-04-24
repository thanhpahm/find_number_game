package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;

/**
 * Dialog for user registration with additional fields
 */
public class RegistrationDialog extends JDialog {
    private final GameClient client;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField emailField;
    private JComboBox<String> sexComboBox;
    private JComboBox<Integer> dayCombo;
    private JComboBox<String> monthCombo;
    private JComboBox<Integer> yearCombo;
    private JButton registerButton;
    private JButton cancelButton;

    // Result of registration
    private boolean registered = false;
    private String username;
    private String password;
    private String email;
    private String sex;
    private Date dateOfBirth;

    public RegistrationDialog(JFrame parent, GameClient client) {
        super(parent, "Register New User", true);
        this.client = client;

        setSize(450, 450);
        setLocationRelativeTo(parent);
        setResizable(false);

        initializeComponents();
    }

    private void initializeComponents() {
        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title
        JLabel titleLabel = new JLabel("User Registration", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Form panel
        JPanel formPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        // Username
        JLabel usernameLabel = new JLabel("Username:", SwingConstants.RIGHT);
        usernameField = new JTextField(15);

        // Password
        JLabel passwordLabel = new JLabel("Password:", SwingConstants.RIGHT);
        passwordField = new JPasswordField(15);

        // Confirm Password
        JLabel confirmPasswordLabel = new JLabel("Confirm Password:", SwingConstants.RIGHT);
        confirmPasswordField = new JPasswordField(15);

        // Email
        JLabel emailLabel = new JLabel("Email:", SwingConstants.RIGHT);
        emailField = new JTextField(15);

        // Sex
        JLabel sexLabel = new JLabel("Sex:", SwingConstants.RIGHT);
        String[] sexOptions = { "Male", "Female", "Other" };
        sexComboBox = new JComboBox<>(sexOptions);

        // Date of Birth
        JLabel dobLabel = new JLabel("Date of Birth:", SwingConstants.RIGHT);
        JPanel dobPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Day combo
        Integer[] days = new Integer[31];
        for (int i = 0; i < 31; i++) {
            days[i] = i + 1;
        }
        dayCombo = new JComboBox<>(days);

        // Month combo
        String[] months = { "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December" };
        monthCombo = new JComboBox<>(months);

        // Year combo - let's allow years from 1950 to current year
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        Integer[] years = new Integer[currentYear - 1950 + 1];
        for (int i = 0; i < years.length; i++) {
            years[i] = currentYear - i;
        }
        yearCombo = new JComboBox<>(years);

        dobPanel.add(dayCombo);
        dobPanel.add(monthCombo);
        dobPanel.add(yearCombo);

        // Add components to form panel
        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);
        formPanel.add(confirmPasswordLabel);
        formPanel.add(confirmPasswordField);
        formPanel.add(emailLabel);
        formPanel.add(emailField);
        formPanel.add(sexLabel);
        formPanel.add(sexComboBox);
        formPanel.add(dobLabel);
        formPanel.add(dobPanel);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));

        registerButton = new JButton("Register");
        cancelButton = new JButton("Cancel");

        registerButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.setPreferredSize(new Dimension(100, 30));

        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        // Add components to main panel
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(buttonPanel);

        // Add main panel to dialog
        add(mainPanel);

        // Set up action listeners
        setupListeners();
    }

    private void setupListeners() {
        registerButton.addActionListener(e -> handleRegister());
        cancelButton.addActionListener(e -> dispose());
    }

    private void handleRegister() {
        // Get values from form
        username = usernameField.getText().trim();
        password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        email = emailField.getText().trim();

        // Get sex value
        String sexOption = (String) sexComboBox.getSelectedItem();
        if ("Male".equals(sexOption)) {
            sex = "M";
        } else if ("Female".equals(sexOption)) {
            sex = "F";
        } else {
            sex = "O";
        }

        // Create date of birth from selected values
        int day = (Integer) dayCombo.getSelectedItem();
        int month = monthCombo.getSelectedIndex(); // January is 0
        int year = (Integer) yearCombo.getSelectedItem();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        dateOfBirth = calendar.getTime();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Username and password cannot be empty",
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "Passwords do not match",
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (username.length() < 3 || username.length() > 20) {
            JOptionPane.showMessageDialog(this,
                    "Username must be between 3 and 20 characters",
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (password.length() < 4) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 4 characters long",
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Email validation
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid email address",
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Set result and close dialog
        registered = true;
        dispose();
    }

    public boolean isRegistered() {
        return registered;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getSex() {
        return sex;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }
}