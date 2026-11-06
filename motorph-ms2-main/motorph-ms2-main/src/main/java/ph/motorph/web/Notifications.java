package ph.motorph.web;

import java.util.ArrayList;
import java.util.List;

/**
 * In-app notifications (design doc scenarios: "notifies the member/employee"): a schedule change, a leave, overtime or
 * pay-change decision, a timesheet correction, a resolved ticket, or released pay. Each person sees only their own.
 * Keeps the newest KEEP per person.
 */
public final class Notifications {
    static final int KEEP = 100;

    public static final class Item {
        public final String id, employeeId, at, kind, text;
        public boolean read;

        Item(String id, String employeeId, String at, String kind, String text) {
            this.id = id;
            this.employeeId = employeeId;
            this.at = at;
            this.kind = kind;
            this.text = text;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private int next = 1;

    /** Nobody is notified about their own action. */
    public synchronized void notify(String employeeId, String actorId, String at, String kind, String text) {
        if (employeeId == null || employeeId.isEmpty() || employeeId.equals(actorId)) return;
        items.add(new Item(String.format("NT-%05d", next++), employeeId, at, kind, text));
        int mine = 0;
        for (int i = items.size() - 1; i >= 0; i--) {
            if (!items.get(i).employeeId.equals(employeeId)) continue;
            if (++mine > KEEP) items.remove(i);
        }
    }

    /** Newest first. */
    public synchronized List<Item> forEmployee(String employeeId) {
        List<Item> mine = new ArrayList<>();
        for (int i = items.size() - 1; i >= 0; i--) if (items.get(i).employeeId.equals(employeeId)) mine.add(items.get(i));
        return mine;
    }

    public synchronized long unread(String employeeId) {
        return items.stream().filter(item -> item.employeeId.equals(employeeId) && !item.read).count();
    }

    public synchronized int markAllRead(String employeeId) {
        int marked = 0;
        for (Item item : items) {
            if (item.employeeId.equals(employeeId) && !item.read) {
                item.read = true;
                marked++;
            }
        }
        return marked;
    }
}
