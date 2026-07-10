package org.exmple.newcustommusicclientsideplayer.client.config;

/**
 * Classifies hard import failures so the GUI can choose the right user-facing screen.
 * Partially compatible files are represented by {@link CConfigImportPreview}, not this exception.
 */
public final class CConfigImportException extends Exception {
    private final Reason reason;

    private CConfigImportException(Reason reason, Throwable cause) {
        super(cause);
        this.reason = reason;
    }

    private CConfigImportException(Reason reason) {
        this.reason = reason;
    }

    public static CConfigImportException ioError(Throwable cause) {
        return new CConfigImportException(Reason.IO_ERROR, cause);
    }

    public static CConfigImportException invalidConfigFile(Throwable cause) {
        return new CConfigImportException(Reason.INVALID_CONFIG_FILE, cause);
    }

    public static CConfigImportException invalidConfigFile() {
        return new CConfigImportException(Reason.INVALID_CONFIG_FILE);
    }

    public static CConfigImportException noCompatibleSettings() {
        return new CConfigImportException(Reason.NO_COMPATIBLE_SETTINGS);
    }

    public Reason reason() {
        return this.reason;
    }

    public enum Reason {
        /**
         * The selected file could not be read, including missing files, permission failures,
         * failed reads, or empty files.
         */
        IO_ERROR,

        /**
         * The file could be read but is not a valid config export for this mod, including malformed
         * JSON, a non-object root, a missing or mismatched mod id, or a missing/non-object config node.
         */
        INVALID_CONFIG_FILE,

        /**
         * The file is a valid config export, but none of its fields can be imported by this version.
         */
        NO_COMPATIBLE_SETTINGS
    }
}
