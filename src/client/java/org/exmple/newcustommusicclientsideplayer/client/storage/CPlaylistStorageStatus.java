package org.exmple.newcustommusicclientsideplayer.client.storage;

import java.util.List;

public record CPlaylistStorageStatus(State state, List<String> unreadablePlaylists) {
    public CPlaylistStorageStatus {
        unreadablePlaylists = List.copyOf(unreadablePlaylists);
    }

    public boolean restricted() {
        return switch (state) {
            case READY, MISSING, PARTIAL -> false;
            default -> true;
        };
    }

    public enum State {
        READY, MISSING, PARTIAL, UNSUPPORTED_VERSION, INVALID_DATA, IO_ERROR
    }
}
