package org.exmple.newcustommusicclientsideplayer.client.storage;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CPlaylistRepository {
    private static final CPlaylistStore STORE = new CPlaylistStore(FabricLoader.getInstance()
        .getConfigDir().resolve(NewcustommusicclientsideplayerClient.MOD_ID).resolve("playlists.json"));

    private CPlaylistRepository() {
    }

    static CPlaylistStore store() {
        return STORE;
    }

    public static synchronized CPlaylistStorageStatus storageStatus() {
        return STORE.status(currentSource());
    }

    public static synchronized boolean createPlaylist(String name) throws IOException {
        return createPlaylist(name, currentSource());
    }

    public static synchronized boolean createPlaylist(String name, CTrackSourceType type) throws IOException {
        return STORE.create(name, type);
    }

    public static synchronized boolean deletePlaylist(String name) throws IOException {
        return STORE.delete(name, currentSource());
    }

    public static synchronized boolean exists(String name) throws IOException {
        return STORE.names(currentSource()).contains(name);
    }

    public static synchronized List<String> listPlaylistNames() throws IOException {
        return STORE.names(currentSource());
    }

    public static synchronized List<PlaylistSummary> listPlaylistSummaries() throws IOException {
        return listPlaylistSummaries(currentSource());
    }

    public static synchronized boolean movePlaylist(String name, int offset) throws IOException {
        return STORE.move(name, offset, currentSource());
    }

    public static synchronized Optional<CPlaylistData> getPlaylist(String name) throws IOException {
        return STORE.get(name, currentSource());
    }

    public static synchronized List<Identifier> getSoundEventPlaylist(String name) throws IOException {
        Optional<CPlaylistData> playlist = STORE.get(name, CTrackSourceType.SOUND_EVENT);
        if (playlist.isEmpty()) {
            return List.of();
        }
        if (playlist.get().sourceType() != CTrackSourceType.SOUND_EVENT) {
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.SOURCE_MISMATCH);
        }
        return playlist.get().tracks();
    }

    public static synchronized void savePlaylist(String name, List<Identifier> tracks) throws IOException {
        STORE.save(name, new CPlaylistData(CTrackSourceType.SOUND_EVENT, tracks, System.currentTimeMillis()));
    }

    public static synchronized void savePlaylist(String name, CPlaylistData playlist) throws IOException {
        STORE.save(name, playlist);
    }

    public static synchronized void renameSoundEventPlaylist(String oldName, String newName, List<Identifier> tracks)
        throws IOException {
        STORE.rename(oldName, newName,
            new CPlaylistData(CTrackSourceType.SOUND_EVENT, tracks, System.currentTimeMillis()));
    }

    private static CTrackSourceType currentSource() {
        return NewcustommusicclientsideplayerClient.getModConfig().defaultTrackSource();
    }

    public static synchronized List<String> listPlaylistNames(CTrackSourceType type) throws IOException {
        return STORE.names(type);
    }

    public static synchronized List<PlaylistSummary> listPlaylistSummaries(CTrackSourceType type) throws IOException {
        List<PlaylistSummary> summaries = new ArrayList<>();
        STORE.readablePlaylists(type).forEach((name, data) -> summaries.add(
            new PlaylistSummary(name, data.tracks().size(), data.modifiedAt(), type)));
        return summaries;
    }

    public static synchronized Optional<CPlaylistData> getPlaylist(String name, CTrackSourceType type) throws IOException {
        return STORE.get(name, type);
    }

    public static synchronized boolean deletePlaylist(String name, CTrackSourceType type) throws IOException {
        return STORE.delete(name, type);
    }

    public static synchronized boolean movePlaylist(String name, int offset, CTrackSourceType type) throws IOException {
        return STORE.move(name, offset, type);
    }

    public static synchronized void renamePlaylist(String oldName, String newName, CPlaylistData playlist) throws IOException {
        STORE.rename(oldName, newName, playlist);
    }

    public static synchronized JsonObject exportPlaylists() throws IOException {
        return STORE.exportPlaylists();
    }

    public static synchronized ImportResult importPlaylists(List<ImportPlaylist> playlists) throws IOException {
        STORE.importPlaylists(playlists);
        return new ImportResult(playlists.size());
    }

    public record PlaylistSummary(String name, int trackCount, long modifiedAt, CTrackSourceType sourceType) {
    }

    public record ImportPlaylist(String name, CTrackSourceType sourceType, List<Identifier> tracks, long modifiedAt) {
        public ImportPlaylist {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(sourceType, "sourceType");
            tracks = List.copyOf(tracks);
        }
    }

    public record ImportResult(int importedCount) {
    }
}
