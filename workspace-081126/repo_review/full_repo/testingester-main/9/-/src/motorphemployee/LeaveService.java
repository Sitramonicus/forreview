package motorphemployee;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * File-backed leave workflow for the MotorPH payroll screens.
 *
 * <p>The leave screens used to read and write {@code pendingleaves.csv},
 * {@code leaves.csv} and the leave-credit columns of {@code data.csv} on their
 * own. Two places knew the same CSV layout, so a single reversed argument
 * (Pending Leaves wrote the reason where the approval belonged) survived in one
 * call site after the other was fixed, and approving a request updated
 * {@code leaves.csv} while the request stayed in {@code pendingleaves.csv} until
 * somebody remembered to press Save.</p>
 *
 * <p>This class is the single writer for the approve and reject decisions. One call to
 * {@link #decide(int, boolean)} applies the whole decision - the request leaves
 * the pending file, one row is appended to the processed file with the columns
 * in header order, and the employee's leave credits move - and restores all
 * three files if any step fails.</p>
 *
 * <p>No Swing types are used here on purpose: the class can be exercised by a
 * console test without opening a window.</p>
 */
public class LeaveService {

    /** Value written in the {@code Approved?} column for an approved request. */
    public static final String APPROVED = "Yes";
    /** Value written in the {@code Approved?} column for a rejected request. */
    public static final String REJECTED = "No";

    /** Column order of pendingleaves.csv and of the processed leaves.csv. */
    public static final String[] LEAVE_COLUMNS = {"Employee ID", "Leave Type", "Days", "Reason", "Approved?"};

    public static final int COLUMN_EMPLOYEE_ID = 0;
    public static final int COLUMN_LEAVE_TYPE = 1;
    public static final int COLUMN_DAYS = 2;
    public static final int COLUMN_REASON = 3;
    public static final int COLUMN_APPROVAL = 4;

    private static final int COL_EMPLOYEE_ID = COLUMN_EMPLOYEE_ID;
    private static final int COL_LEAVE_TYPE = COLUMN_LEAVE_TYPE;
    private static final int COL_DAYS = COLUMN_DAYS;
    private static final int COL_REASON = COLUMN_REASON;

    private static final int DATA_LEAVE_CREDITS = 21;
    private static final int DATA_APPROVED_LEAVES = 22;
    private static final int DATA_REJECTED_LEAVES = 23;

    private final Path dataFile;
    private final Path pendingFile;
    private final Path leavesFile;

    public LeaveService(String dataFile, String pendingFile, String leavesFile) {
        this.dataFile = Paths.get(dataFile);
        this.pendingFile = Paths.get(pendingFile);
        this.leavesFile = Paths.get(leavesFile);
    }

    /** The three files the GUI uses, relative to the project directory. */
    public static LeaveService forProjectFiles() {
        return new LeaveService(
                "src/motorphemployee/data.csv",
                "src/motorphemployee/pendingleaves.csv",
                "src/motorphemployee/leaves.csv");
    }

    /** Rows of pendingleaves.csv, in file order. */
    public List<String[]> loadPending() throws IOException {
        return readRows(pendingFile);
    }

    /** Rows of leaves.csv (processed requests), in file order. */
    public List<String[]> loadProcessed() throws IOException {
        return readRows(leavesFile);
    }

    /**
     * Applies one decision to the request at {@code pendingIndex}.
     *
     * @param pendingIndex zero-based index into {@link #loadPending()}
     * @param approved     {@code true} to approve, {@code false} to reject
     * @return the row that was written to {@code leaves.csv}: employee id, leave
     *         type, days, reason, {@code Yes}/{@code No}
     * @throws IllegalArgumentException the row is gone, or the employee is unknown
     * @throws IllegalStateException    the employee has no leave credit left
     * @throws IOException              the files could not be written; all three
     *                                  files are restored before this is thrown
     */
    public String[] decide(int pendingIndex, boolean approved) throws IOException {
        return decide(pendingIndex, null, approved);
    }

    /**
     * Same as {@link #decide(int, boolean)}, but refuses when the row at
     * {@code pendingIndex} is not the request the screen showed, which happens
     * when the pending file changed while the window was open.
     *
     * @param expected the four cells the table displayed, or {@code null}
     */
    public String[] decide(int pendingIndex, String[] expected, boolean approved) throws IOException {
        List<String[]> pending = readRows(pendingFile);
        if (pendingIndex < 0 || pendingIndex >= pending.size()) {
            throw new IllegalArgumentException(
                    "That leave request is no longer in the pending list. Reopen the window and try again.");
        }
        if (expected != null && !sameRequest(pending.get(pendingIndex), expected)) {
            throw new IllegalArgumentException(
                    "The pending leave list changed while this window was open. Reopen the window and try again.");
        }

        String[] request = pending.get(pendingIndex);
        String employeeId = cell(request, COL_EMPLOYEE_ID);
        String leaveType = cell(request, COL_LEAVE_TYPE);
        String reason = reasonOf(request);
        int days = parseDays(cell(request, COL_DAYS));

        List<String[]> employees = readRows(dataFile);
        ensureUniformColumns(employees);
        int employeeRow = indexOfEmployee(employees, employeeId);
        if (employeeRow < 0) {
            throw new IllegalArgumentException("Employee ID " + employeeId + " is not in the employee data file.");
        }
        String[] employee = employees.get(employeeRow);
        int credits = parseNumber(cell(employee, DATA_LEAVE_CREDITS), "leave credits", employeeId);
        int approvedLeaves = parseNumber(cell(employee, DATA_APPROVED_LEAVES), "approved leaves", employeeId);
        int rejectedLeaves = parseNumber(cell(employee, DATA_REJECTED_LEAVES), "rejected leaves", employeeId);
        if (approved && credits <= 0) {
            throw new IllegalStateException("Employee " + employeeId + " has no leave credit left.");
        }

        // New credit values are computed before the first write so a business-rule
        // failure cannot leave the three files half updated.
        employee[DATA_LEAVE_CREDITS] = String.valueOf(approved ? credits - 1 : credits);
        employee[DATA_APPROVED_LEAVES] = String.valueOf(approved ? approvedLeaves + 1 : approvedLeaves);
        employee[DATA_REJECTED_LEAVES] = String.valueOf(approved ? rejectedLeaves : rejectedLeaves + 1);

        String[] processed = new String[] {
            employeeId, leaveType, String.valueOf(days), reason, approved ? APPROVED : REJECTED
        };

        byte[] pendingBefore = readBytes(pendingFile);
        byte[] leavesBefore = readBytes(leavesFile);
        byte[] dataBefore = readBytes(dataFile);
        try {
            pending.remove(pendingIndex);
            writeRows(pendingFile, pending);
            appendRow(leavesFile, processed);
            writeRows(dataFile, employees);
        } catch (IOException | RuntimeException failure) {
            restore(pendingFile, pendingBefore);
            restore(leavesFile, leavesBefore);
            restore(dataFile, dataBefore);
            throw failure;
        }
        return processed;
    }

    /**
     * The reason text of a leave row. Older rows were written with the columns
     * reversed and a reason that can itself contain a comma, so everything from
     * the reason column onwards is joined back into one value.
     */
    public static String reasonOf(String[] row) {
        if (row.length <= COL_REASON) {
            return "";
        }
        String[] tail = Arrays.copyOfRange(row, COL_REASON, row.length);
        return String.join(",", tail).trim();
    }

    /** Pending row (Employee ID, Leave Type, Days, Reason) for the Pending Leaves table. */
    public static Object[] pendingTableRow(String[] row) {
        return new Object[] {
            cell(row, COL_EMPLOYEE_ID), cell(row, COL_LEAVE_TYPE), cell(row, COL_DAYS), reasonOf(row)
        };
    }

    /**
     * Processed row (Employee ID, Leave Type, Days, Reason, Approved?) for the
     * Leave Management table. The approval is the last field and the reason is
     * everything between the days and the approval, so a reason that contains a
     * comma still lands in one cell.
     */
    public static Object[] processedTableRow(String[] row) {
        String approval = row.length > COLUMN_APPROVAL ? row[row.length - 1].trim() : "";
        String reason = row.length > COLUMN_APPROVAL
                ? String.join(",", Arrays.copyOfRange(row, COL_REASON, row.length - 1)).trim()
                : reasonOf(row);
        return new Object[] {
            cell(row, COL_EMPLOYEE_ID), cell(row, COL_LEAVE_TYPE), cell(row, COL_DAYS), reason, approval
        };
    }

    /**
     * Refuses to rewrite an employee file whose rows do not all have the header's
     * column count. A field with an unescaped comma would shift every later
     * column, and writing the parsed rows back would turn that into lost data
     * (known issue 13 - the screens still split on every comma).
     */
    private static void ensureUniformColumns(List<String[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        int columns = rows.get(0).length;
        for (int i = 1; i < rows.size(); i++) {
            if (rows.get(i).length != columns) {
                throw new IllegalStateException("data.csv row " + (i + 1) + " has " + rows.get(i).length
                        + " columns but the header has " + columns
                        + ". Fix the file before processing leaves.");
            }
        }
    }

    /** True when the file row still describes the request the table displayed. */
    private static boolean sameRequest(String[] row, String[] expected) {
        if (expected == null) {
            return true;
        }
        String reason = expected.length > COL_REASON ? expected[COL_REASON].trim() : "";
        return cell(row, COL_EMPLOYEE_ID).equals(expected[COL_EMPLOYEE_ID].trim())
                && cell(row, COL_LEAVE_TYPE).equalsIgnoreCase(expected[COL_LEAVE_TYPE].trim())
                && cell(row, COL_DAYS).equals(expected[COL_DAYS].trim())
                && reasonOf(row).equals(reason);
    }

    private static int parseDays(String raw) {
        int days;
        try {
            days = Integer.parseInt(raw.trim());
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException("The number of leave days is not a number: '" + raw + "'.");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("The number of leave days must be at least 1.");
        }
        return days;
    }

    private static int parseNumber(String raw, String field, String employeeId) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException(
                    "Employee " + employeeId + " has an invalid value for " + field + ": '" + raw + "'.");
        }
    }

    private static int indexOfEmployee(List<String[]> rows, String employeeId) {
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length > 0 && row[COL_EMPLOYEE_ID].trim().equals(employeeId)) {
                return i;
            }
        }
        return -1;
    }

    private static String cell(String[] row, int index) {
        return index < row.length ? row[index].trim() : "";
    }

    private static List<String[]> readRows(Path file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(file)) {
            return rows;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty()) {
                rows.add(line.split(",", -1));
            }
        }
        return rows;
    }

    private static byte[] readBytes(Path file) throws IOException {
        return Files.exists(file) ? Files.readAllBytes(file) : null;
    }

    private static void restore(Path file, byte[] snapshot) {
        try {
            if (snapshot == null) {
                Files.deleteIfExists(file);
            } else {
                Files.write(file, snapshot);
            }
        } catch (IOException ignored) {
            // A failed restore must not hide the original failure.
        }
    }

    private void appendRow(Path file, String[] row) throws IOException {
        String line = String.join(",", row) + separatorOf(file);
        Files.write(file, line.getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }

    private void writeRows(Path file, List<String[]> rows) throws IOException {
        String separator = separatorOf(file);
        StringBuilder content = new StringBuilder();
        for (String[] row : rows) {
            content.append(String.join(",", row)).append(separator);
        }
        writeAtomically(file, content.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeAtomically(Path file, byte[] bytes) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path directory = absolute.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }
        Path temporary = Files.createTempFile(directory, absolute.getFileName().toString(), ".tmp");
        Files.write(temporary, bytes);
        try {
            Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException notAtomic) {
            Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Keeps the line ending a file already uses, so a rewrite is a content diff
     * only. A file that has no line ending yet (a fresh leaves.csv) borrows the
     * separator of the other leave files instead of mixing CRLF and LF.
     */
    private String separatorOf(Path file) {
        String detected = detectSeparator(file);
        if (detected != null) {
            return detected;
        }
        for (Path candidate : new Path[] {pendingFile, leavesFile, dataFile}) {
            if (!candidate.equals(file)) {
                String inherited = detectSeparator(candidate);
                if (inherited != null) {
                    return inherited;
                }
            }
        }
        return System.lineSeparator();
    }

    private static String detectSeparator(Path file) {
        try {
            if (Files.exists(file)) {
                String head = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                int newline = head.indexOf('\n');
                if (newline > 0 && head.charAt(newline - 1) == '\r') {
                    return "\r\n";
                }
                if (newline >= 0) {
                    return "\n";
                }
                if (head.indexOf('\r') >= 0) {
                    return "\r\n";
                }
            }
        } catch (IOException ignored) {
            // Unknown: let the caller fall back.
        }
        return null;
    }
}
