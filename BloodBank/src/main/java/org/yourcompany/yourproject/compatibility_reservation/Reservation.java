package org.yourcompany.yourproject.compatibility_reservation;

import org.yourcompany.yourproject.inventory.BloodComponent;
import org.yourcompany.yourproject.inventory.BloodGroup;
import org.yourcompany.yourproject.inventory.BloodUnit;
import org.yourcompany.yourproject.inventory.InventoryManager;

import java.util.List;

public class Reservation {

    private final CompatibilityChecker checker;
    private final ReservationRepository reservationRepository;

    public Reservation() {
        checker = new CompatibilityChecker();
        reservationRepository = new MySQLReservationRepository();
    }

    // Check compatibility without changing the unit's status.
    public boolean reserve(BloodGroup recipient, BloodUnit unit) {
        return checker.isCompatible(recipient, unit);
    }

    // Find a compatible unit that appears available in inventory.
    public BloodUnit reserveAvailableUnit(
            BloodGroup recipient,
            BloodComponent component) {

        InventoryManager inventoryManager = new InventoryManager();
        List<BloodUnit> units = inventoryManager.getAllBloodUnits();

        for (BloodUnit unit : units) {
            if (unit.getComponent() == component
                    && reserve(recipient, unit)) {
                return unit;
            }
        }

        return null;
    }

    // Save the reservation using the database transaction.
    public int saveReservation(int patientId, BloodUnit unit) {
        if (unit == null) {
            return -1;
        }

        return reservationRepository.save(
                patientId, unit.getUnitId());
    }

    public boolean cancelReservation(int reservationId) {
        return reservationRepository.cancel(reservationId);
    }

    public ReservationRecord getReservation(int reservationId) {
        return reservationRepository.findById(reservationId);
    }

    public List<ReservationRecord> getAllReservations() {
        return reservationRepository.findAll();
    }
}