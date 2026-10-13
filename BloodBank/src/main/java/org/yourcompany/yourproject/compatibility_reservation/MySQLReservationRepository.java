
package org.yourcompany.yourproject.compatibility_reservation;

import com.bloodbank.common.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MySQLReservationRepository implements ReservationRepository {

    @Override
    public int save(int patientId, int unitId) {

        String reserveUnitSql =
                "UPDATE blood_inventory " +
                "SET status = 'RESERVED' " +
                "WHERE unit_id = ? " +
                "AND status = 'AVAILABLE' " +
                "AND expiry_date >= CURRENT_DATE";

        String insertReservationSql =
                "INSERT INTO reservations " +
                "(patient_id, unit_id, reservation_date, status) " +
                "VALUES (?, ?, ?, 'RESERVED')";

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                // Step 1: Reserve the unit only if it is available.
                try (PreparedStatement update =
                             conn.prepareStatement(reserveUnitSql)) {

                    update.setInt(1, unitId);

                    if (update.executeUpdate() != 1) {
                        conn.rollback();
                        return -1;
                    }
                }

                // Step 2: Insert the reservation and retrieve its ID.
                try (PreparedStatement insert =
                             conn.prepareStatement(
                                     insertReservationSql,
                                     Statement.RETURN_GENERATED_KEYS)) {

                    insert.setInt(1, patientId);
                    insert.setInt(2, unitId);
                    insert.setDate(3, Date.valueOf(LocalDate.now()));

                    if (insert.executeUpdate() != 1) {
                        conn.rollback();
                        return -1;
                    }

                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (keys.next()) {
                            int reservationId = keys.getInt(1);

                            // Step 3: Commit both database changes.
                            conn.commit();
                            return reservationId;
                        }
                    }
                }

                conn.rollback();
                return -1;

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return -1;

            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public boolean cancel(int reservationId) {

        String findSql =
                "SELECT unit_id FROM reservations " +
                "WHERE reservation_id = ? " +
                "AND status = 'RESERVED' FOR UPDATE";

        String cancelSql =
                "UPDATE reservations SET status = 'CANCELLED' " +
                "WHERE reservation_id = ? AND status = 'RESERVED'";

        String releaseSql =
                "UPDATE blood_inventory SET status = CASE " +
                "WHEN expiry_date < CURRENT_DATE THEN 'EXPIRED' " +
                "ELSE 'AVAILABLE' END " +
                "WHERE unit_id = ? AND status = 'RESERVED'";

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                int unitId;

                // Step 1: Find and lock the active reservation.
                try (PreparedStatement find = conn.prepareStatement(findSql)) {
                    find.setInt(1, reservationId);

                    try (ResultSet rs = find.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }

                        unitId = rs.getInt("unit_id");
                    }
                }

                // Step 2: Cancel the reservation.
                try (PreparedStatement cancel = conn.prepareStatement(cancelSql)) {
                    cancel.setInt(1, reservationId);

                    if (cancel.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }

                // Step 3: Release the unit, accounting for expiry.
                try (PreparedStatement release = conn.prepareStatement(releaseSql)) {
                    release.setInt(1, unitId);

                    if (release.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;

            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public ReservationRecord findById(int reservationId) {

        String query =
                "SELECT * FROM reservations WHERE reservation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, reservationId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<ReservationRecord> findAll() {

        List<ReservationRecord> reservations = new ArrayList<>();

        String query =
                "SELECT * FROM reservations ORDER BY reservation_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                reservations.add(mapRecord(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reservations;
    }

    private ReservationRecord mapRecord(ResultSet rs) throws SQLException {

        return new ReservationRecord(
                rs.getInt("reservation_id"),
                rs.getInt("patient_id"),
                rs.getInt("unit_id"),
                rs.getDate("reservation_date").toLocalDate(),
                rs.getString("status")
        );
    }
}
