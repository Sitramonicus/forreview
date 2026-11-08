package motorphpayrollsystem;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class Toast {

    public static final int SUCCESS = 0;
    public static final int ERROR   = 1;
    public static final int WARNING = 2;
    public static final int INFO    = 3;

    private static final Color[] BG = {
        new Color(22, 163, 74),   // SUCCESS - green
        new Color(220, 38, 38),   // ERROR   - red
        new Color(217, 119, 6),   // WARNING - amber
        new Color(0, 51, 153),    // INFO    - blue
    };
    private static final String[] ICONS = {
        "✔  ", "✖  ", "⚠  ", "ℹ  "
    };

    public static void show(Component parent, String message, int type) {
        SwingUtilities.invokeLater(() -> {
            JWindow toast = new JWindow(SwingUtilities.getWindowAncestor(parent));
            toast.setLayout(new BorderLayout());

            JLabel lbl = new JLabel(ICONS[type] + message);
            lbl.setFont(new Font("Dialog", Font.BOLD, 13));
            lbl.setForeground(Color.WHITE);
            lbl.setBorder(new EmptyBorder(12, 18, 12, 18));
            lbl.setOpaque(true);
            lbl.setBackground(BG[type]);

            toast.add(lbl);
            toast.pack();

            // Position bottom-right of parent window
            Window win = SwingUtilities.getWindowAncestor(parent);
            if (win != null) {
                int x = win.getX() + win.getWidth()  - toast.getWidth()  - 20;
                int y = win.getY() + win.getHeight() - toast.getHeight() - 40;
                toast.setLocation(x, y);
            } else {
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                toast.setLocation(screen.width - toast.getWidth() - 20,
                                  screen.height - toast.getHeight() - 60);
            }

            toast.setVisible(true);

            // Auto-dismiss after 3 seconds
            Timer timer = new Timer(3000, e -> toast.dispose());
            timer.setRepeats(false);
            timer.start();
        });
    }

    /** Shortcut methods */
    public static void success(Component p, String msg) { show(p, msg, SUCCESS); }
    public static void error  (Component p, String msg) { show(p, msg, ERROR);   }
    public static void warning(Component p, String msg) { show(p, msg, WARNING); }
    public static void info   (Component p, String msg) { show(p, msg, INFO);    }
}
