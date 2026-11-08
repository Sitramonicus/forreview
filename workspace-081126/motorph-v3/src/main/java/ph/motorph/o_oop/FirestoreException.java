package ph.motorph.o_oop;

/** A safe, user-facing Firestore REST failure with an HTTP status for API mapping. */
public final class FirestoreException extends RuntimeException {
    private final int statusCode;

    public FirestoreException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() { return statusCode; }
}
