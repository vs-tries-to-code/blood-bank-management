package com.bloodbank.dashboard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class AdminDashboard extends JPanel {

    private final MainFrame mainFrame;
    private final JLabel availableUnitsValue = new JLabel("...");
    private final JLabel pendingRequestsValue = new JLabel("...");
    private final JLabel donationRecordsValue = new JLabel("...");
    private final JLabel loadStatusLabel = new JLabel(" ");
    private final BloodAvailabilityTable availabilityTable = new BloodAvailabilityTable();

    public AdminDashboard(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(24, 24, 24, 24));
        setBackground(DashboardTheme.BACKGROUND);

        JLabel title = DashboardTheme.createSectionTitle("Admin Dashboard");
        add(title, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(18, 18));
        contentPanel.setOpaque(false);

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 18, 18));
        summaryPanel.setOpaque(false);
        summaryPanel.add(DashboardTheme.createStatCard(
            "Available Units", availableUnitsValue, new Color(182, 29, 48)));
        summaryPanel.add(DashboardTheme.createStatCard(
            "Pending Requests", pendingRequestsValue, new Color(42, 130, 95)));
        summaryPanel.add(DashboardTheme.createStatCard(
            "Donation Records", donationRecordsValue, new Color(98, 93, 178)));
        contentPanel.add(summaryPanel, BorderLayout.NORTH);

        JPanel availabilityPanel = new JPanel(new BorderLayout(8, 8));
        availabilityPanel.setOpaque(false);
        JLabel availabilityTitle = new JLabel("Available Units by Blood Group and Component");
        availabilityTitle.setFont(DashboardTheme.SUBTITLE_FONT);
        availabilityPanel.add(availabilityTitle, BorderLayout.NORTH);
        availabilityPanel.add(new JScrollPane(availabilityTable), BorderLayout.CENTER);
        contentPanel.add(availabilityPanel, BorderLayout.CENTER);
        contentPanel.add(loadStatusLabel, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setOpaque(false);

        JButton inventoryButton = DashboardTheme.createSecondaryButton("Inventory");
        JButton donationButton = DashboardTheme.createSecondaryButton("Donations");
        JButton requestButton = DashboardTheme.createActionButton("Requests");
        JButton refreshButton = DashboardTheme.createSecondaryButton("Refresh");
        JButton logoutButton = DashboardTheme.createSecondaryButton("Logout");

        buttonPanel.add(inventoryButton);
        buttonPanel.add(donationButton);
        buttonPanel.add(requestButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(logoutButton);
        add(buttonPanel, BorderLayout.SOUTH);

        inventoryButton.addActionListener(e -> mainFrame.showScreen("INVENTORY"));
        donationButton.addActionListener(e -> mainFrame.showScreen("DONATIONS"));
        requestButton.addActionListener(e -> mainFrame.showScreen("ADMIN_REQUESTS"));
        refreshButton.addActionListener(e -> refreshData());
        logoutButton.addActionListener(e -> mainFrame.showLogin());
    }

    public void refreshData() {
        availableUnitsValue.setText("...");
        pendingRequestsValue.setText("...");
        donationRecordsValue.setText("...");
        loadStatusLabel.setText("Loading dashboard data...");

        new SwingWorker<DashboardDataService.Snapshot, Void>() {
            @Override
            protected DashboardDataService.Snapshot doInBackground() throws Exception {
                return new DashboardDataService().load("");
            }

            @Override
            protected void done() {
                try {
                    DashboardDataService.Snapshot snapshot = get();
                    availableUnitsValue.setText(String.valueOf(snapshot.availableUnits()));
                    pendingRequestsValue.setText(String.valueOf(snapshot.pendingRequests()));
                    donationRecordsValue.setText(String.valueOf(snapshot.donationRecords()));
                    availabilityTable.showSnapshot(snapshot);
                    loadStatusLabel.setText(" ");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showLoadFailure(e);
                } catch (ExecutionException e) {
                    showLoadFailure(e.getCause());
                }
            }
        }.execute();
    }

    private void showLoadFailure(Throwable error) {
        System.err.println("Could not load admin dashboard data: " + error.getMessage());
        availableUnitsValue.setText("—");
        pendingRequestsValue.setText("—");
        donationRecordsValue.setText("—");
        availabilityTable.showUnavailable();
        loadStatusLabel.setText("Dashboard data unavailable. Check the MySQL connection and schema.");
    }
}