package org.exmple.newcustommusicclientsideplayer.client.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CPlaylistRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve(NewcustommusicclientsideplayerClient.MOD_ID)
        .resolve("playlists.json");

    private CPlaylistRepository() {
    }

    public static synchronized boolean createPlaylist(String playlistName) throws IOException {
        StoredData data = readData();
        if (data.playlists.containsKey(playlistName)) {
            return false;
        }

        data.playlists.put(playlistName, new StoredPlaylist());
        writeData(data);
        return true;
    }

    public static synchronized boolean deletePlaylist(String playlistName) throws IOException {
        StoredData data = readData();
        if (data.playlists.remove(playlistName) == null) {
            return false;
        }

        writeData(data);
        return true;
    }

    public static synchronized boolean exists(String playlistName) throws IOException {
        return readData().playlists.containsKey(playlistName);
    }

    public static synchronized List<String> listPlaylistNames() throws IOException {
        return new ArrayList<>(readData().playlists.keySet());
    }

    public static synchronized List<PlaylistSummary> listPlaylistSummaries() throws IOException {
        StoredData data = readData();
        return data.playlists.entrySet()
            .stream()
            .map(entry -> new PlaylistSummary(entry.getKey(), entry.getValue().tracks.size(), entry.getValue().modifiedAt))
            .toList();
    }

    public static synchronized boolean movePlaylist(String playlistName, int offset) throws IOException {
        if (offset == 0) {
            return false;
        }

        StoredData data = readData();
        List<Map.Entry<String, StoredPlaylist>> entries = new ArrayList<>(data.playlists.entrySet());
        int currentIndex = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().equals(playlistName)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0) {
            return false;
        }

        int targetIndex = currentIndex + offset;
        if (targetIndex < 0 || targetIndex >= entries.size()) {
            return false;
        }

        Map.Entry<String, StoredPlaylist> moving = entries.remove(currentIndex);
        entries.add(targetIndex, moving);

        LinkedHashMap<String, StoredPlaylist> reordered = new LinkedHashMap<>();
        for (Map.Entry<String, StoredPlaylist> entry : entries) {
            reordered.put(entry.getKey(), entry.getValue());
        }

        data.playlists = reordered;
        writeData(data);
        return true;
    }

    public static synchronized List<Identifier> getPlaylist(String playlistName) throws IOException {
        StoredPlaylist storedPlaylist = readData().playlists.get(playlistName);
        if (storedPlaylist == null) {
            return List.of();
        }

        List<Identifier> identifiers = new ArrayList<>();
        for (String rawId : storedPlaylist.tracks) {
            Identifier id = Identifier.tryParse(rawId);
            if (id != null) {
                identifiers.add(id);
            }
        }

        return identifiers;
    }

    public static synchronized void savePlaylist(String playlistName, List<Identifier> playlist) throws IOException {
        StoredData data = readData();
        StoredPlaylist storedPlaylist = new StoredPlaylist();
        storedPlaylist.tracks = playlist.stream().map(Identifier::toString).collect(Collectors.toCollection(ArrayList::new));
        storedPlaylist.modifiedAt = System.currentTimeMillis();
        data.playlists.put(playlistName, storedPlaylist);
        writeData(data);
    }

    public static synchronized List<ExportPlaylist> exportPlaylists() throws IOException {
        StoredData data = readData();
        List<ExportPlaylist> playlists = new ArrayList<>();
        for (Map.Entry<String, StoredPlaylist> entry : data.playlists.entrySet()) {
            playlists.add(new ExportPlaylist(
                entry.getKey(),
                entry.getValue().tracks,
                entry.getValue().modifiedAt
            ));
        }

        return playlists;
    }

    public static synchronized ImportResult importPlaylists(List<ImportPlaylist> playlists) throws IOException {
        StoredData data = readData();
        for (ImportPlaylist playlist : playlists) {
            StoredPlaylist storedPlaylist = new StoredPlaylist();
            storedPlaylist.tracks = playlist.tracks()
                .stream()
                .map(Identifier::toString)
                .collect(Collectors.toCollection(ArrayList::new));
            storedPlaylist.modifiedAt = playlist.modifiedAt();
            data.playlists.put(playlist.name(), storedPlaylist);
        }

        writeData(data);
        return new ImportResult(playlists.size());
    }

    private static StoredData readData() throws IOException {
        if (!Files.exists(FILE_PATH)) {
            return new StoredData();
        }

        long fallbackModifiedAt = Files.getLastModifiedTime(FILE_PATH).toMillis();
        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!(root instanceof JsonObject rootObject)) {
                return new StoredData();
            }

            JsonElement playlistsElement = rootObject.get("playlists");
            if (!(playlistsElement instanceof JsonObject playlistsObject)) {
                return new StoredData();
            }

            StoredData data = new StoredData();
            for (Map.Entry<String, JsonElement> playlistEntry : playlistsObject.entrySet()) {
                data.playlists.put(playlistEntry.getKey(), parsePlaylist(playlistEntry.getValue(), fallbackModifiedAt));
            }

            return data;
        }
    }

    private static StoredPlaylist parsePlaylist(JsonElement element, long fallbackModifiedAt) {
        StoredPlaylist playlist = new StoredPlaylist();
        if (element instanceof JsonArray legacyArray) {
            for (JsonElement trackElement : legacyArray) {
                if (trackElement instanceof JsonPrimitive primitive && primitive.isString()) {
                    playlist.tracks.add(primitive.getAsString());
                }
            }

            playlist.modifiedAt = fallbackModifiedAt;
            return playlist;
        }

        if (element instanceof JsonObject object) {
            JsonElement tracksElement = object.get("tracks");
            if (tracksElement instanceof JsonArray tracksArray) {
                for (JsonElement trackElement : tracksArray) {
                    if (trackElement instanceof JsonPrimitive primitive && primitive.isString()) {
                        playlist.tracks.add(primitive.getAsString());
                    }
                }
            }

            JsonElement modifiedAtElement = object.get("modifiedAt");
            if (modifiedAtElement instanceof JsonPrimitive primitive && primitive.isNumber()) {
                playlist.modifiedAt = primitive.getAsLong();
            } else {
                playlist.modifiedAt = fallbackModifiedAt;
            }
        }

        return playlist;
    }

    private static void writeData(StoredData data) throws IOException {
        Files.createDirectories(FILE_PATH.getParent());
        try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
            GSON.toJson(data, writer);
        }
    }

    public static final class PlaylistSummary {
        private final String name;
        private final int trackCount;
        private final long modifiedAt;

        public PlaylistSummary(String name, int trackCount, long modifiedAt) {
            this.name = name;
            this.trackCount = trackCount;
            this.modifiedAt = modifiedAt;
        }

        public String name() {
            return this.name;
        }

        public int trackCount() {
            return this.trackCount;
        }

        public long modifiedAt() {
            return this.modifiedAt;
        }
    }

    public record ExportPlaylist(String name, List<String> tracks, long modifiedAt) {
        public ExportPlaylist {
            tracks = List.copyOf(tracks);
        }
    }

    public record ImportPlaylist(String name, List<Identifier> tracks, long modifiedAt) {
        public ImportPlaylist {
            tracks = List.copyOf(tracks);
        }
    }

    public record ImportResult(int importedCount) {
    }

    private static final class StoredData {
        private LinkedHashMap<String, StoredPlaylist> playlists = new LinkedHashMap<>();
    }

    private static final class StoredPlaylist {
        private List<String> tracks = new ArrayList<>();
        private long modifiedAt = System.currentTimeMillis();
    }
}

