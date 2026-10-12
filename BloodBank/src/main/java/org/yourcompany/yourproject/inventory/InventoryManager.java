
package org.yourcompany.yourproject.inventory;

import com.bloodbank.common.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InventoryManager {

    public List<BloodUnit> getAllBloodUnits() {

        List<BloodUnit> inventoryList = new ArrayList<>();

        String query = "SELECT * FROM blood_inventory";

        try (Connection con = DatabaseConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {

                int unitId = rs.getInt("unit_id");

                BloodGroup bloodGroup =
                        BloodGroup.valueOf(
                                rs.getString("blood_group")
                        );

                BloodComponent bloodComponent =
                        BloodComponent.valueOf(
                                rs.getString("component")
                        );

                String donorPhone =
                        rs.getString("donor_phone");

                LocalDate collectionDate =
                        rs.getDate("collection_date").toLocalDate();

                BloodStatus status =
                        BloodStatus.valueOf(
                                rs.getString("status")
                        );

                BloodUnit unit =
                        new BloodUnit(
                                unitId,
                                bloodGroup,
                                bloodComponent,
                                donorPhone,
                                collectionDate,
                                status
                        );

                inventoryList.add(unit);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return inventoryList;
    }


    public boolean addBloodUnit(BloodUnit unit) {

        String query =
                "INSERT INTO blood_inventory " +
                "(blood_group, component, donor_phone, " +
                "collection_date, expiry_date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn =
                     DatabaseConnection.getConnection();
             PreparedStatement pstmt =
                     conn.prepareStatement(query)) {

            pstmt.setString(
                    1,
                    unit.getBloodGroup().name()
            );

            pstmt.setString(
                    2,
                    unit.getComponent().name()
            );

            pstmt.setString(
                    3,
                    unit.getDonorPhone()
            );

            pstmt.setDate(
                    4,
                    Date.valueOf(unit.getCollectionDate())
            );

            pstmt.setDate(
                    5,
                    Date.valueOf(unit.getExpiryDate())
            );

            pstmt.setString(
                    6,
                    unit.getStatus().name()
            );

            int rowsAffected =
                    pstmt.executeUpdate();

            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // Count available, non-expired units for a blood group and component
    public int getAvailableStock(
            BloodGroup bloodGroup,
            BloodComponent component) {

        String query =
                "SELECT COUNT(*) FROM blood_inventory " +
                "WHERE blood_group = ? " +
                "AND component = ? " +
                "AND status = 'AVAILABLE' " +
                "AND expiry_date >= ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(query)) {

            pstmt.setString(1, bloodGroup.name());
            pstmt.setString(2, component.name());
            pstmt.setDate(3, Date.valueOf(LocalDate.now()));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }

        return -1;
    }
}
