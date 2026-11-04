package motorphpayrollsystem;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CSVUtil {

    /** Read all rows from a CSV file. Returns an empty list if the file is missing. */
    public static List<String[]> read(String filePath) throws IOException {
        File f = new File(filePath);
        if (!f.exists()) return new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(f))) {
            return reader.readAll();
        } catch (CsvException e) {
            throw new IOException("CSV parse error: " + e.getMessage(), e);
        }
    }

    /** Overwrite a CSV file with the supplied rows. */
    public static void write(String filePath, List<String[]> rows) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            writer.writeAll(rows);
        }
    }

    /** Append a single row to a CSV file (creates the file if absent). */
    public static void append(String filePath, String[] row) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath, true))) {
            writer.writeNext(row);
        }
    }

    /**
     * Ensure a CSV file exists with the given header line.
     * Creates the file and writes the header only when the file is missing or empty.
     */
    public static void ensureHeader(String filePath, String headerLine) throws IOException {
        File f = new File(filePath);
        if (!f.exists() || f.length() == 0) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println(headerLine);
            }
        }
    }
}
