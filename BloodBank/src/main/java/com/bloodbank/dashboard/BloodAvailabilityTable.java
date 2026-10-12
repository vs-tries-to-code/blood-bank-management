package com.bloodbank.dashboard;

import org.yourcompany.yourproject.inventory.BloodComponent;
import org.yourcompany.yourproject.inventory.BloodGroup;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class BloodAvailabilityTable extends JTable {

    private final DefaultTableModel model;

    public BloodAvailabilityTable() {
        Object[] columns = new Object[BloodComponent.values().length + 1];
        columns[0] = "Blood Group";
        for (int index = 0; index < BloodComponent.values().length; index++) {
            columns[index + 1] = BloodComponent.values()[index];
        }

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        setModel(model);
        setFillsViewportHeight(true);
        setRowHeight(30);

        for (BloodGroup group : BloodGroup.values()) {
            Object[] row = new Object[columns.length];
            row[0] = group;
            for (int index = 1; index < row.length; index++) {
                row[index] = "...";
            }
            model.addRow(row);
        }
    }

    public void showSnapshot(DashboardDataService.Snapshot snapshot) {
        for (int row = 0; row < BloodGroup.values().length; row++) {
            BloodGroup group = BloodGroup.values()[row];
            for (int column = 0; column < BloodComponent.values().length; column++) {
                model.setValueAt(snapshot.available(group, BloodComponent.values()[column]), row, column + 1);
            }
        }
    }

    public void showUnavailable() {
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int column = 1; column < model.getColumnCount(); column++) {
                model.setValueAt("—", row, column);
            }
        }
    }
}
