package org.exmple.newcustommusicclientsideplayer.client.storage;

import java.io.IOException;

public final class CPlaylistStorageException extends IOException {
    private final Reason reason;

    public CPlaylistStorageException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return this.reason;
    }

    public enum Reason {
        UNSUPPORTED_VERSION, INVALID_DATA, UNREADABLE_PLAYLIST, SOURCE_MISMATCH,
        ARCHIVE_CONFLICT, STORAGE_CHANGED, NAME_CONFLICT
    }
}
