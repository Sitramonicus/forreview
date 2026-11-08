import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import motorphemployee.LeaveService;

/**
 * Console evidence for the leave fixes (KI-18 / KI-19).
 *
 * <p>Runs the file logic of {@link LeaveService} against throwaway copies of the
 * three CSV files, so approving a leave never touches the repository data. One
 * line per assertion; the process exits with 1 when anything fails.</p>
 *
 * <pre>
 * java -cp &lt;classes&gt; LeaveServiceTest
 * </pre>
 */
public final class LeaveServiceTest {

    private static int passed;
    private static int failed;
    private static Path work;
    private static int scenario;

    public static void main(String[] args) throws Exception {
        System.out.println("LeaveService console evidence — KI-18 (duplicate leaves) + KI-19 (swapped columns)");
        System.out.println();

        approveWritesCorrectColumnsAndRemovesPending();
        rejectWritesNoAndKeepsCredits();
        theSameRequestCannotBeProcessedTwice();
        unknownEmployeeLeavesTheFilesUntouched();
        employeeWithoutCreditIsRefused();
        aStaleRowIsRefusedAfterTheFileChanged();
        aRaggedEmployeeFileIsRefused();
        reasonWithACommaStaysInTheReasonColumn();
        windowsLineEndingsSurviveTheRewrite();
        aWriteFailureRollsEverythingBack();

        System.out.println();
        System.out.println("Summary: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- scenarios

    private static void approveWritesCorrectColumnsAndRemovesPending() throws IOException {
        start("approve: request leaves pendingleaves.csv, lands in leaves.csv with Yes, credit drops by 1");
        LeaveService service = fixture("23,Sick Leave,2,Sick\r\n13,Emergency Leave,1,Family trip\r\n", "", 5, 0, 0);

        String[] processed = service.decide(0, true);

        check("returned row keeps header order", "23|Sick Leave|2|Sick|Yes", String.join("|", processed));
        check("pending file keeps only the untouched request", "13,Emergency Leave,1,Family trip",
                String.join(" / ", lines(work.resolve("pendingleaves.csv"))));
        check("leaves.csv got one row", 1, lines(work.resolve("leaves.csv")).size());
        check("leaves.csv row has the reason in column 4 and Yes in column 5", "23,Sick Leave,2,Sick,Yes",
                lines(work.resolve("leaves.csv")).get(0));
        List<String> dataRows = lines(work.resolve("data.csv"));
        check("data.csv keeps all 24 columns on the edited row", 24, dataRows.get(1).split(",", -1).length);
        check("leave credits 5 -> 4", "4", creditsOf("23"));
        check("approved leaves 0 -> 1", "1", approvedOf("23"));
        check("rejected leaves unchanged", "0", rejectedOf("23"));
    }

    private static void rejectWritesNoAndKeepsCredits() throws IOException {
        start("reject: same single write, No in the approval column, credit untouched");
        LeaveService service = fixture("13,Emergency Leave,2,Family trip\r\n", "", 5, 0, 0);

        String[] processed = service.decide(0, false);

        check("returned row keeps header order", "13|Emergency Leave|2|Family trip|No", String.join("|", processed));
        check("pending file is empty", 0, lines(work.resolve("pendingleaves.csv")).size());
        check("leaves.csv row has the reason in column 4 and No in column 5", "13,Emergency Leave,2,Family trip,No",
                lines(work.resolve("leaves.csv")).get(0));
        check("leave credits unchanged", "5", creditsOf("13"));
        check("approved leaves unchanged", "0", approvedOf("13"));
        check("rejected leaves 0 -> 1", "1", rejectedOf("13"));
    }

    private static void theSameRequestCannotBeProcessedTwice() throws IOException {
        start("the same request cannot be approved twice (the old duplicate-leave bug)");
        LeaveService service = fixture("23,Sick Leave,2,Sick\r\n", "", 5, 0, 0);

        service.decide(0, true);
        String pendingAfterFirst = String.join("|", lines(work.resolve("pendingleaves.csv")));
        String leavesAfterFirst = String.join("|", lines(work.resolve("leaves.csv")));
        int rowsAfterFirst = lines(work.resolve("leaves.csv")).size();

        boolean refused = false;
        String message = "";
        try {
            service.decide(0, true);
        } catch (IllegalArgumentException expected) {
            refused = true;
            message = expected.getMessage();
        }
        check("second decision on the same index is refused", true, refused);
        check("the refusal explains why", true, message.contains("no longer in the pending list"));
        check("pending file still empty", "", pendingAfterFirst);
        check("leaves.csv still has one row", rowsAfterFirst, lines(work.resolve("leaves.csv")).size());
        check("leaves.csv unchanged", leavesAfterFirst, String.join("|", lines(work.resolve("leaves.csv"))));
        check("leave credit was only deducted once", "4", creditsOf("23"));
    }

    private static void unknownEmployeeLeavesTheFilesUntouched() throws IOException {
        start("unknown employee: refused before any file is written");
        LeaveService service = fixture("99,Sick Leave,2,Sick\r\n", "", 5, 0, 0);
        String pendingBefore = String.join("|", lines(work.resolve("pendingleaves.csv")));
        String dataBefore = String.join("|", lines(work.resolve("data.csv")));

        boolean refused = false;
        try {
            service.decide(0, true);
        } catch (IllegalArgumentException expected) {
            refused = expected.getMessage().contains("not in the employee data file");
        }
        check("refused", true, refused);
        check("pending file untouched", pendingBefore, String.join("|", lines(work.resolve("pendingleaves.csv"))));
        check("data file untouched", dataBefore, String.join("|", lines(work.resolve("data.csv"))));
        check("leaves.csv still empty", 0, lines(work.resolve("leaves.csv")).size());
    }

    private static void employeeWithoutCreditIsRefused() throws IOException {
        start("employee with 0 leave credits: approval refused, nothing written");
        LeaveService service = fixture("23,Sick Leave,2,Sick\r\n", "", 0, 3, 1);
        String pendingBefore = String.join("|", lines(work.resolve("pendingleaves.csv")));

        boolean refused = false;
        try {
            service.decide(0, true);
        } catch (IllegalStateException expected) {
            refused = expected.getMessage().contains("no leave credit left");
        }
        check("refused", true, refused);
        check("pending file untouched", pendingBefore, String.join("|", lines(work.resolve("pendingleaves.csv"))));
        check("data file untouched", "0", creditsOf("23"));
        check("leaves.csv still empty", 0, lines(work.resolve("leaves.csv")).size());
    }

    private static void aStaleRowIsRefusedAfterTheFileChanged() throws IOException {
        start("a row that changed on disk is refused instead of processing the wrong request");
        LeaveService service = fixture("23,Sick Leave,2,Sick\r\n13,Emergency Leave,1,Family trip\r\n", "", 5, 0, 0);
        // The window showed row 0 as employee 23; the file is replaced underneath it.
        Files.write(work.resolve("pendingleaves.csv"),
                "13,Emergency Leave,1,Family trip\r\n".getBytes(StandardCharsets.UTF_8));

        boolean refused = false;
        try {
            service.decide(0, new String[] {"23", "Sick Leave", "2", "Sick"}, true);
        } catch (IllegalArgumentException expected) {
            refused = expected.getMessage().contains("changed while this window was open");
        }
        check("refused", true, refused);
        check("no processed row was written", 0, lines(work.resolve("leaves.csv")).size());
        check("leave credits untouched", "5", creditsOf("23"));
    }

    private static void aRaggedEmployeeFileIsRefused() throws IOException {
        start("data.csv with a shifted row is refused instead of being rewritten");
        LeaveService service = fixture("23,Sick Leave,2,Sick\r\n", "", 5, 0, 0);
        // The shape a comma inside an address produces: one extra column on row 2.
        String shifted = employee("13", "Valencia", 5, 0, 0).replace(",Test,", ",Test Blk 5, Phase 2,");
        Files.write(work.resolve("data.csv"), (header() + "\r\n"
                + employee("23", "Racimo", 5, 0, 0) + "\r\n" + shifted + "\r\n").getBytes(StandardCharsets.UTF_8));

        boolean refused = false;
        try {
            service.decide(0, true);
        } catch (IllegalStateException expected) {
            refused = expected.getMessage().contains("Fix the file before processing leaves");
        }
        check("refused", true, refused);
        check("pending file untouched", 1, lines(work.resolve("pendingleaves.csv")).size());
        check("leaves.csv still empty", 0, lines(work.resolve("leaves.csv")).size());
        check("shifted row kept its extra column (nothing rewritten)", 25,
                lines(work.resolve("data.csv")).get(2).split(",", -1).length);
    }

    private static void reasonWithACommaStaysInTheReasonColumn() throws IOException {
        start("a reason that contains a comma stays in the Reason column");
        LeaveService service = fixture("23,Sick Leave,2,Blk 5, Phase 2\r\n", "", 5, 0, 0);

        String[] processed = service.decide(0, true);

        check("reason rebuilt from the split fields", "Blk 5, Phase 2", processed[LeaveService.COLUMN_REASON]);
        check("approval still last", "Yes", processed[LeaveService.COLUMN_APPROVAL]);
        Object[] pendingCells = LeaveService.pendingTableRow(new String[] {"23", "Sick", "2", "Blk 5", " Phase 2"});
        check("pending table shows the whole reason", "Blk 5, Phase 2", String.valueOf(pendingCells[3]));
    }

    private static void windowsLineEndingsSurviveTheRewrite() throws IOException {
        start("CRLF files keep CRLF after the pending rewrite (no whole-file diff)");
        LeaveService service = fixture("23,Sick Leave,2,Sick\r\n13,Emergency Leave,1,Family trip\r\n", "", 5, 0, 0);

        service.decide(0, true);

        String pending = new String(Files.readAllBytes(work.resolve("pendingleaves.csv")), StandardCharsets.UTF_8);
        check("remaining line still ends with CRLF", true, pending.endsWith("\r\n"));
        check("no bare LF was introduced", false, pending.replace("\r\n", "").contains("\n"));
        String leaves = new String(Files.readAllBytes(work.resolve("leaves.csv")), StandardCharsets.UTF_8);
        check("appended row also uses CRLF", true, leaves.endsWith("\r\n"));
    }

    private static void aWriteFailureRollsEverythingBack() throws IOException {
        start("a write failure restores all three files (no half-processed request)");
        LeaveService service = fixture("23,Sick Leave,2,Sick\r\n", "", 5, 0, 0);
        String pendingBefore = new String(Files.readAllBytes(work.resolve("pendingleaves.csv")), StandardCharsets.UTF_8);
        String dataBefore = new String(Files.readAllBytes(work.resolve("data.csv")), StandardCharsets.UTF_8);
        // A directory in place of leaves.csv makes the append fail after the
        // pending file was already rewritten.
        Path broken = work.resolve("leaves.csv");
        Files.deleteIfExists(broken);
        Files.createDirectory(broken);

        boolean failedLoudly = false;
        try {
            service.decide(0, true);
        } catch (IOException expected) {
            failedLoudly = true;
        }
        check("the failure is reported", true, failedLoudly);
        check("pending file restored", pendingBefore,
                new String(Files.readAllBytes(work.resolve("pendingleaves.csv")), StandardCharsets.UTF_8));
        check("data file restored", dataBefore,
                new String(Files.readAllBytes(work.resolve("data.csv")), StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ fixture

    /**
     * Fresh temp directory with the three files: one employee record (id 23,
     * id 13 as the second pending row's owner) and the given pending content.
     */
    private static LeaveService fixture(String pendingCsv, String leavesCsv,
            int credits, int approvedLeaves, int rejectedLeaves) throws IOException {
        work = Files.createTempDirectory("leave-service-scenario-" + scenario);
        Files.write(work.resolve("pendingleaves.csv"), pendingCsv.getBytes(StandardCharsets.UTF_8));
        Files.write(work.resolve("leaves.csv"), leavesCsv.getBytes(StandardCharsets.UTF_8));
        StringBuilder data = new StringBuilder();
        data.append(header()).append("\r\n");
        data.append(employee("23", "Racimo", credits, approvedLeaves, rejectedLeaves)).append("\r\n");
        data.append(employee("13", "Valencia", 5, 0, 0)).append("\r\n");
        Files.write(work.resolve("data.csv"), data.toString().getBytes(StandardCharsets.UTF_8));
        return new LeaveService(
                work.resolve("data.csv").toString(),
                work.resolve("pendingleaves.csv").toString(),
                work.resolve("leaves.csv").toString());
    }

    private static String header() {
        return "Employee #,Last Name,First Name,Birthday,Address,Phone Number,SSS #,Philhealth #,TIN #,"
                + "Pag-ibig #,Status,Position,Immediate Supervisor,Basic Salary,Rice Subsidy,Phone Allowance,"
                + "Clothing Allowance,Gross Semi-monthly Rate,Hourly Rate,Username,Password,"
                + "Leave Credits,Approved Leaves,Rejected Leaves";
    }

    private static String employee(String id, String lastName, int credits, int approvedLeaves, int rejectedLeaves) {
        String[] columns = new String[24];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = "";
        }
        columns[0] = id;
        columns[1] = lastName;
        columns[2] = "Test";
        columns[13] = "20000";
        columns[14] = "1500";
        columns[15] = "1000";
        columns[16] = "1000";
        columns[17] = "10000";
        columns[18] = "55.56";
        columns[19] = "admin";
        columns[20] = "password";
        columns[21] = String.valueOf(credits);
        columns[22] = String.valueOf(approvedLeaves);
        columns[23] = String.valueOf(rejectedLeaves);
        return String.join(",", columns);
    }

    // ------------------------------------------------------------------ helpers

    private static void start(String name) {
        scenario++;
        System.out.println("[" + scenario + "] " + name);
    }

    private static void check(String label, Object expected, Object actual) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(actual));
        if (ok) {
            passed++;
            System.out.println("    PASS  " + label);
        } else {
            failed++;
            System.out.println("    FAIL  " + label + " (expected <" + expected + ">, got <" + actual + ">)");
        }
    }

    private static List<String> lines(Path file) throws IOException {
        List<String> rows = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty()) {
                rows.add(line);
            }
        }
        return rows;
    }

    private static String creditsOf(String employeeId) throws IOException {
        return columnOf(employeeId, 21);
    }

    private static String approvedOf(String employeeId) throws IOException {
        return columnOf(employeeId, 22);
    }

    private static String rejectedOf(String employeeId) throws IOException {
        return columnOf(employeeId, 23);
    }

    private static String columnOf(String employeeId, int column) throws IOException {
        for (String line : lines(work.resolve("data.csv"))) {
            String[] cells = line.split(",", -1);
            if (cells[0].equals(employeeId)) {
                return cells[column];
            }
        }
        return "employee " + employeeId + " not found";
    }
}
