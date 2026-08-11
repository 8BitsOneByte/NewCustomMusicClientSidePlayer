package org.exmple.newcustommusicclientsideplayer.client.storage;

public final class CTrackNameImportException extends Exception {
    private final Reason reason;

    private CTrackNameImportException(Reason reason, Throwable cause) {
        super(cause);
        this.reason = reason;
    }

    private CTrackNameImportException(Reason reason) {
        this.reason = reason;
    }

    public static CTrackNameImportException ioError(Throwable cause) {
        return new CTrackNameImportException(Reason.IO_ERROR, cause);
    }

    public static CTrackNameImportException invalidTrackNameFile(Throwable cause) {
        return new CTrackNameImportException(Reason.INVALID_TRACK_NAME_FILE, cause);
    }

    public static CTrackNameImportException invalidTrackNameFile() {
        return new CTrackNameImportException(Reason.INVALID_TRACK_NAME_FILE);
    }

    public Reason reason() {
        return this.reason;
    }

    public enum Reason {
        IO_ERROR,
        INVALID_TRACK_NAME_FILE
    }
}
