package motorph.ui;

import javax.swing.JButton;
import javax.swing.JTextField;

/**
 * Static utility methods for consistent UI styling across all frames.
 * Centralizes all button and text field styling to eliminate duplication and
 * ensure consistent look-and-feel throughout the application.
 */
public class UIStyleHelper {

    // Color constants
    private static final int PRIMARY_COLOR_RED = 44;
    private static final int PRIMARY_COLOR_GREEN = 90;
    private static final int PRIMARY_COLOR_BLUE = 160;

    private static final int SECONDARY_COLOR_RED = 100;
    private static final int SECONDARY_COLOR_GREEN = 100;
    private static final int SECONDARY_COLOR_BLUE = 110;

    private static final int DANGER_COLOR_RED = 180;
    private static final int DANGER_COLOR_GREEN = 30;
    private static final int DANGER_COLOR_BLUE = 30;

    private static final int FIELD_BG_RED = 255;
    private static final int FIELD_BG_GREEN = 255;
    private static final int FIELD_BG_BLUE = 255;

    private static final int FIELD_FG_RED = 30;
    private static final int FIELD_FG_GREEN = 30;
    private static final int FIELD_FG_BLUE = 50;

    private static final int BORDER_COLOR_RED = 200;
    private static final int BORDER_COLOR_GREEN = 200;
    private static final int BORDER_COLOR_BLUE = 210;

    private static final int WHITE_RED = 255;
    private static final int WHITE_GREEN = 255;
    private static final int WHITE_BLUE = 255;

    /**
     * Applies standard primary button styling (blue background, white text).
     * Used for main action buttons (Save, Search, Compute, etc.).
     *
     * @param button the JButton to style
     */
    public static void applyButtonStyle(JButton button) {
        if (button == null) return;
        button.setBackground(new java.awt.Color(PRIMARY_COLOR_RED, PRIMARY_COLOR_GREEN, PRIMARY_COLOR_BLUE));
        button.setForeground(new java.awt.Color(WHITE_RED, WHITE_GREEN, WHITE_BLUE));
        button.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.setOpaque(true);
    }

    /**
     * Applies standard secondary button styling (gray background, white text).
     * Used for auxiliary action buttons (Back, Cancel, Show All, Exit, etc.).
     *
     * @param button the JButton to style
     */
    public static void applySecondaryButtonStyle(JButton button) {
        if (button == null) return;
        button.setBackground(new java.awt.Color(SECONDARY_COLOR_RED, SECONDARY_COLOR_GREEN, SECONDARY_COLOR_BLUE));
        button.setForeground(new java.awt.Color(WHITE_RED, WHITE_GREEN, WHITE_BLUE));
        button.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.setOpaque(true);
    }

    /**
     * Applies danger button styling (red background, white text).
     * Used for destructive action buttons (Delete, etc.).
     *
     * @param button the JButton to style
     */
    public static void applyDangerButtonStyle(JButton button) {
        if (button == null) return;
        button.setBackground(new java.awt.Color(DANGER_COLOR_RED, DANGER_COLOR_GREEN, DANGER_COLOR_BLUE));
        button.setForeground(new java.awt.Color(WHITE_RED, WHITE_GREEN, WHITE_BLUE));
        button.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.setOpaque(true);
    }

    /**
     * Applies standard styling to a text input field (JTextField or subclasses).
     * Used for all text input fields throughout the application.
     *
     * @param field the JTextField to style
     */
    public static void applyFieldStyle(JTextField field) {
        if (field == null) return;
        field.setBackground(new java.awt.Color(FIELD_BG_RED, FIELD_BG_GREEN, FIELD_BG_BLUE));
        field.setForeground(new java.awt.Color(FIELD_FG_RED, FIELD_FG_GREEN, FIELD_FG_BLUE));
        field.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.PLAIN, 13));
        field.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(
                        new java.awt.Color(BORDER_COLOR_RED, BORDER_COLOR_GREEN, BORDER_COLOR_BLUE), 1),
                javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }
}
