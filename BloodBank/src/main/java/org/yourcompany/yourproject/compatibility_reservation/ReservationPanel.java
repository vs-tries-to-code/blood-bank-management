
package org.yourcompany.yourproject.compatibility_reservation;

import org.yourcompany.yourproject.inventory.BloodComponent;
import org.yourcompany.yourproject.inventory.BloodGroup;
import org.yourcompany.yourproject.inventory.BloodUnit;

import javax.swing.*;
import java.awt.*;

public class ReservationPanel extends JFrame {

    private JComboBox<BloodGroup> bloodGroupBox;
    private JComboBox<BloodComponent> componentBox;
    private JTextField patientIdField;
    private JTextField reservationIdField;

    private JLabel unitIdLabel;
    private JLabel unitGroupLabel;
    private JLabel unitComponentLabel;
    private JLabel unitStatusLabel;
    private JLabel reservationIdLabel;

    private final Reservation reservation;

    private int currentReservationId = -1;
    private BloodUnit reservedUnit;

    public ReservationPanel() {

        reservation = new Reservation();

        setTitle("Blood Bank - Reservation");
        setSize(650, 550);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Title
        JLabel title = new JLabel(
                "Blood Reservation",
                SwingConstants.CENTER
        );
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));
        add(title, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 12));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 35, 15, 35));

        formPanel.add(new JLabel("Patient ID:"));
        patientIdField = new JTextField();
        formPanel.add(patientIdField);

        formPanel.add(new JLabel("Recipient Blood Group:"));
        bloodGroupBox = new JComboBox<>(BloodGroup.values());
        formPanel.add(bloodGroupBox);

        formPanel.add(new JLabel("Blood Component:"));
        componentBox = new JComboBox<>(BloodComponent.values());
        formPanel.add(componentBox);

        formPanel.add(new JLabel("Reservation ID to cancel:"));
        reservationIdField = new JTextField();
        formPanel.add(reservationIdField);

        JButton reserveButton = new JButton("Find and Reserve");
        JButton cancelButton = new JButton("Cancel Reservation");

        formPanel.add(reserveButton);
        formPanel.add(cancelButton);

        add(formPanel, BorderLayout.CENTER);

        // Reservation details
        JPanel detailsPanel = new JPanel(new GridLayout(5, 2, 10, 8));
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Reservation Details"),
                BorderFactory.createEmptyBorder(10, 20, 15, 20)
        ));

        detailsPanel.add(new JLabel("Reservation ID:"));
        reservationIdLabel = new JLabel("-");
        detailsPanel.add(reservationIdLabel);

        detailsPanel.add(new JLabel("Blood Unit ID:"));
        unitIdLabel = new JLabel("-");
        detailsPanel.add(unitIdLabel);

        detailsPanel.add(new JLabel("Blood Group:"));
        unitGroupLabel = new JLabel("-");
        detailsPanel.add(unitGroupLabel);

        detailsPanel.add(new JLabel("Component:"));
        unitComponentLabel = new JLabel("-");
        detailsPanel.add(unitComponentLabel);

        detailsPanel.add(new JLabel("Status:"));
        unitStatusLabel = new JLabel("-");
        detailsPanel.add(unitStatusLabel);

        add(detailsPanel, BorderLayout.SOUTH);

        // Reserve button
        reserveButton.addActionListener(e -> reserveBlood());

        // Cancel button
        cancelButton.addActionListener(e -> cancelReservation());
    }

    private void reserveBlood() {

        String patientText = patientIdField.getText().trim();

        if (patientText.isEmpty()) {
            showMessage("Please enter the Patient ID.");
            return;
        }

        int patientId;

        try {
            patientId = Integer.parseInt(patientText);

            if (patientId <= 0) {
                showMessage("Patient ID must be a positive number.");
                return;
            }
        } catch (NumberFormatException ex) {
            showMessage("Patient ID must be a valid integer.");
            return;
        }

        BloodGroup recipient =
                (BloodGroup) bloodGroupBox.getSelectedItem();

        BloodComponent component =
                (BloodComponent) componentBox.getSelectedItem();

        if (recipient == null || component == null) {
            showMessage("Please select a blood group and component.");
            return;
        }

        // Find a compatible candidate.
        BloodUnit candidate =
                reservation.reserveAvailableUnit(recipient, component);

        if (candidate == null) {
            clearDetails();
            showMessage("No compatible blood unit was found.");
            return;
        }

        // The database repository performs the actual reservation.
        // A successful result is the generated reservation ID.
        int newReservationId =
                reservation.saveReservation(patientId, candidate);

        if (newReservationId == -1) {
            clearDetails();
            showMessage(
                    "Reservation failed. The unit may no longer be available, " +
                    "the patient may not exist, or a database error occurred."
            );
            return;
        }

        currentReservationId = newReservationId;
        reservedUnit = candidate;

        reservationIdField.setText(String.valueOf(newReservationId));
        reservationIdLabel.setText(String.valueOf(newReservationId));
        unitIdLabel.setText(String.valueOf(candidate.getUnitId()));
        unitGroupLabel.setText(candidate.getBloodGroup().toString());
        unitComponentLabel.setText(candidate.getComponent().toString());
        unitStatusLabel.setText("RESERVED");

        showMessage(
                "Reservation successful!\nReservation ID: " + newReservationId +
                "\nBlood Unit ID: " + candidate.getUnitId()
        );
    }

    private void cancelReservation() {

        String idText = reservationIdField.getText().trim();

        int reservationId;

        if (idText.isEmpty()) {
            reservationId = currentReservationId;
        } else {
            try {
                reservationId = Integer.parseInt(idText);

                if (reservationId <= 0) {
                    showMessage("Reservation ID must be a positive number.");
                    return;
                }
            } catch (NumberFormatException ex) {
                showMessage("Reservation ID must be a valid integer.");
                return;
            }
        }

        if (reservationId == -1) {
            showMessage("Enter a reservation ID to cancel.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Cancel reservation " + reservationId + "?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        boolean cancelled = reservation.cancelReservation(reservationId);

        if (cancelled) {
            if (reservationId == currentReservationId) {
                currentReservationId = -1;
                reservedUnit = null;
            }

            clearDetails();
            reservationIdField.setText("");

            showMessage(
                    "Reservation " + reservationId +
                    " was cancelled successfully."
            );
        } else {
            showMessage(
                    "Cancellation failed. Check that the reservation exists " +
                    "and is still RESERVED."
            );
        }
    }

    private void clearDetails() {
        reservationIdLabel.setText("-");
        unitIdLabel.setText("-");
        unitGroupLabel.setText("-");
        unitComponentLabel.setText("-");
        unitStatusLabel.setText("-");
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Blood Reservation",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ReservationPanel panel = new ReservationPanel();
            panel.setVisible(true);
        });
    }
}
