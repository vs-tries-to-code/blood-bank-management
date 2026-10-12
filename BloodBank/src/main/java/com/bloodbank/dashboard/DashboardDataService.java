package com.bloodbank.dashboard;

import com.bloodbank.common.DatabaseConnection;
import org.yourcompany.yourproject.inventory.BloodComponent;
import org.yourcompany.yourproject.inventory.BloodGroup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class DashboardDataService {

    public Snapshot load(String hospitalName) throws SQLException {
        Map<BloodGroup, Map<BloodComponent, Integer>> availability = new EnumMap<>(BloodGroup.class);
        for (BloodGroup group : BloodGroup.values()) {
            Map<BloodComponent, Integer> componentCounts = new EnumMap<>(BloodComponent.class);
            for (BloodComponent component : BloodComponent.values()) {
                componentCounts.put(component, 0);
            }
            availability.put(group, componentCounts);
        }

        int availableUnits = 0;
        int pendingRequests;
        int donationRecords;
        int hospitalRequests;

        try (Connection connection = DatabaseConnection.getConnection()) {
            String availabilitySql = "SELECT blood_group, component, COUNT(*) AS unit_count "
                    + "FROM blood_inventory WHERE status = 'AVAILABLE' "
                    + "AND expiry_date >= CURRENT_DATE GROUP BY blood_group, component";
            try (PreparedStatement statement = connection.prepareStatement(availabilitySql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    BloodGroup group = BloodGroup.valueOf(resultSet.getString("blood_group"));
                    BloodComponent component = BloodComponent.valueOf(resultSet.getString("component"));
                    int count = resultSet.getInt("unit_count");
                    availability.get(group).put(component, count);
                    availableUnits += count;
                }
            }

            pendingRequests = count(connection,
                    "SELECT COUNT(*) FROM hospital_requests WHERE status = 'PENDING'");
            donationRecords = count(connection,
                    "SELECT COUNT(*) FROM donation_records");

            String hospitalRequestsSql =
                    "SELECT COUNT(*) FROM hospital_requests WHERE hospital_name = ?";
            try (PreparedStatement statement = connection.prepareStatement(hospitalRequestsSql)) {
                statement.setString(1, hospitalName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    hospitalRequests = resultSet.getInt(1);
                }
            }
        }

        return new Snapshot(availableUnits, pendingRequests, donationRecords,
                hospitalRequests, availability);
    }

    private int count(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    public record Snapshot(
            int availableUnits,
            int pendingRequests,
            int donationRecords,
            int hospitalRequests,
            Map<BloodGroup, Map<BloodComponent, Integer>> availability) {

        public Snapshot {
            Map<BloodGroup, Map<BloodComponent, Integer>> copy = new EnumMap<>(BloodGroup.class);
            availability.forEach((group, counts) -> {
                Map<BloodComponent, Integer> componentCopy = new EnumMap<>(BloodComponent.class);
                componentCopy.putAll(counts);
                copy.put(group, Collections.unmodifiableMap(componentCopy));
            });
            availability = Collections.unmodifiableMap(copy);
        }

        public int available(BloodGroup group, BloodComponent component) {
            return availability.getOrDefault(group, Map.of()).getOrDefault(component, 0);
        }
    }
}
