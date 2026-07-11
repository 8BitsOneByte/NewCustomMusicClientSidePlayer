package org.exmple.newcustommusicclientsideplayer.client.storage;

/**
 * Classifies hard playlist import failures so the GUI can choose the right user-facing screen.
 * Partially compatible files are represented by CPlaylistImportPreview, not this exception.
 */
public final class CPlaylistImportException extends Exception {
    private final Reason reason;

    private CPlaylistImportException(Reason reason, Throwable cause) {
        super(cause);
        this.reason = reason;
    }

    private CPlaylistImportException(Reason reason) {
        this.reason = reason;
    }

    public static CPlaylistImportException ioError(Throwable cause) {
        return new CPlaylistImportException(Reason.IO_ERROR, cause);
    }

    public static CPlaylistImportException invalidPlaylistFile(Throwable cause) {
        return new CPlaylistImportException(Reason.INVALID_PLAYLIST_FILE, cause);
    }

    public static CPlaylistImportException invalidPlaylistFile() {
        return new CPlaylistImportException(Reason.INVALID_PLAYLIST_FILE);
    }

    public static CPlaylistImportException noCompatiblePlaylists() {
        return new CPlaylistImportException(Reason.NO_COMPATIBLE_PLAYLISTS);
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
         * The file could be read but is not a valid playlist export for this mod, including
         * malformed JSON, a non-object root, a missing or mismatched mod id, or a missing/non-object
         * playlists node.
         */
        INVALID_PLAYLIST_FILE,

        /**
         * The file is a valid playlist export, but none of its playlists can be imported by this
         * version.
         */
        NO_COMPATIBLE_PLAYLISTS
    }
}
