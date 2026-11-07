package org.exmple.newcustommusicclientsideplayer.client.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CPlaylistTransferData {
    public static final int CURRENT_FORMAT_VERSION = CPlaylistFormat.CURRENT_VERSION;

    private int formatVersion = CURRENT_FORMAT_VERSION;
    private String modId = NewcustommusicclientsideplayerClient.MOD_ID;
    private String modVersion = "";
    private String minecraftVersion = "";
    private long exportedAt;
    private LinkedHashMap<String, Map<String, Playlist>> playlists = new LinkedHashMap<>();

    CPlaylistTransferData() {
    }

    public CPlaylistTransferData(
        String modVersion,
        String minecraftVersion,
        long exportedAt,
        Map<String, Map<String, Playlist>> playlists
    ) {
        this.modVersion = Objects.requireNonNull(modVersion, "modVersion");
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        this.exportedAt = exportedAt;
        this.playlists = new LinkedHashMap<>(Objects.requireNonNull(playlists, "playlists"));
    }

    public int formatVersion() {
        return this.formatVersion;
    }

    public String modId() {
        return this.modId;
    }

    public String modVersion() {
        return this.modVersion;
    }

    public String minecraftVersion() {
        return this.minecraftVersion;
    }

    public long exportedAt() {
        return this.exportedAt;
    }

    public Map<String, Map<String, Playlist>> playlists() {
        return new LinkedHashMap<>(this.playlists);
    }

    public static final class Playlist {
        private CTrackSourceType sourceType;
        private List<String> tracks = new ArrayList<>();
        private long modifiedAt;

        Playlist() {
        }

        public Playlist(CTrackSourceType sourceType, List<String> tracks, long modifiedAt) {
            this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
            this.tracks = new ArrayList<>(Objects.requireNonNull(tracks, "tracks"));
            this.modifiedAt = modifiedAt;
        }

        public CTrackSourceType sourceType() {
            return this.sourceType;
        }

        public List<String> tracks() {
            return List.copyOf(this.tracks);
        }

        public long modifiedAt() {
            return this.modifiedAt;
        }
    }
}
