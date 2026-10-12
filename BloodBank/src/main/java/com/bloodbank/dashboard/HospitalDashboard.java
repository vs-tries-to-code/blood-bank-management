package com.bloodbank.dashboard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class HospitalDashboard extends JPanel {

    private final MainFrame mainFrame;
    private final JLabel availableUnitsValue = new JLabel("...");
    private final JLabel myRequestsValue = new JLabel("...");
    private final JLabel loadStatusLabel = new JLabel(" ");
    private final BloodAvailabilityTable availabilityTable = new BloodAvailabilityTable();

    public HospitalDashboard(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(24, 24, 24, 24));
        setBackground(DashboardTheme.BACKGROUND);

        JLabel title = DashboardTheme.createSectionTitle("Hospital Dashboard");
        add(title, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(18, 18));
        contentPanel.setOpaque(false);

        JPanel summaryPanel = new JPanel(new GridLayout(1, 2, 18, 18));
        summaryPanel.setOpaque(false);
        summaryPanel.add(DashboardTheme.createStatCard(
            "Available Units", availableUnitsValue, new Color(173, 35, 57)));
        summaryPanel.add(DashboardTheme.createStatCard(
            "My Requests", myRequestsValue, new Color(36, 110, 154)));
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

        JButton inventoryButton = DashboardTheme.createSecondaryButton("View Inventory");
        JButton requestButton = DashboardTheme.createActionButton("Request Blood");
        JButton refreshButton = DashboardTheme.createSecondaryButton("Refresh");
        JButton logoutButton = DashboardTheme.createSecondaryButton("Logout");

        buttonPanel.add(inventoryButton);
        buttonPanel.add(requestButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(logoutButton);
        add(buttonPanel, BorderLayout.SOUTH);

        inventoryButton.addActionListener(e -> mainFrame.showScreen("INVENTORY"));
        requestButton.addActionListener(e -> mainFrame.showHospitalRequests());
        refreshButton.addActionListener(e -> refreshData());
        logoutButton.addActionListener(e -> mainFrame.showLogin());
    }

    public void refreshData() {
        availableUnitsValue.setText("...");
        myRequestsValue.setText("...");
        loadStatusLabel.setText("Loading dashboard data...");
        String hospitalName = mainFrame.getCurrentUser().getUsername();

        new SwingWorker<DashboardDataService.Snapshot, Void>() {
            @Override
            protected DashboardDataService.Snapshot doInBackground() throws Exception {
                return new DashboardDataService().load(hospitalName);
            }

            @Override
            protected void done() {
                try {
                    DashboardDataService.Snapshot snapshot = get();
                    availableUnitsValue.setText(String.valueOf(snapshot.availableUnits()));
                    myRequestsValue.setText(String.valueOf(snapshot.hospitalRequests()));
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
        System.err.println("Could not load hospital dashboard data: " + error.getMessage());
        availableUnitsValue.setText("—");
        myRequestsValue.setText("—");
        availabilityTable.showUnavailable();
        loadStatusLabel.setText("Dashboard data unavailable. Check the MySQL connection and schema.");
    }
}