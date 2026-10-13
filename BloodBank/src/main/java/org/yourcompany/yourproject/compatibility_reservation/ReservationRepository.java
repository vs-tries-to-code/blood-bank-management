
package org.yourcompany.yourproject.compatibility_reservation;

import java.util.List;

public interface ReservationRepository {

    // Returns the new reservation ID, or -1 if reservation fails.
    int save(int patientId, int unitId);

    boolean cancel(int reservationId);

    ReservationRecord findById(int reservationId);

    List<ReservationRecord> findAll();
}
