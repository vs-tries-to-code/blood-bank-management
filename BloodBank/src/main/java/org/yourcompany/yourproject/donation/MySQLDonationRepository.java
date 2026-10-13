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

    public void save(Donor donor, LocalDate donationDate, int quantityMl) throws SQLException {
        String upsertDonorSql = "INSERT INTO donors "
                + "(donor_name, blood_group, phone_number, age, gender) VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE donor_name = VALUES(donor_name), "
                + "blood_group = VALUES(blood_group), age = VALUES(age), gender = VALUES(gender)";
        String findDonorSql = "SELECT donor_id FROM donors WHERE phone_number = ?";
        String insertDonationSql = "INSERT INTO donation_records "
                + "(donor_id, donation_date, quantity_ml) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(upsertDonorSql)) {
                    statement.setString(1, donor.getName());
                    statement.setString(2, donor.getBloodGroup());
                    statement.setString(3, donor.getPhone());
                    statement.setInt(4, donor.getAge());
                    statement.setString(5, donor.getGender());
                    statement.executeUpdate();
                }

                int donorId;
                try (PreparedStatement statement = connection.prepareStatement(findDonorSql)) {
                    statement.setString(1, donor.getPhone());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Could not find donor after saving donor details.");
                        }
                        donorId = resultSet.getInt("donor_id");
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(insertDonationSql)) {
                    statement.setInt(1, donorId);
                    statement.setDate(2, Date.valueOf(donationDate));
                    statement.setInt(3, quantityMl);
                    statement.executeUpdate();
                }

                connection.commit();
            } catch (SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
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