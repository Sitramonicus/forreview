package ph.motorph.o_oop;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Employee photos (the employee record's picture). PNG or JPEG only, checked by the file's own first bytes rather
 * than its name, at most 150 KB; kept in the data file as Base64. HR and the System Admin upload or remove them.
 * shortcut: Base64 in the one data file keeps this simple for a demo; a real system stores images as files or in object storage.
 */
public final class EmployeePhotos {
    static final int MAX_BYTES = 150 * 1024;

    public static final class Photo {
        public final String type, base64, updatedBy, updatedAt;

        Photo(String type, String base64, String updatedBy, String updatedAt) {
            this.type = type;
            this.base64 = base64;
            this.updatedBy = updatedBy;
            this.updatedAt = updatedAt;
        }

        public byte[] bytes() { return Base64.getDecoder().decode(base64); }
    }

    private final Map<String, Photo> photos = new HashMap<>();

    /** "image/png" or "image/jpeg" from the bytes themselves, or empty if it is neither. */
    static Optional<String> imageType(byte[] bytes) {
        if (bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') return Optional.of("image/png");
        if (bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) return Optional.of("image/jpeg");
        return Optional.empty();
    }

    /** From a data URL ("data:image/png;base64,...") or plain Base64. */
    public synchronized Photo set(String employeeId, String data, String by, String at) {
        String text = data == null ? "" : data.trim();
        int comma = text.indexOf(',');
        if (text.startsWith("data:") && comma > 0) text = text.substring(comma + 1);
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("The photo could not be read.");
        }
        if (bytes.length == 0) throw new IllegalArgumentException("Choose a photo.");
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("The photo must be at most 150 KB.");
        String type = imageType(bytes).orElseThrow(() -> new IllegalArgumentException("The photo must be a PNG or JPEG image."));
        Photo photo = new Photo(type, Base64.getEncoder().encodeToString(bytes), by, at);
        photos.put(employeeId, photo);
        return photo;
    }

    public synchronized Optional<Photo> find(String employeeId) { return Optional.ofNullable(photos.get(employeeId)); }

    public synchronized boolean remove(String employeeId) { return photos.remove(employeeId) != null; }
}
