package com.bloodbank.dashboard;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class DashboardTheme {
    public static final Color BLOOD_RED = new Color(171, 24, 36);
    public static final Color BLOOD_DARK = new Color(102, 0, 15);
    public static final Color BACKGROUND = new Color(245, 247, 249);
    public static final Color PANEL = new Color(255, 255, 255);
    public static final Color ACCENT_SOFT = new Color(255, 239, 242);
    public static final Color TEXT = new Color(28, 34, 40);
    public static final Color MUTED = new Color(97, 112, 127);
    public static final Color BORDER = new Color(224, 229, 235);

    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 30);
    public static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 13);

    private DashboardTheme() {
    }

    public static void apply() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // Nimbus is preferred, but the app should still work with the default LF.
        }

        UIManager.put("control", BACKGROUND);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("ScrollPane.background", BACKGROUND);
        UIManager.put("TextField.background", PANEL);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.border", new RoundedBorder(BORDER, 12));
        UIManager.put("PasswordField.background", PANEL);
        UIManager.put("PasswordField.foreground", TEXT);
        UIManager.put("PasswordField.border", new RoundedBorder(BORDER, 12));
        UIManager.put("ComboBox.background", PANEL);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("Table.background", PANEL);
        UIManager.put("Table.gridColor", new Color(229, 234, 240));
        UIManager.put("Table.selectionBackground", BLOOD_RED);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.font", BODY_FONT);
        UIManager.put("Button.background", BLOOD_RED);
        UIManager.put("Button.foreground", Color.WHITE);
    }

    public static JButton createActionButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(BODY_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(BLOOD_RED);
        button.setBorder(new RoundedBorder(BLOOD_RED, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(140, 42));
        return button;
    }

    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(BODY_FONT.deriveFont(Font.BOLD));
        button.setForeground(BLOOD_RED);
        button.setBackground(new Color(255, 246, 247));
        button.setBorder(new RoundedBorder(BLOOD_RED, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(160, 42));
        return button;
    }

    public static JPanel createStatCard(String labelText, String valueText, Color accent) {
        JLabel valueLabel = new JLabel(valueText, SwingConstants.CENTER);
        return createStatCard(labelText, valueLabel, accent);
    }

    public static JPanel createStatCard(String labelText, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setOpaque(true);
        card.setBackground(PANEL);
        card.setBorder(new RoundedBorder(accent, 18));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accent.darker());
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel label = new JLabel(labelText, SwingConstants.CENTER);
        label.setFont(LABEL_FONT);
        label.setForeground(MUTED);

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(18, 18, 18, 18));
        content.add(valueLabel, BorderLayout.CENTER);
        content.add(label, BorderLayout.SOUTH);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    public static JLabel createSectionTitle(String title) {
        JLabel label = new JLabel(title);
        label.setFont(TITLE_FONT);
        label.setForeground(TEXT);
        return label;
    }

    public static final class RoundedBorder implements Border {
        private final Color color;
        private final int radius;

        public RoundedBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 8, 8, 8);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
