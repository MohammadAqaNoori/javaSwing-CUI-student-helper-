import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.util.Scanner;

// Person class
class Person {
    protected String name;

    public Person(String name) {
        this.name = name.trim();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name.trim(); }
}

// Student class
class Student extends Person {
    private String semester;
    private double gpa;
    private String classification;

    public Student(String name, String semester) {
        super(name);
        this.semester = semester.trim();
    }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester.trim(); }
    public double getGPA() { return gpa; }
    public String getClassification() { return classification; }

    public void calculateGPA(int[] marks) {
        double total = 0;
        for (int mark : marks) {
            total += getPoints(mark);
        }
        this.gpa = total / marks.length;
        this.classification = classifyGPA(this.gpa);
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

    private String classifyGPA(double gpa) {
        if (gpa >= 3.66) return "Excellent";
        else if (gpa >= 3.00) return "Good";
        else if (gpa >= 2.00) return "Average";
        else if (gpa > 0) return "Probation";
        else return "Fail";
    }

    public String toFileString() {
        return "Name: " + name + "\nSemester: " + semester + "\nGPA: " + String.format("%.2f", gpa) +
                "\nClassification: " + classification + "\n------------------------\n";
    }
}

// FileHandler class
class FileHandler {
    private final String filename = "students.txt";

    public boolean save(Student s) {
        if (checkDuplicate(s.getName())) return false;
        try (FileWriter fw = new FileWriter(filename, true)) {
            fw.write(s.toFileString());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean checkDuplicate(String name) {
        try (Scanner sc = new Scanner(new File(filename))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.equalsIgnoreCase("Name: " + name)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public String viewAll() {
        StringBuilder sb = new StringBuilder();
        try (Scanner sc = new Scanner(new File(filename))) {
            while (sc.hasNextLine()) {
                sb.append(sc.nextLine()).append("\n");
            }
            if (sb.length() == 0) sb.append("No records found.");
        } catch (Exception e) {
            sb.append("No records found.");
        }
        return sb.toString();
    }

    public String search(String name) {
        StringBuilder sb = new StringBuilder();
        boolean found = false;
        try (Scanner sc = new Scanner(new File(filename))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.equalsIgnoreCase("Name: " + name.trim())) {
                    found = true;
                    sb.append(line).append("\n");
                    for (int i = 0; i < 4 && sc.hasNextLine(); i++) {
                        sb.append(sc.nextLine()).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            return "File error";
        }
        return found ? sb.toString() : "Student not found.";
    }

    public boolean update(Student s) {
        File file = new File(filename);
        File temp = new File("temp.txt");
        boolean updated = false;
        try (Scanner sc = new Scanner(file); FileWriter fw = new FileWriter(temp)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.trim().equalsIgnoreCase("Name: " + s.getName())) {
                    updated = true;
                    fw.write(s.toFileString());
                    for (int i = 0; i < 4 && sc.hasNextLine(); i++) sc.nextLine();
                } else {
                    fw.write(line + "\n");
                }
            }
        } catch (Exception e) {
            return false;
        }
        file.delete();
        temp.renameTo(file);
        return updated;
    }

    public boolean delete(String name) {
        File file = new File(filename);
        File temp = new File("temp.txt");
        boolean deleted = false;
        try (Scanner sc = new Scanner(file); FileWriter fw = new FileWriter(temp)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.trim().equalsIgnoreCase("Name: " + name.trim())) {
                    deleted = true;
                    for (int i = 0; i < 4 && sc.hasNextLine(); i++) sc.nextLine();
                } else {
                    fw.write(line + "\n");
                }
            }
        } catch (Exception e) {
            return false;
        }
        file.delete();
        temp.renameTo(file);
        return deleted;
    }
}

// Main GUI Class
public class ComsatsStudentHelper extends JFrame {
    private JTextField nameField, semesterField, subjectField;
    private JTextArea outputArea;
    private FileHandler fileHandler = new FileHandler();
    private Student currentStudent;

    public ComsatsStudentHelper() {
        // Use Nimbus Look and Feel - Built-in, modern, clean, professional
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus not available, fall back to default
        }

        // COMSATS-inspired accent color
        UIManager.put("nimbusBlueGrey", new Color(0, 160, 227)); // Teal accent
        UIManager.put("control", Color.WHITE);

        setTitle("COMSATS Student Helper");
        setSize(1050, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        setContentPane(mainPanel);

        // Header - Deep blue COMSATS color
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(57, 49, 133));
        JLabel title = new JLabel("COMSATS Student Helper", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.CENTER);

        // Optional logo
        try {
            ImageIcon logoIcon = new ImageIcon("images/logo.png");
            Image scaled = logoIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaled));
            header.add(logoLabel, BorderLayout.WEST);
        } catch (Exception ignored) {}

        mainPanel.add(header, BorderLayout.NORTH);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 15, 25));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Student Information"));
        inputPanel.setBackground(Color.WHITE);

        inputPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Semester:"));
        semesterField = new JTextField();
        inputPanel.add(semesterField);
        inputPanel.add(new JLabel("Number of Subjects:"));
        subjectField = new JTextField();
        inputPanel.add(subjectField);

        JButton clearBtn = new JButton("Clear Fields");
        clearBtn.addActionListener(e -> {
            nameField.setText(""); semesterField.setText(""); subjectField.setText("");
            currentStudent = null;
        });
        inputPanel.add(clearBtn);

        mainPanel.add(inputPanel, BorderLayout.WEST);

        // Output Area - White background, black text
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Arial", Font.PLAIN, 16));
        outputArea.setForeground(Color.BLACK);
        outputArea.setBackground(Color.WHITE);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Output / Records"));

        mainPanel.add(scroll, BorderLayout.CENTER);

        // Buttons Panel - All uniform professional blue
        JPanel buttonPanel = new JPanel(new GridLayout(4, 2, 20, 20));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Actions"));

        Font btnFont = new Font("Arial", Font.BOLD, 15);
        Color btnColor = new Color(0, 160, 227); // COMSATS teal

        JButton calc = new JButton("Calculate GPA");
        JButton save = new JButton("Save Record");
        JButton view = new JButton("View All Records");
        JButton search = new JButton("Search Student");
        JButton update = new JButton("Update Record");
        JButton delete = new JButton("Delete Record");
        JButton aboutC = new JButton("About COMSATS");
        JButton aboutDev = new JButton("About Developer");

        JButton[] buttons = {calc, save, view, search, update, delete, aboutC, aboutDev};
        for (JButton btn : buttons) {
            btn.setFont(btnFont);
            btn.setBackground(btnColor);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            buttonPanel.add(btn);
        }

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Action Listeners
        calc.addActionListener(e -> calculateGPAAction());
        save.addActionListener(e -> saveAction());
        view.addActionListener(e -> viewAction());
        search.addActionListener(e -> searchAction());
        update.addActionListener(e -> updateAction());
        delete.addActionListener(e -> deleteAction());

        aboutC.addActionListener(e -> outputArea.setText(
                "COMSATS University Islamabad (CUI)\nEstablished: 1998\nLeading Public Sector University in IT & Engineering\nMultiple Campuses | Over 36,000 Students\nRanked Among Top Universities in Pakistan"
        ));

        aboutDev.addActionListener(e -> outputArea.setText(
                "Developed by: Mohammad Aqa Noori\nBS Software Engineering\nCOMSATS University Islamabad\n Final Project - Java Swing\nDecember 2025"
        ));
    }

    private void calculateGPAAction() {
        String name = nameField.getText().trim();
        String sem = semesterField.getText().trim();
        if (name.isEmpty() || sem.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter Name and Semester!");
            return;
        }
        int n;
        try {
            n = Integer.parseInt(subjectField.getText().trim());
            if (n <= 0) throw new Exception();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid number of subjects!");
            return;
        }

        int[] marks = new int[n];
        for (int i = 0; i < n; i++) {
            while (true) {
                String input = JOptionPane.showInputDialog(this, "Enter marks for subject " + (i + 1) + " (0-100):");
                if (input == null) return;
                try {
                    int mark = Integer.parseInt(input.trim());
                    if (mark < 0 || mark > 100) throw new Exception();
                    marks[i] = mark;
                    break;
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid marks! Enter 0-100.");
                }
            }
        }

        currentStudent = new Student(name, sem);
        currentStudent.calculateGPA(marks);

        outputArea.setText("GPA Calculated Successfully!\n\n" +
                "Name: " + currentStudent.getName() + "\n" +
                "Semester: " + currentStudent.getSemester() + "\n" +
                "GPA: " + String.format("%.2f", currentStudent.getGPA()) + "\n" +
                "Classification: " + currentStudent.getClassification());
    }

    private void saveAction() {
        if (currentStudent == null) {
            JOptionPane.showMessageDialog(this, "Calculate GPA first!");
            return;
        }
        if (fileHandler.save(currentStudent)) {
            outputArea.setText("Record saved successfully!");
        } else {
            outputArea.setText("Student already exists! Use Update instead.");
        }
    }

    private void viewAction() {
        outputArea.setText(fileHandler.viewAll());
    }

    private void searchAction() {
        String name = JOptionPane.showInputDialog(this, "Enter student name to search:");
        if (name != null && !name.trim().isEmpty()) {
            outputArea.setText(fileHandler.search(name.trim()));
        }
    }

    private void updateAction() {
        if (currentStudent == null) {
            JOptionPane.showMessageDialog(this, "Calculate GPA first!");
            return;
        }
        if (fileHandler.update(currentStudent)) {
            outputArea.setText("Student record updated successfully.");
        } else {
            outputArea.setText("Student not found.");
        }
    }

    private void deleteAction() {
        String name = JOptionPane.showInputDialog(this, "Enter student name to delete:");
        if (name != null && !name.trim().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (fileHandler.delete(name.trim())) {
                    outputArea.setText("Student record deleted.");
                } else {
                    outputArea.setText("Student not found.");
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ComsatsStudentHelper().setVisible(true));
    }
}