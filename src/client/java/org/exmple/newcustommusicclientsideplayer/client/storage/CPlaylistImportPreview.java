package org.exmple.newcustommusicclientsideplayer.client.storage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public final class CPlaylistImportPreview {
    private final List<PlaylistToImport> playlists;
    private final List<String> skippedPlaylistEntries;
    private final List<String> unreadableTrackListPlaylists;
    private final LinkedHashMap<String, List<String>> invalidTracksByPlaylist;
    private final List<String> invalidModifiedAtPlaylists;
    private final List<Rename> renamedPlaylists;

    public CPlaylistImportPreview(
        List<PlaylistToImport> playlists,
        List<String> skippedPlaylistEntries,
        List<String> unreadableTrackListPlaylists,
        Map<String, List<String>> invalidTracksByPlaylist,
        List<String> invalidModifiedAtPlaylists,
        List<Rename> renamedPlaylists
    ) {
        this.playlists = copyList(playlists, "playlists");
        this.skippedPlaylistEntries = copyList(skippedPlaylistEntries, "skippedPlaylistEntries");
        this.unreadableTrackListPlaylists = copyList(
            unreadableTrackListPlaylists,
            "unreadableTrackListPlaylists"
        );
        this.invalidTracksByPlaylist = copyInvalidTracks(invalidTracksByPlaylist);
        this.invalidModifiedAtPlaylists = copyList(invalidModifiedAtPlaylists, "invalidModifiedAtPlaylists");
        this.renamedPlaylists = copyList(renamedPlaylists, "renamedPlaylists");
    }

    public List<PlaylistToImport> playlists() {
        return List.copyOf(this.playlists);
    }

    public List<String> skippedPlaylistEntries() {
        return List.copyOf(this.skippedPlaylistEntries);
    }

    public int skippedPlaylistCount() {
        return this.skippedPlaylistEntries.size();
    }

    public List<String> unreadableTrackListPlaylists() {
        return List.copyOf(this.unreadableTrackListPlaylists);
    }

    public Map<String, List<String>> invalidTracksByPlaylist() {
        return copyInvalidTracks(this.invalidTracksByPlaylist);
    }

    public List<String> invalidModifiedAtPlaylists() {
        return List.copyOf(this.invalidModifiedAtPlaylists);
    }

    public List<Rename> renamedPlaylists() {
        return List.copyOf(this.renamedPlaylists);
    }

    public boolean hasWarnings() {
        return !this.skippedPlaylistEntries.isEmpty()
            || !this.unreadableTrackListPlaylists.isEmpty()
            || !this.invalidTracksByPlaylist.isEmpty()
            || !this.invalidModifiedAtPlaylists.isEmpty()
            || !this.renamedPlaylists.isEmpty();
    }

    private static <T> List<T> copyList(List<T> list, String name) {
        return List.copyOf(Objects.requireNonNull(list, name));
    }

    private static LinkedHashMap<String, List<String>> copyInvalidTracks(Map<String, List<String>> source) {
        Objects.requireNonNull(source, "invalidTracksByPlaylist");

        LinkedHashMap<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            copy.put(
                Objects.requireNonNull(entry.getKey(), "playlistName"),
                List.copyOf(Objects.requireNonNull(entry.getValue(), "invalidTracks"))
            );
        }

        return copy;
    }

    public static final class PlaylistToImport {
        private final String name;
        private final List<Identifier> tracks;
        private final long modifiedAt;

        public PlaylistToImport(String name, List<Identifier> tracks, long modifiedAt) {
            this.name = Objects.requireNonNull(name, "name");
            this.tracks = List.copyOf(Objects.requireNonNull(tracks, "tracks"));
            this.modifiedAt = modifiedAt;
        }

        public String name() {
            return this.name;
        }

        public List<Identifier> tracks() {
            return List.copyOf(this.tracks);
        }

        public long modifiedAt() {
            return this.modifiedAt;
        }
    }

    public static final class Rename {
        private final String originalName;
        private final String importedName;

        public Rename(String originalName, String importedName) {
            this.originalName = Objects.requireNonNull(originalName, "originalName");
            this.importedName = Objects.requireNonNull(importedName, "importedName");
        }

        public String originalName() {
            return this.originalName;
        }

        public String importedName() {
            return this.importedName;
        }
    }
}
