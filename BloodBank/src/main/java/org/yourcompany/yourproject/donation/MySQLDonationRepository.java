package org.yourcompany.yourproject.donation;

import com.bloodbank.common.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MySQLDonationRepository {

    public void save(int donorId, LocalDate donationDate, int quantityMl) throws SQLException {
        String sql = "INSERT INTO donation_records (donor_id, donation_date, quantity_ml) "
                + "VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donorId);
            statement.setDate(2, Date.valueOf(donationDate));
            statement.setInt(3, quantityMl);
            statement.executeUpdate();
        }
    }

    public List<DonationHistory> findAll() throws SQLException {
        String sql = "SELECT dr.donation_id, dr.donor_id, d.donor_name, d.blood_group, "
                + "dr.donation_date, dr.quantity_ml "
                + "FROM donation_records dr LEFT JOIN donors d ON d.donor_id = dr.donor_id "
                + "ORDER BY dr.donation_date DESC, dr.donation_id DESC";
        List<DonationHistory> donations = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String donorId = resultSet.getString("donor_id");
                String donorName = resultSet.getString("donor_name");
                String bloodGroup = resultSet.getString("blood_group");
                donations.add(new DonationHistory(
                        resultSet.getString("donation_id"),
                        donorId == null ? "" : donorId,
                        donorName == null ? "Unknown donor" : donorName,
                        bloodGroup == null ? "Unknown" : bloodGroup,
                        resultSet.getDate("donation_date").toLocalDate().toString(),
                        resultSet.getInt("quantity_ml")
                ));
            }
        }

        return donations;
    }
}