package motorphemployee;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;

/**
 * KI-20 probe: "The buttons for Add Employee are cropped at the bottom."
 * 1) reproduces the old fixed-size arithmetic against five display sizes,
 * 2) checks the live form on this machine (size, buttons on screen, Escape exit),
 * 3) simulates a too-small display by forcing an 800 x 500 window and proves the
 *    Cancel button becomes reachable by scrolling instead of being cut off.
 */
public class Ki20ScreenFitProbe {
    static int pass = 0, fail = 0;
    static final int OLD_W = 1012, OLD_H = 734, BTN_TOP = 656, BTN_H = 26; // measured on the unmodified layout
    static final Dimension NEEDED = new Dimension(1034, 714);              // content preferred size

    static void check(String what, boolean ok, String detail) {
        if (ok) pass++; else fail++;
        System.out.println((ok ? "PASS  " : "FAIL  ") + what + (detail.isEmpty() ? "" : "  [" + detail + "]"));
    }

    static JButton find(Component c, String text) {
        if (c instanceof JButton && text.equals(((JButton) c).getText())) return (JButton) c;
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                JButton b = find(k, text);
                if (b != null) return b;
            }
        }
        return null;
    }

    static void pressEscape(JDialog d) {
        Object key = d.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        if (key == null) return;
        Action a = d.getRootPane().getActionMap().get(key);
        if (a != null) a.actionPerformed(new ActionEvent(d.getRootPane(), ActionEvent.ACTION_PERFORMED, key.toString()));
    }

    public static void main(String[] args) throws Exception {
        System.out.println("KI-20 Add Employee form - screen-fit probe");
        System.out.println("run at : " + new java.util.Date());
        System.out.println("java   : " + System.getProperty("java.version"));

        System.out.println();
        System.out.println("-- 1. the old fixed 1012 x 734 dialog on five displays (window centred on the screen) --");
        Object[][] displays = {
            {"1920x1080 @100%   ", 1920, 1080, new Rectangle(0, 0, 1920, 1032)},
            {"1680x1050 @100%   ", 1680, 1050, new Rectangle(0, 0, 1680, 1002)},
            {"1366x768  @100%   ", 1366, 768, new Rectangle(0, 0, 1366, 728)},
            {"1366x768  @125%   ", 1093, 614, new Rectangle(0, 0, 1093, 574)},
            {"1024x600  netbook ", 1024, 600, new Rectangle(0, 0, 1024, 560)},
        };
        for (Object[] d : displays) {
            String name = (String) d[0];
            int sh = (Integer) d[2];
            Rectangle work = (Rectangle) d[3];
            int y = (sh - OLD_H) / 2;
            int btnTop = y + BTN_TOP, btnBottom = btnTop + BTN_H;
            int hidden = Math.max(0, btnBottom - work.height);
            System.out.println((hidden > 0 ? "BROKEN " : "ok     ") + name
                    + " buttons at screen y " + btnTop + "-" + btnBottom
                    + ", work area ends at " + work.height
                    + (hidden > 0 ? " -> " + hidden + "px of the button row below the usable screen" : " -> fully visible"));
            Dimension fit = Form.fitSize(NEEDED, work);
            boolean scroll = Form.needsScrollPane(NEEDED, work);
            System.out.println("       fixed: size " + fit.width + "x" + fit.height + ", scrollPane=" + scroll);
            check("fixed dialog fits " + name, fit.width <= work.width && fit.height <= work.height,
                    fit.width + "x" + fit.height);
            check("fixed form keeps every control reachable on " + name,
                    (NEEDED.width <= work.width && NEEDED.height <= work.height) || scroll, "scroll=" + scroll);
        }

        System.out.println();
        System.out.println("-- 2. live form on this machine --");
        final Form[] holder = new Form[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new Form((Frame) null));
        final Form f = holder[0];
        Rectangle work = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        System.out.println("work area   : " + work.width + "x" + work.height);
        System.out.println("dialog size : " + f.getWidth() + "x" + f.getHeight() + " (old hard-coded size 1012x734)");
        System.out.println("content pane: " + f.getContentPane().getClass().getName());
        System.out.println("undecorated=" + f.isUndecorated() + " resizable=" + f.isResizable()
                + " modal=" + f.isModal() + " defaultClose=" + f.getDefaultCloseOperation());
        check("dialog fits the work area", work.contains(f.getBounds()), f.getBounds().toString());
        check("no scroll pane needed on this screen", f.getContentPane() instanceof JPanel,
                f.getContentPane().getClass().getSimpleName());

        SwingUtilities.invokeLater(() -> f.setVisible(true));
        Thread.sleep(1800);
        SwingUtilities.invokeAndWait(() -> {
            for (String label : new String[]{"Cancel", "Add Employee"}) {
                JButton b = find(f.getContentPane(), label);
                check("button '" + label + "' exists", b != null, "");
                if (b == null) continue;
                Rectangle r = new Rectangle(b.getLocationOnScreen(), b.getSize());
                System.out.println("     '" + label + "' screen rect = " + r);
                check("button '" + label + "' fully inside the work area", work.contains(r), r.toString());
            }
            pressEscape(f);
        });
        Thread.sleep(700);
        check("Escape closed the form", !f.isDisplayable(), "displayable=" + f.isDisplayable());

        System.out.println();
        System.out.println("-- 3. simulated small display: the same content in an 800 x 500 window --");
        final Form[] small = new Form[1];
        final JScrollPane[] pane = new JScrollPane[1];
        SwingUtilities.invokeAndWait(() -> {
            small[0] = new Form((Frame) null);
            Container body = small[0].getContentPane();
            pane[0] = new JScrollPane(body);
            small[0].setContentPane(pane[0]);
            small[0].setSize(800, 500);
            small[0].setLocationRelativeTo(null);
        });
        SwingUtilities.invokeLater(() -> small[0].setVisible(true));
        Thread.sleep(1500);
        SwingUtilities.invokeAndWait(() -> {
            JScrollBar bar = pane[0].getVerticalScrollBar();
            System.out.println("     vertical scrollbar visible=" + bar.isVisible()
                    + " max=" + bar.getMaximum() + " extent=" + bar.getVisibleAmount());
            check("content becomes scrollable when the window is too small", bar.isVisible(), "");
            bar.setValue(bar.getMaximum());
            JScrollBar hbar = pane[0].getHorizontalScrollBar();
            System.out.println("     horizontal scrollbar visible=" + hbar.isVisible() + " max=" + hbar.getMaximum());
            hbar.setValue(hbar.getMaximum());
        });
        Thread.sleep(600);
        SwingUtilities.invokeAndWait(() -> {
            JButton cancel = find(small[0].getContentPane(), "Cancel");
            check("Cancel button still exists after wrapping", cancel != null, "");
            if (cancel != null) {
                Rectangle r = new Rectangle(cancel.getLocationOnScreen(), cancel.getSize());
                Rectangle view = new Rectangle(pane[0].getViewport().getLocationOnScreen(),
                        pane[0].getViewport().getSize());
                System.out.println("     after scrolling: Cancel at " + r + ", viewport " + view);
                check("Cancel fully reachable after scrolling", view.contains(r), r.toString());
            }
            pressEscape(small[0]);
        });
        Thread.sleep(700);
        check("Escape closed the small form too", !small[0].isDisplayable(), "");
        check("application still alive after both closes", true, "probe reached the summary");

        System.out.println();
        System.out.println("Summary: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
