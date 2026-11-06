package ph.motorph.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.function.Function;

/**
 * The local demo's data file (default data/local-demo.json): the whole in-memory state as one JSON document,
 * written after every change through a temporary file that replaces the old one, so a crash never leaves half a file.
 * A file that cannot be read is moved aside (never overwritten) and the demo starts from its built-in data.
 * Synthetic data only; government numbers in it are the fake ones typed into the demo.
 */
public final class LocalStateFile {
    public static final int FORMAT_VERSION = 1;
    /** Backups (data/backups): an automatic copy at most every 30 minutes of changes, plus "Back up now"; the newest 20 are kept. */
    static final int KEEP_BACKUPS = 20;
    static final long AUTO_BACKUP_MINUTES = 30;
    private static final java.time.format.DateTimeFormatter STAMP = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
        .withZone(java.time.ZoneId.of("Asia/Manila"));
    private final Path path;
    private Instant lastBackup;
    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, text(LocalDate::parse))
        .registerTypeAdapter(LocalTime.class, text(LocalTime::parse))
        .registerTypeAdapter(LocalDateTime.class, text(LocalDateTime::parse))
        .registerTypeAdapter(YearMonth.class, text(YearMonth::parse))
        .disableHtmlEscaping()
        .create();

    public LocalStateFile(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public Path path() { return path; }

    public Gson gson() { return gson; }

    private static <T> TypeAdapter<T> text(Function<String, T> parse) {
        return new TypeAdapter<T>() {
            @Override public void write(JsonWriter out, T value) throws IOException {
                if (value == null) out.nullValue();
                else out.value(value.toString());
            }
            @Override public T read(JsonReader in) throws IOException {
                if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                return parse.apply(in.nextString());
            }
        }.nullSafe();
    }

    /** Writes {"format":1,"savedAt":…,"stores":…} atomically. */
    public synchronized void save(JsonObject stores) throws IOException {
        JsonObject document = new JsonObject();
        document.addProperty("format", FORMAT_VERSION);
        document.addProperty("savedAt", Instant.now().toString());
        document.add("stores", stores);
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.write(temp, gson.toJson(document).getBytes(StandardCharsets.UTF_8));
        replaceWithRetry(temp);
        if (lastBackup == null) lastBackup = newestBackupTime();
        if (lastBackup == null || lastBackup.isBefore(Instant.now().minusSeconds(AUTO_BACKUP_MINUTES * 60))) backupNow("auto");
    }

    /** On Windows a reader (a virus scanner, an editor) can hold the file for a moment; try the replace a few times. */
    private void replaceWithRetry(Path temp) throws IOException {
        for (int attempt = 1; ; attempt++) {
            try {
                try {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (java.nio.file.FileSystemException busy) { // includes AccessDeniedException
                if (attempt >= 5) throw busy;
                try {
                    Thread.sleep(40L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw busy;
                }
            }
        }
    }

    public Path backupDirectory() { return path.resolveSibling("backups"); }

    /** Copies the current data file into data/backups and keeps the newest KEEP_BACKUPS; returns the backup's name. */
    public synchronized String backupNow(String reason) throws IOException {
        if (!Files.exists(path)) throw new IOException("There is no data file to back up yet.");
        Files.createDirectories(backupDirectory());
        String name = "local-demo-" + STAMP.format(Instant.now()) + "-" + reason + ".json";
        Files.copy(path, backupDirectory().resolve(name), StandardCopyOption.REPLACE_EXISTING);
        lastBackup = Instant.now();
        java.util.List<String> names = listBackups();
        for (String old : names.subList(Math.min(KEEP_BACKUPS, names.size()), names.size())) Files.deleteIfExists(backupDirectory().resolve(old));
        return name;
    }

    /** Backup file names, newest first. */
    public synchronized java.util.List<String> listBackups() throws IOException {
        if (!Files.isDirectory(backupDirectory())) return new java.util.ArrayList<>();
        try (java.util.stream.Stream<Path> files = Files.list(backupDirectory())) {
            return files.map(file -> file.getFileName().toString()).filter(LocalStateFile::isBackupName)
                .sorted(java.util.Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList());
        }
    }

    static boolean isBackupName(String name) { return name != null && name.matches("local-demo-\\d{8}-\\d{6}-\\d{3}-[a-z-]{1,20}\\.json"); }

    /** The stores saved in one backup (name checked, so it cannot point outside data/backups). */
    public synchronized JsonObject loadBackup(String name) throws IOException {
        if (!isBackupName(name) || !Files.exists(backupDirectory().resolve(name))) throw new java.util.NoSuchElementException("Backup not found.");
        return readStores(backupDirectory().resolve(name));
    }

    private Instant newestBackupTime() {
        try {
            java.util.List<String> names = listBackups();
            return names.isEmpty() ? null : Files.getLastModifiedTime(backupDirectory().resolve(names.get(0))).toInstant();
        } catch (IOException ex) {
            return null;
        }
    }

    /** Writes an archive file (e.g. old audit-log entries) next to the data file, in data/archive. Returns its name. */
    public synchronized String writeArchive(String prefix, JsonElement content) throws IOException {
        Path folder = path.resolveSibling("archive");
        Files.createDirectories(folder);
        String name = prefix + "-" + STAMP.format(Instant.now()) + ".json";
        Files.write(folder.resolve(name), gson.toJson(content).getBytes(StandardCharsets.UTF_8));
        return name;
    }

    /** The saved stores, or empty when there is no file yet. Throws when the file exists but cannot be used. */
    public synchronized Optional<JsonObject> load() throws IOException {
        if (!Files.exists(path)) return Optional.empty();
        return Optional.of(readStores(path));
    }

    private JsonObject readStores(Path file) throws IOException {
        JsonElement parsed = JsonParser.parseString(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        if (!parsed.isJsonObject() || !parsed.getAsJsonObject().has("stores")
                || parsed.getAsJsonObject().get("format").getAsInt() != FORMAT_VERSION) {
            throw new IOException("Unknown data file format.");
        }
        return parsed.getAsJsonObject().getAsJsonObject("stores");
    }

    /** Keeps an unreadable file for inspection instead of overwriting it; returns where it went. */
    public synchronized Path setAside() throws IOException {
        Path aside = path.resolveSibling(path.getFileName() + ".unreadable-" + Instant.now().toEpochMilli());
        Files.move(path, aside, StandardCopyOption.REPLACE_EXISTING);
        return aside;
    }
}
