package org.yourcompany.yourproject.donation;

import com.bloodbank.dashboard.DashboardTheme;
import com.bloodbank.dashboard.MainFrame;
import org.yourcompany.yourproject.inventory.BloodGroup;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DonationHistoryUI extends JPanel {

    private final DefaultTableModel tableModel;
    private final MySQLDonationRepository repository;
    private final JLabel statusLabel;
    private final JButton addButton;
    private final MainFrame mainFrame;

    public DonationHistoryUI(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        repository = new MySQLDonationRepository();
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(DashboardTheme.BACKGROUND);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        JLabel titleLabel = DashboardTheme.createSectionTitle("Donation History");
        JButton backButton = DashboardTheme.createSecondaryButton("Back");
        backButton.addActionListener(event -> mainFrame.goBackToDashboard());
        titleBar.add(titleLabel, BorderLayout.CENTER);
        titleBar.add(backButton, BorderLayout.EAST);
        add(titleBar, BorderLayout.NORTH);

        String[] columns = {
                "Donation ID", "Donor ID", "Donor Name", "Donation Date", "Blood Group", "Quantity (ml)"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable donationTable = new JTable(tableModel);
        donationTable.setFillsViewportHeight(true);
        donationTable.setRowHeight(30);
        add(new JScrollPane(donationTable), BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        addButton = DashboardTheme.createActionButton("Add Donation");
        addButton.addActionListener(event -> addDonation());
        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.setOpaque(false);
        actionsPanel.add(statusLabel, BorderLayout.CENTER);
        actionsPanel.add(addButton, BorderLayout.EAST);
        add(actionsPanel, BorderLayout.SOUTH);

        loadDonations();
    }

    private void addDonation() {
        JTextField donorNameField = new JTextField(18);
        JComboBox<BloodGroup> bloodGroupBox = new JComboBox<>(BloodGroup.values());
        JTextField phoneField = new JTextField(18);
        JTextField ageField = new JTextField("18", 18);
        JComboBox<String> genderBox = new JComboBox<>(
                new String[]{"FEMALE", "MALE", "OTHER", "UNSPECIFIED"});
        JTextField dateField = new JTextField(LocalDate.now().toString(), 18);
        JTextField quantityField = new JTextField("350", 18);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Donor name:"));
        form.add(donorNameField);
        form.add(new JLabel("Blood group:"));
        form.add(bloodGroupBox);
        form.add(new JLabel("Phone (used to find existing donors):"));
        form.add(phoneField);
        form.add(new JLabel("Age:"));
        form.add(ageField);
        form.add(new JLabel("Gender:"));
        form.add(genderBox);
        form.add(new JLabel("Donation date (YYYY-MM-DD):"));
        form.add(dateField);
        form.add(new JLabel("Quantity (ml):"));
        form.add(quantityField);

        int result = JOptionPane.showConfirmDialog(
                this, form, "Add Donation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String donorName = donorNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String ageInput = ageField.getText().trim();
        String dateInput = dateField.getText().trim();
        String quantityInput = quantityField.getText().trim();
        int age;
        int quantityMl;
        LocalDate donationDate;
        try {
            age = Integer.parseInt(ageInput);
            donationDate = LocalDate.parse(dateInput);
            quantityMl = Integer.parseInt(quantityInput);
            if (donorName.isBlank() || phone.isBlank() || age <= 0 || age > 130 || quantityMl <= 0) {
                throw new IllegalArgumentException("Donor details and positive age/quantity are required.");
            }
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter a donor name and phone, an age from 1 to 130, a valid date (YYYY-MM-DD), and a positive quantity.",
                    "Invalid Donation Details",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        Donor donor = new Donor(
                null,
                donorName,
                age,
                (String) genderBox.getSelectedItem(),
                ((BloodGroup) bloodGroupBox.getSelectedItem()).name(),
                phone,
                null
        );

        addButton.setEnabled(false);
        statusLabel.setText("Saving donation...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                repository.save(donor, donationDate, quantityMl);
                return null;
            }

            @Override
            protected void done() {
                addButton.setEnabled(true);
                try {
                    get();
                    statusLabel.setText("Donation saved.");
                    loadDonations();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showSaveFailure(exception);
                } catch (ExecutionException exception) {
                    showSaveFailure(exception.getCause());
                }
            }
        }.execute();
    }

    private void loadDonations() {
        statusLabel.setText("Loading donations...");
        new SwingWorker<List<DonationHistory>, Void>() {
            @Override
            protected List<DonationHistory> doInBackground() throws SQLException {
                return repository.findAll();
            }

            @Override
            protected void done() {
                try {
                    List<DonationHistory> donations = get();
                    tableModel.setRowCount(0);
                    for (DonationHistory donation : donations) {
                        tableModel.addRow(new Object[]{
                                donation.getDonationId(), donation.getDonorId(),
                                donation.getDonorName(), donation.getDonationDate(),
                                donation.getBloodGroup(), donation.getQuantityMl()
                        });
                    }
                    statusLabel.setText(donations.size() + " donation record(s)");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showLoadFailure(exception);
                } catch (ExecutionException exception) {
                    showLoadFailure(exception.getCause());
                }
            }
        }.execute();
    }

    private void showSaveFailure(Throwable error) {
        statusLabel.setText("Could not save donation.");
        JOptionPane.showMessageDialog(
                this,
                "Could not save donation. Check the donor details and database connection.\n"
                        + error.getMessage(),
                "Save Failed",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void showLoadFailure(Throwable error) {
        statusLabel.setText("Could not load donations. Check the MySQL connection and schema.");
        System.err.println("Could not load donation records: " + error.getMessage());
    }

}