import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

class User {
    private String username;
    private String passwordHash;
    private boolean isAdmin;

    public User(String username, String passwordHash, boolean isAdmin) {
        this.username = username.trim();
        this.passwordHash = passwordHash;
        this.isAdmin = isAdmin;
    }

    public String getUsername() { return username; }
    public boolean isAdmin() { return isAdmin; }

    public boolean checkPassword(String pass) {
        if (passwordHash.length() < 24) {
            return false; // corrupted or old format
        }
        String saltB64 = passwordHash.substring(0, 24);
        byte[] salt = Base64.getDecoder().decode(saltB64);
        String storedHash = passwordHash.substring(24);
        return storedHash.equals(hashPassword(pass, salt));
    }

    public String toFileString() {
        return username + ":" + passwordHash + ":" + (isAdmin ? "admin" : "user") + "\n";
    }

    public static String generateHash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hash = hashPassword(password, salt);
        return saltB64 + hash;
    }

    private static String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(password.getBytes());
            for (int i = 0; i < 10000; i++) {
                md.reset();
                hashed = md.digest(hashed);
            }
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static User fromLine(String line) {
        String[] parts = line.split(":", 3);
        if (parts.length == 3) {
            return new User(parts[0], parts[1], "admin".equals(parts[2]));
        }
        return null;
    }
}

class UserManager {
    private final String userFile = "users.txt";

    public UserManager() {
        File file = new File(userFile);
        if (!file.exists() || !hasValidAdmin()) {
            recreateDefaultAdmin();
        }
    }

    private void recreateDefaultAdmin() {
        try (FileWriter fw = new FileWriter(userFile)) {
            String hashed = User.generateHash("comsats123");
            fw.write("admin:" + hashed + ":admin\n");
        } catch (IOException e) {
            System.out.println("Failed to create default admin.");
        }
    }

    private boolean hasValidAdmin() {
        try (Scanner sc = new Scanner(new File(userFile))) {
            while (sc.hasNextLine()) {
                User user = User.fromLine(sc.nextLine());
                if (user != null && "admin".equalsIgnoreCase(user.getUsername())) {
                    return user.checkPassword("comsats123");
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public boolean signup(String username, String password) {
        if (username.trim().isEmpty() || password.isEmpty()) return false;
        if (findUser(username.trim()) != null) return false;

        String hashed = User.generateHash(password);
        try (FileWriter fw = new FileWriter(userFile, true)) {
            fw.write(username.trim() + ":" + hashed + ":user\n");
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public User login(String username, String password) {
        try (Scanner sc = new Scanner(new File(userFile))) {
            while (sc.hasNextLine()) {
                User user = User.fromLine(sc.nextLine());
                if (user != null && user.getUsername().equalsIgnoreCase(username.trim()) &&
                        user.checkPassword(password)) {
                    return user;
                }
            }
        } catch (FileNotFoundException ignored) {}
        return null;
    }

    private User findUser(String username) {
        try (Scanner sc = new Scanner(new File(userFile))) {
            while (sc.hasNextLine()) {
                User user = User.fromLine(sc.nextLine());
                if (user != null && user.getUsername().equalsIgnoreCase(username.trim())) {
                    return user;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}

class Subject {
    String name;
    int marks;
    int credits;
    double points;
    String letterGrade;

    public Subject(String name, int marks, int credits) {
        this.name = name.trim();
        this.marks = marks;
        this.credits = credits;
        this.points = getPoints(marks);
        this.letterGrade = getLetterGrade(marks);
    }

    private double getPoints(int mark) {
        if (mark >= 85) return 4.00;
        else if (mark >= 80) return 3.66;
        else if (mark >= 75) return 3.33;
        else if (mark >= 71) return 3.00;
        else if (mark >= 68) return 2.66;
        else if (mark >= 63) return 2.33;
        else if (mark >= 60) return 2.00;
        else if (mark >= 57) return 1.66;
        else if (mark >= 54) return 1.33;
        else if (mark >= 50) return 1.00;
        else return 0.00;
    }

    private String getLetterGrade(int mark) {
        if (mark >= 85) return "A";
        else if (mark >= 80) return "A-";
        else if (mark >= 75) return "B+";
        else if (mark >= 71) return "B";
        else if (mark >= 68) return "B-";
        else if (mark >= 63) return "C+";
        else if (mark >= 60) return "C";
        else if (mark >= 57) return "C-";
        else if (mark >= 54) return "D+";
        else if (mark >= 50) return "D";
        else return "F";
    }
}

class SemesterRecord {
    String studentName;
    String semester;
    double gpa;
    String classification;
    List<Subject> subjects = new ArrayList<>();

    public SemesterRecord(String studentName, String semester, double gpa, String classification, List<Subject> subjects) {
        this.studentName = studentName;
        this.semester = semester;
        this.gpa = gpa;
        this.classification = classification;
        this.subjects.addAll(subjects);
    }

    public String toFileString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(studentName).append("\n");
        sb.append("Semester: ").append(semester).append("\n");
        sb.append("GPA: ").append(String.format("%.2f", gpa)).append("\n");
        sb.append("Classification: ").append(classification).append("\n");
        sb.append("Subjects:\n");
        for (Subject sub : subjects) {
            sb.append("  ").append(sub.name)
              .append(" | Marks: ").append(sub.marks)
              .append(" | Credits: ").append(sub.credits)
              .append(" | Grade: ").append(sub.letterGrade).append("\n");
        }
        sb.append("------------------------\n");
        return sb.toString();
    }
}

class FileHandler {
    private final String filename = "students.txt";

    public boolean save(SemesterRecord record) {
        try (FileWriter fw = new FileWriter(filename, true)) {
            fw.write(record.toFileString());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean delete(String studentName) {
        File file = new File(filename);
        File temp = new File("temp.txt");
        boolean deleted = false;
        try (Scanner sc = new Scanner(file); PrintWriter pw = new PrintWriter(temp)) {
            boolean skip = false;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.trim().startsWith("Name: " + studentName.trim())) {
                    deleted = true;
                    skip = true;
                }
                if (!skip) pw.println(line);
                if (line.trim().equals("------------------------")) skip = false;
            }
        } catch (Exception e) {
            return false;
        }
        file.delete();
        temp.renameTo(file);
        return deleted;
    }
}

class WelcomeFrame extends JFrame {
    public WelcomeFrame() {
        setTitle("COMSATS Student GPA Helper");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);

        JLabel welcomeLabel = new JLabel("Welcome to COMSATS GPA Helper", JLabel.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 32));
        welcomeLabel.setForeground(new Color(57, 49, 133));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(welcomeLabel, gbc);

        try {
            ImageIcon logoIcon = new ImageIcon("images/logo.png");
            Image img = logoIcon.getImage();
            Image scaled = img.getScaledInstance(180, 180, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaled));
            gbc.gridy = 1;
            panel.add(logoLabel, gbc);
        } catch (Exception ignored) {}

        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(new Font("Arial", Font.BOLD, 20));
        loginBtn.setBackground(new Color(0, 160, 227));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setPreferredSize(new Dimension(200, 60));
        gbc.gridy = 2; gbc.gridwidth = 1; gbc.gridx = 0;
        panel.add(loginBtn, gbc);

        JButton signupBtn = new JButton("Sign Up");
        signupBtn.setFont(new Font("Arial", Font.BOLD, 20));
        signupBtn.setBackground(new Color(57, 49, 133));
        signupBtn.setForeground(Color.WHITE);
        signupBtn.setPreferredSize(new Dimension(200, 60));
        gbc.gridx = 1;
        panel.add(signupBtn, gbc);

        add(panel);

        UserManager userManager = new UserManager();

        loginBtn.addActionListener(e -> {
            dispose();
            new LoginFrame(userManager).setVisible(true);
        });

        signupBtn.addActionListener(e -> {
            dispose();
            new SignupFrame(userManager).setVisible(true);
        });
    }
}

class SignupFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField, confirmField;
    private JLabel messageLabel;

    public SignupFrame(UserManager userManager) {
        setTitle("Sign Up - COMSATS GPA Helper");
        setSize(450, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(7, 2, 10, 20));
        panel.setBorder(new EmptyBorder(40, 60, 40, 60));
        panel.setBackground(Color.WHITE);

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        panel.add(new JLabel("Confirm Password:"));
        confirmField = new JPasswordField();
        panel.add(confirmField);

        JButton signupBtn = new JButton("Create Account");
        signupBtn.setFont(new Font("Arial", Font.BOLD, 16));
        signupBtn.setBackground(new Color(0, 160, 227));
        signupBtn.setForeground(Color.WHITE);
        panel.add(signupBtn);

        messageLabel = new JLabel(" ", JLabel.CENTER);
        messageLabel.setForeground(Color.RED);
        panel.add(messageLabel);

        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> {
            dispose();
            new WelcomeFrame().setVisible(true);
        });
        panel.add(backBtn);
        panel.add(new JLabel());

        add(panel);

        signupBtn.addActionListener(e -> {
            String user = usernameField.getText().trim();
            String pass1 = new String(passwordField.getPassword());
            String pass2 = new String(confirmField.getPassword());

            if (user.isEmpty() || pass1.isEmpty()) {
                messageLabel.setText("Please fill all fields!");
            } else if (!pass1.equals(pass2)) {
                messageLabel.setText("Passwords do not match!");
            } else if (userManager.signup(user, pass1)) {
                JOptionPane.showMessageDialog(this, "Account created successfully!\nYou can now login.", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
                new LoginFrame(userManager).setVisible(true);
            } else {
                messageLabel.setText("Username already exists!");
            }
        });
    }
}

class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel messageLabel;

    public LoginFrame(UserManager userManager) {
        setTitle("COMSATS Student Helper - Login");
        setSize(450, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(40, 60, 40, 60));
        mainPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Login to COMSATS GPA Helper", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(57, 49, 133));
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 20));
        formPanel.setBackground(Color.WHITE);

        formPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        formPanel.add(usernameField);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(new Font("Arial", Font.BOLD, 16));
        loginBtn.setBackground(new Color(0, 160, 227));
        loginBtn.setForeground(Color.WHITE);

        messageLabel = new JLabel(" ", JLabel.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        formPanel.add(loginBtn);
        formPanel.add(messageLabel);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        try {
            ImageIcon logoIcon = new ImageIcon("images/logo.png");
            Image img = logoIcon.getImage();
            Image scaled = img.getScaledInstance(120, 120, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaled));
            JPanel logoPanel = new JPanel();
            logoPanel.setBackground(Color.WHITE);
            logoPanel.add(logoLabel);
            mainPanel.add(logoPanel, BorderLayout.SOUTH);
        } catch (Exception ignored) {}

        JButton backBtn = new JButton("Back to Welcome");
        backBtn.addActionListener(e -> {
            dispose();
            new WelcomeFrame().setVisible(true);
        });
        mainPanel.add(backBtn, BorderLayout.EAST);

        add(mainPanel);

        loginBtn.addActionListener(e -> {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword());

            User loggedInUser = userManager.login(user, pass);
            if (loggedInUser != null) {
                messageLabel.setForeground(new Color(0, 150, 0));
                messageLabel.setText("Login Successful!");
                dispose();
                SwingUtilities.invokeLater(() -> new ComsatsStudentHelper(loggedInUser).setVisible(true));
            } else {
                messageLabel.setForeground(Color.RED);
                messageLabel.setText("Invalid username or password!");
                passwordField.setText("");
            }
        });
    }
}

public class ComsatsStudentHelper extends JFrame {
    private JTextField nameField, semesterField, subjectField;
    private JTextArea outputArea;
    private JTable recordsTable;
    private DefaultTableModel tableModel;
    private FileHandler fileHandler = new FileHandler();
    private User currentUser;
    private JTabbedPane tabbedPane;
    private JPanel mainPanel, statsPanel;
    private JTextArea statsArea;

    private String currentStudentName;
    private String currentSemester;
    private List<Subject> currentSubjects = new ArrayList<>();
    private SemesterRecord currentRecord;

    public ComsatsStudentHelper(User currentUser) {
        this.currentUser = currentUser;

        setTitle("COMSATS Student GPA Helper - Logged in as: " + currentUser.getUsername() +
                (currentUser.isAdmin() ? " (Admin)" : " (User)"));
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        add(tabbedPane);

        mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        tabbedPane.addTab("GPA Calculator & Records", mainPanel);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(57, 49, 133));
        JLabel title = new JLabel("COMSATS Student GPA Helper", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.CENTER);

        try {
            ImageIcon logoIcon = new ImageIcon("images/logo.png");
            Image img = logoIcon.getImage();
            Image scaled = img.getScaledInstance(120, 120, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaled));
            header.add(logoLabel, BorderLayout.WEST);
        } catch (Exception ignored) {}

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(Color.RED);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFont(new Font("Arial", Font.BOLD, 14));
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();
                new WelcomeFrame().setVisible(true);
            }
        });
        header.add(logoutBtn, BorderLayout.EAST);

        mainPanel.add(header, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel(new BorderLayout(10, 15));
        leftPanel.setPreferredSize(new Dimension(420, 0));
        leftPanel.setBackground(Color.WHITE);

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 15, 25));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Student & Semester Information"));
        inputPanel.setBackground(Color.WHITE);

        inputPanel.add(new JLabel("Student Name:"));
        nameField = new JTextField();
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Semester:"));
        semesterField = new JTextField();
        inputPanel.add(semesterField);

        inputPanel.add(new JLabel("Number of Subjects:"));
        subjectField = new JTextField();
        inputPanel.add(subjectField);

        JButton clearBtn = new JButton("Clear Fields");
        clearBtn.addActionListener(e -> clearFields());
        inputPanel.add(clearBtn);

        leftPanel.add(inputPanel, BorderLayout.NORTH);

        outputArea = new JTextArea(15, 30);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 15));
        JScrollPane resultScroll = new JScrollPane(outputArea);
        resultScroll.setBorder(BorderFactory.createTitledBorder("GPA Result & Subjects"));
        leftPanel.add(resultScroll, BorderLayout.CENTER);

        mainPanel.add(leftPanel, BorderLayout.WEST);

        tableModel = new DefaultTableModel(new Object[]{"Student", "Semester", "GPA", "Classification"}, 0);
        recordsTable = new JTable(tableModel);
        recordsTable.setRowHeight(30);
        recordsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 15));
        recordsTable.getTableHeader().setBackground(new Color(57, 49, 133));
        recordsTable.getTableHeader().setForeground(Color.WHITE);
        JScrollPane tableScroll = new JScrollPane(recordsTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("All Records"));
        mainPanel.add(tableScroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(4, 2, 20, 20));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Actions"));

        Font btnFont = new Font("Arial", Font.BOLD, 16);
        Color btnColor = new Color(0, 160, 227);

        JButton calcBtn = new JButton("Calculate Weighted GPA");
        JButton saveBtn = new JButton("Save Semester Record");
        JButton viewHistoryBtn = new JButton("View Student History");
        JButton deleteBtn = new JButton("Delete Student Records");
        JButton refreshBtn = new JButton("Refresh Table");
        JButton aboutComsats = new JButton("About COMSATS");
        JButton aboutDev = new JButton("About Developer");

        JButton[] buttons = {calcBtn, saveBtn, viewHistoryBtn, deleteBtn, refreshBtn, aboutComsats, aboutDev};
        for (JButton btn : buttons) {
            btn.setFont(btnFont);
            btn.setBackground(btnColor);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            buttonPanel.add(btn);
        }

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        if (!currentUser.isAdmin()) {
            saveBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
        }

        calcBtn.addActionListener(e -> calculateWeightedGPA());
        saveBtn.addActionListener(e -> saveRecord());
        viewHistoryBtn.addActionListener(e -> viewStudentHistory());
        deleteBtn.addActionListener(e -> deleteStudentRecord());
        refreshBtn.addActionListener(e -> refreshTable());
        aboutComsats.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "COMSATS University Islamabad\nEstablished: 1998\nLeader in IT & Engineering", "About COMSATS", JOptionPane.INFORMATION_MESSAGE));
        aboutDev.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Developed by: Noori\nBS Software Engineering\nAdvanced OOP Project - December 2025", "About Developer", JOptionPane.INFORMATION_MESSAGE));

        statsPanel = new JPanel(new BorderLayout());
        statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Arial", Font.PLAIN, 16));
        statsPanel.add(new JScrollPane(statsArea), BorderLayout.CENTER);
        tabbedPane.addTab("Statistics", statsPanel);

        refreshTable();
        refreshStats();
    }

    private void clearFields() {
        nameField.setText("");
        semesterField.setText("");
        subjectField.setText("");
        outputArea.setText("");
        currentSubjects.clear();
        currentRecord = null;
    }

    private void calculateWeightedGPA() {
        currentStudentName = nameField.getText().trim();
        currentSemester = semesterField.getText().trim();
        if (currentStudentName.isEmpty() || currentSemester.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter Student Name and Semester!");
            return;
        }

        int numSubjects;
        try {
            numSubjects = Integer.parseInt(subjectField.getText().trim());
            if (numSubjects <= 0) throw new Exception();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid number of subjects!");
            return;
        }

        currentSubjects.clear();
        double totalPoints = 0;
        int totalCredits = 0;

        StringBuilder result = new StringBuilder("=== GPA CALCULATION RESULT ===\n\n");
        result.append("Student: ").append(currentStudentName).append("\n");
        result.append("Semester: ").append(currentSemester).append("\n\n");
        result.append(String.format("%-30s %-8s %-8s %-8s\n", "Subject", "Marks", "Credits", "Grade"));
        result.append("-----------------------------------------------------------------\n");

        for (int i = 0; i < numSubjects; i++) {
            String subName = JOptionPane.showInputDialog(this, "Enter Subject " + (i + 1) + " Name:");
            if (subName == null || subName.trim().isEmpty()) return;
            subName = subName.trim();

            int marks = getValidInput("Enter marks for " + subName + " (0-100):", 0, 100);
            if (marks == -1) return;

            int credits = getValidInput("Enter credit hours for " + subName + " (1-6):", 1, 6);
            if (credits == -1) return;

            Subject subject = new Subject(subName, marks, credits);
            currentSubjects.add(subject);

            totalPoints += subject.points * credits;
            totalCredits += credits;

            result.append(String.format("%-30s %-8d %-8d %-8s\n", subName, marks, credits, subject.letterGrade));
        }

        double gpa = totalCredits > 0 ? totalPoints / totalCredits : 0.0;
        String classification = classifyGPA(gpa);

        result.append("-----------------------------------------------------------------\n");
        result.append("Total Credits: ").append(totalCredits).append("\n");
        result.append("Weighted GPA: ").append(String.format("%.2f", gpa)).append("\n");
        result.append("Classification: ").append(classification).append("\n");

        currentRecord = new SemesterRecord(currentStudentName, currentSemester, gpa, classification, currentSubjects);
        outputArea.setText(result.toString());

        refreshStats();
    }

    private int getValidInput(String message, int min, int max) {
        while (true) {
            String input = JOptionPane.showInputDialog(this, message);
            if (input == null) return -1;
            try {
                int value = Integer.parseInt(input.trim());
                if (value >= min && value <= max) return value;
                throw new Exception();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Invalid input! Must be between " + min + " and " + max + ".");
            }
        }
    }

    private String classifyGPA(double gpa) {
        if (gpa >= 3.66) return "Excellent";
        else if (gpa >= 3.00) return "Good";
        else if (gpa >= 2.00) return "Average";
        else if (gpa > 0) return "Probation";
        else return "Fail";
    }

    private void saveRecord() {
        if (currentRecord == null) {
            JOptionPane.showMessageDialog(this, "Calculate GPA first!");
            return;
        }
        if (!currentUser.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Only Admin can save records!");
            return;
        }
        if (fileHandler.save(currentRecord)) {
            JOptionPane.showMessageDialog(this, "Semester record saved successfully!");
            refreshTable();
            refreshStats();
        }
    }

    private void viewStudentHistory() {
        String name = JOptionPane.showInputDialog(this, "Enter student name to view history:");
        if (name == null || name.trim().isEmpty()) return;

        List<SemesterRecord> history = new ArrayList<>();
        try (Scanner sc = new Scanner(new File("students.txt"))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.startsWith("Name: " + name.trim())) {
                    String semester = sc.nextLine().trim().substring(10);
                    double gpa = Double.parseDouble(sc.nextLine().trim().substring(5));
                    String classification = sc.nextLine().trim().substring(15);
                    List<Subject> subjects = new ArrayList<>();
                    sc.nextLine(); // "Subjects:"
                    while (sc.hasNextLine()) {
                        String subLine = sc.nextLine().trim();
                        if (subLine.equals("------------------------")) break;
                        if (subLine.startsWith("  ")) {
                            String[] parts = subLine.substring(2).split(" \\| ");
                            String subName = parts[0];
                            int marks = Integer.parseInt(parts[1].substring(7));
                            int credits = Integer.parseInt(parts[2].substring(9));
                            subjects.add(new Subject(subName, marks, credits));
                        }
                    }
                    history.add(new SemesterRecord(name.trim(), semester, gpa, classification, subjects));
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error reading records.");
            return;
        }

        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No records found for " + name);
            return;
        }

        StringBuilder sb = new StringBuilder("=== HISTORY FOR " + name.toUpperCase() + " ===\n\n");
        double cumGPA = 0;
        for (SemesterRecord rec : history) {
            sb.append("Semester: ").append(rec.semester)
              .append(" | GPA: ").append(String.format("%.2f", rec.gpa))
              .append(" | ").append(rec.classification).append("\n");
            cumGPA += rec.gpa;
        }
        sb.append("\nCumulative Average GPA: ").append(String.format("%.2f", cumGPA / history.size()));
        JOptionPane.showMessageDialog(this, sb.toString(), "Student History", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteStudentRecord() {
        String name = JOptionPane.showInputDialog(this, "Enter student name to DELETE all records:");
        if (name == null || name.trim().isEmpty()) return;
        int confirm = JOptionPane.showConfirmDialog(this, "This will delete ALL records for " + name + ".\nContinue?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (fileHandler.delete(name.trim())) {
                JOptionPane.showMessageDialog(this, "All records deleted.");
                refreshTable();
                refreshStats();
            } else {
                JOptionPane.showMessageDialog(this, "Student not found.");
            }
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        try (Scanner sc = new Scanner(new File("students.txt"))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.startsWith("Name: ")) {
                    String student = line.substring(6);
                    String semester = sc.nextLine().trim().substring(10);
                    String gpaStr = sc.nextLine().trim().substring(5);
                    String classification = sc.nextLine().trim().substring(15);
                    while (sc.hasNextLine() && !sc.nextLine().trim().equals("------------------------")) {}
                    tableModel.addRow(new Object[]{student, semester, gpaStr, classification});
                }
            }
        } catch (Exception ignored) {}
    }

    private void refreshStats() {
        Map<String, Integer> classCount = new HashMap<>(Map.of("Excellent", 0, "Good", 0, "Average", 0, "Probation", 0, "Fail", 0));
        int totalRecords = 0;
        double totalGPA = 0;

        try (Scanner sc = new Scanner(new File("students.txt"))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.startsWith("Semester: ")) {
                    totalRecords++;
                    double gpa = Double.parseDouble(sc.nextLine().trim().substring(5));
                    totalGPA += gpa;
                    String classification = sc.nextLine().trim().substring(15);
                    classCount.put(classification, classCount.get(classification) + 1);
                    while (sc.hasNextLine() && !sc.nextLine().trim().equals("------------------------")) {}
                }
            }
        } catch (Exception ignored) {}

        StringBuilder stats = new StringBuilder("=== OVERALL STATISTICS ===\n\n");
        stats.append("Total Semester Records: ").append(totalRecords).append("\n");
        if (totalRecords > 0) {
            stats.append("Overall Average GPA: ").append(String.format("%.2f", totalGPA / totalRecords)).append("\n\n");
        }
        stats.append("Classification Distribution:\n");
        for (Map.Entry<String, Integer> entry : classCount.entrySet()) {
            int count = entry.getValue();
            if (count > 0) {
                double percent = totalRecords > 0 ? (count * 100.0 / totalRecords) : 0;
                stats.append("  ").append(entry.getKey()).append(": ").append(count)
                     .append(" records (").append(String.format("%.1f", percent)).append("%)\n");
            }
        }

        statsArea.setText(stats.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WelcomeFrame().setVisible(true));
    }
}
