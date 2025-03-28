import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete Registration System
 * Combines database, data model, and GUI in one file
 * Implements all features from the screenshot
 */
public class CombinedRegistrationSystem extends JFrame {
    // Database configuration
    private static final String DB_URL = "jdbc:sqlite:registrations.db";
    private Connection dbConnection;
    
    // Form components
    private JTextField nameField, mobileField, dobField, addressField, contactField;
    private JRadioButton maleRadio, femaleRadio;
    private JCheckBox termsCheckBox;
    private JButton submitButton, resetButton, hideButton;
    private JLabel timeLabel;
    
    // Table components
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton registerButton, exitButton;
    
    // Timer
    private Timer timer;
    private long startTime;

    public CombinedRegistrationSystem() {
        initializeDatabase();
        setupUI();
        startTimer();
    }

    // Database methods
    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            dbConnection = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database initialization failed: " + e.getMessage(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "name TEXT NOT NULL, " +
                     "mobile TEXT, " +
                     "gender TEXT, " +
                     "dob TEXT, " +
                     "address TEXT, " +
                     "contact TEXT, " +
                     "terms_accepted BOOLEAN, " +
                     "reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        
        try (Statement stmt = dbConnection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private boolean saveRegistration(String name, String mobile, String gender, 
                                   String dob, String address, String contact, 
                                   boolean termsAccepted) {
        String sql = "INSERT INTO users(name, mobile, gender, dob, address, contact, terms_accepted) " +
                     "VALUES(?,?,?,?,?,?,?)";
        
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, mobile);
            pstmt.setString(3, gender);
            pstmt.setString(4, dob);
            pstmt.setString(5, address);
            pstmt.setString(6, contact);
            pstmt.setBoolean(7, termsAccepted);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error saving registration: " + e.getMessage());
            return false;
        }
    }

    private List<Object[]> getAllRegistrations() {
        List<Object[]> registrations = new ArrayList<>();
        String sql = "SELECT id, name, gender, address, contact FROM users ORDER BY reg_date DESC";
        
        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("gender"),
                    rs.getString("address"),
                    rs.getString("contact")
                };
                registrations.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching registrations: " + e.getMessage());
        }
        
        return registrations;
    }

    // UI Setup
    private void setupUI() {
        setTitle("Registration System");
        setSize(800, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Create form panel
        JPanel formPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Registration Form"));

        // Name field
        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        // Mobile field
        formPanel.add(new JLabel("Mobile:"));
        mobileField = new JTextField();
        formPanel.add(mobileField);

        // Gender field
        formPanel.add(new JLabel("Gender:"));
        JPanel genderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maleRadio = new JRadioButton("Male");
        femaleRadio = new JRadioButton("Female");
        ButtonGroup genderGroup = new ButtonGroup();
        genderGroup.add(maleRadio);
        genderGroup.add(femaleRadio);
        genderPanel.add(maleRadio);
        genderPanel.add(femaleRadio);
        formPanel.add(genderPanel);

        // DOB field
        formPanel.add(new JLabel("DOB:"));
        dobField = new JTextField();
        formPanel.add(dobField);

        // Address field
        formPanel.add(new JLabel("Address:"));
        addressField = new JTextField();
        formPanel.add(addressField);

        // Contact field
        formPanel.add(new JLabel("Contact:"));
        contactField = new JTextField();
        formPanel.add(contactField);

        // Buttons and terms panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // Terms checkbox
        JPanel termsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        termsCheckBox = new JCheckBox("Accept Terms And Conditions.");
        termsPanel.add(termsCheckBox);
        bottomPanel.add(termsPanel, BorderLayout.NORTH);

        // Action buttons
        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        submitButton = new JButton("Submit");
        resetButton = new JButton("Reset");
        hideButton = new JButton("Hide");
        
        submitButton.addActionListener(e -> submitForm());
        resetButton.addActionListener(e -> resetForm());
        hideButton.addActionListener(e -> toggleTableVisibility());
        
        actionButtons.add(submitButton);
        actionButtons.add(resetButton);
        actionButtons.add(hideButton);
        bottomPanel.add(actionButtons, BorderLayout.CENTER);

        // Timer panel
        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        timeLabel = new JLabel("Time left: 00:00:00");
        timerPanel.add(timeLabel);
        bottomPanel.add(timerPanel, BorderLayout.SOUTH);

        // Create table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Registration Form"));
        
        // Table model
        tableModel = new DefaultTableModel(new Object[]{"ID", "Name", "Gender", "Address", "Contact"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        userTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(userTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Table buttons
        JPanel tableButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        registerButton = new JButton("Register");
        exitButton = new JButton("Exit");
        
        registerButton.addActionListener(e -> showTable());
        exitButton.addActionListener(e -> System.exit(0));
        
        tableButtons.add(registerButton);
        tableButtons.add(exitButton);
        tablePanel.add(tableButtons, BorderLayout.SOUTH);

        // Add components to main frame
        add(formPanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.SOUTH);

        // Load existing data
        loadRegistrations();
    }

    // Form handling methods
    private void submitForm() {
        if (!termsCheckBox.isSelected()) {
            JOptionPane.showMessageDialog(this, "Please accept the terms and conditions", 
                                         "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required", 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String gender = maleRadio.isSelected() ? "Male" : 
                      femaleRadio.isSelected() ? "Female" : null;
        
        boolean success = saveRegistration(
            name,
            mobileField.getText().trim(),
            gender,
            dobField.getText().trim(),
            addressField.getText().trim(),
            contactField.getText().trim(),
            true
        );

        if (success) {
            JOptionPane.showMessageDialog(this, "Registration successful!", 
                                       "Success", JOptionPane.INFORMATION_MESSAGE);
            resetForm();
            loadRegistrations();
        } else {
            JOptionPane.showMessageDialog(this, "Registration failed", 
                                       "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetForm() {
        nameField.setText("");
        mobileField.setText("");
        maleRadio.setSelected(false);
        femaleRadio.setSelected(false);
        dobField.setText("");
        addressField.setText("");
        contactField.setText("");
        termsCheckBox.setSelected(false);
    }

    private void loadRegistrations() {
        tableModel.setRowCount(0); // Clear existing data
        List<Object[]> registrations = getAllRegistrations();
        for (Object[] row : registrations) {
            tableModel.addRow(row);
        }
    }

    private void toggleTableVisibility() {
        Component tablePanel = ((BorderLayout)getContentPane().getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        boolean visible = tablePanel.isVisible();
        tablePanel.setVisible(!visible);
        hideButton.setText(visible ? "Show" : "Hide");
        revalidate();
    }

    private void showTable() {
        Component tablePanel = ((BorderLayout)getContentPane().getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        tablePanel.setVisible(true);
        hideButton.setText("Hide");
        revalidate();
    }

    // Timer methods
    private void startTimer() {
        startTime = System.currentTimeMillis();
        timer = new Timer(1000, e -> updateTimer());
        timer.start();
    }

    private void updateTimer() {
        long elapsed = System.currentTimeMillis() - startTime;
        long hours = (elapsed / (1000 * 60 * 60)) % 24;
        long minutes = (elapsed / (1000 * 60)) % 60;
        long seconds = (elapsed / 1000) % 60;
        
        // Display as countdown from 24 hours
        timeLabel.setText(String.format("Time left: %02d:%02d:%02d", 
                             23 - hours, 59 - minutes, 59 - seconds));
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new CombinedRegistrationSystem().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}