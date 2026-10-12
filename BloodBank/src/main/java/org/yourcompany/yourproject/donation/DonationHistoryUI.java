package org.yourcompany.yourproject.donation;

import com.bloodbank.dashboard.DashboardTheme;
import com.bloodbank.dashboard.MainFrame;

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
        String donorIdInput = JOptionPane.showInputDialog(this, "Enter an existing donor ID:");
        if (donorIdInput == null) {
            return;
        }

        String dateInput = JOptionPane.showInputDialog(
                this, "Donation date (YYYY-MM-DD):", LocalDate.now().toString());
        if (dateInput == null) {
            return;
        }

        String quantityInput = JOptionPane.showInputDialog(this, "Quantity in milliliters:", "350");
        if (quantityInput == null) {
            return;
        }

        int donorId;
        int quantityMl;
        LocalDate donationDate;
        try {
            donorId = Integer.parseInt(donorIdInput.trim());
            donationDate = LocalDate.parse(dateInput.trim());
            quantityMl = Integer.parseInt(quantityInput.trim());
            if (donorId <= 0 || quantityMl <= 0) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter a positive donor ID, a valid date (YYYY-MM-DD), and a positive quantity.",
                    "Invalid Donation",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        addButton.setEnabled(false);
        statusLabel.setText("Saving donation...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                repository.save(donorId, donationDate, quantityMl);
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
                "Could not save donation. Confirm that the donor ID exists and the database is available.\n"
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