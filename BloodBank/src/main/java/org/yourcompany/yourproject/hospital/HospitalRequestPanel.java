
package org.yourcompany.yourproject.hospital;

import com.bloodbank.dashboard.DashboardTheme;
import com.bloodbank.dashboard.MainFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import org.yourcompany.yourproject.inventory.InventoryManager;
import org.yourcompany.yourproject.inventory.BloodGroup;
import org.yourcompany.yourproject.inventory.BloodComponent;

public class HospitalRequestPanel extends JPanel {

    private HospitalRequestRepository repository;
    private DefaultTableModel tableModel;
    private JTable requestTable;
    private final MainFrame mainFrame;
    private final String hospitalName;
    private final InventoryManager inventoryManager =
            new InventoryManager();

    public HospitalRequestPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        if (mainFrame.getCurrentUser() == null) {
            throw new IllegalStateException("No user is logged in.");
        }

        this.hospitalName =
                mainFrame.getCurrentUser().getUsername();

        repository =
                HospitalRequestRepositoryProvider.getRepository();

        setLayout(new BorderLayout(10, 10));

        // Title bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);

        JLabel title = new JLabel("Hospital Blood Request");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JButton backButton =
                DashboardTheme.createSecondaryButton("Back");

        backButton.addActionListener(
                e -> mainFrame.goBackToDashboard()
        );

        titleBar.add(title, BorderLayout.CENTER);
        titleBar.add(backButton, BorderLayout.EAST);
        add(titleBar, BorderLayout.NORTH);

        // Request form
        JPanel formPanel =
                new JPanel(new GridLayout(5, 2, 10, 10));

        // Hospital name comes from the logged-in account
        JLabel hospitalLabel = new JLabel("Hospital:");
        JLabel hospitalField = new JLabel(hospitalName);

        // Blood group
        JLabel bloodLabel = new JLabel("Blood Group:");

        String[] bloodGroups = {
                "A_POSITIVE",
                "A_NEGATIVE",
                "B_POSITIVE",
                "B_NEGATIVE",
                "AB_POSITIVE",
                "AB_NEGATIVE",
                "O_POSITIVE",
                "O_NEGATIVE"
        };

        JComboBox<String> bloodGroupBox =
                new JComboBox<>(bloodGroups);

        // Component
        JLabel componentLabel = new JLabel("Component:");

        String[] components = {
                "WHOLE_BLOOD",
                "PRBC",
                "PLATELETS",
                "FPP"
        };

        JComboBox<String> componentBox =
                new JComboBox<>(components);

        // Units required
        JLabel unitsLabel = new JLabel("Units Required:");
        JTextField unitsField = new JTextField();

        // Urgency
        JLabel urgencyLabel = new JLabel("Urgency:");

        String[] urgencyOptions = {
                "Normal",
                "Urgent",
                "Emergency"
        };

        JComboBox<String> urgencyBox =
                new JComboBox<>(urgencyOptions);

        formPanel.add(hospitalLabel);
        formPanel.add(hospitalField);

        formPanel.add(bloodLabel);
        formPanel.add(bloodGroupBox);

        formPanel.add(componentLabel);
        formPanel.add(componentBox);

        formPanel.add(unitsLabel);
        formPanel.add(unitsField);

        formPanel.add(urgencyLabel);
        formPanel.add(urgencyBox);

        JButton submitButton = new JButton("Submit Request");
        JButton clearButton = new JButton("Clear Form");

        submitButton.addActionListener(e -> {

            String bloodGroup =
                    (String) bloodGroupBox.getSelectedItem();

            String component =
                    (String) componentBox.getSelectedItem();

            String units = unitsField.getText().trim();

            String urgency =
                    (String) urgencyBox.getSelectedItem();

            // Validate units
            if (units.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter the number of units."
                );
                return;
            }

            int unitCount;

            try {
                unitCount = Integer.parseInt(units);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Units must be a number."
                );
                return;
            }

            if (unitCount <= 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Units must be greater than 0."
                );
                return;
            }

            // Check current stock
            int availableStock;

            try {
                availableStock = inventoryManager.getAvailableStock(
                        BloodGroup.valueOf(bloodGroup),
                        BloodComponent.valueOf(component)
                );
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid blood group or component.",
                        "Invalid Selection",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Handle database or stock-check errors
            if (availableStock < 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Unable to check blood stock. Please try again.",
                        "Stock Check Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Show the appropriate stock warning
            if (availableStock == 0) {

                JOptionPane.showMessageDialog(
                        this,
                        "OUT OF STOCK!\nNo available units of "
                                + bloodGroup + " " + component + ".",
                        "Out of Stock",
                        JOptionPane.WARNING_MESSAGE
                );

            } else if (availableStock < unitCount) {

                String message =
                        "INSUFFICIENT STOCK!\nRequested: "
                                + unitCount
                                + "\nAvailable: " + availableStock;

                if (availableStock < 3) {
                    message += "\n\nLOW STOCK ALERT!";
                }

                JOptionPane.showMessageDialog(
                        this,
                        message,
                        "Stock Warning",
                        JOptionPane.WARNING_MESSAGE
                );

            } else if (availableStock - unitCount < 3) {

                JOptionPane.showMessageDialog(
                        this,
                        "LOW STOCK ALERT!\nRemaining after fulfilment: "
                                + (availableStock - unitCount),
                        "Low Stock",
                        JOptionPane.WARNING_MESSAGE
                );
            }

            // Create and save the request.
            // This does NOT deduct inventory.
            HospitalRequest request = new HospitalRequest(
                    0,
                    hospitalName,
                    bloodGroup,
                    component,
                    unitCount,
                    urgency
            );

            repository.save(request);

            loadRequests();

            JOptionPane.showMessageDialog(
                    this,
                    "Request submitted successfully!"
            );

            clearForm(
                    unitsField,
                    bloodGroupBox,
                    componentBox,
                    urgencyBox
            );
        });

        clearButton.addActionListener(e ->
                clearForm(
                        unitsField,
                        bloodGroupBox,
                        componentBox,
                        urgencyBox
                )
        );

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(submitButton);
        bottomPanel.add(clearButton);

        // Requests table
        String[] columns = {
                "ID",
                "Hospital",
                "Blood Group",
                "Component",
                "Units",
                "Urgency",
                "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        requestTable = new JTable(tableModel);

        JScrollPane tableScrollPane =
                new JScrollPane(requestTable);

        JPanel centerPanel =
                new JPanel(new BorderLayout(20, 20));

        centerPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        30, 50, 30, 50
                )
        );

        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);
        centerPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Load only this hospital's requests
        loadRequests();
    }

    private void loadRequests() {

        tableModel.setRowCount(0);

        for (HospitalRequest request :
                repository.findByHospitalName(hospitalName)) {

            tableModel.addRow(new Object[]{
                    request.getRequestId(),
                    request.getHospitalName(),
                    request.getBloodGroup(),
                    request.getComponent(),
                    request.getUnitsRequired(),
                    request.getUrgency(),
                    request.getStatus()
            });
        }
    }

    private void clearForm(
            JTextField unitsField,
            JComboBox<String> bloodGroupBox,
            JComboBox<String> componentBox,
            JComboBox<String> urgencyBox) {

        unitsField.setText("");
        bloodGroupBox.setSelectedIndex(0);
        componentBox.setSelectedIndex(0);
        urgencyBox.setSelectedIndex(0);
    }
}
