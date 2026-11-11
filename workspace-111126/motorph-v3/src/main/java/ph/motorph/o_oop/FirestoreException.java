package ph.motorph.o_oop;

public final class FirestoreException extends RuntimeException {
    private final int statusCode;

    public FirestoreException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() { return statusCode; }
}
