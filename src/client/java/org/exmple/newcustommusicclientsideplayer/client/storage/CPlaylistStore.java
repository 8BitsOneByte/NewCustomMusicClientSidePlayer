package org.exmple.newcustommusicclientsideplayer.client.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

final class CPlaylistStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private CPlaylistStorageStatus status = new CPlaylistStorageStatus(CPlaylistStorageStatus.State.MISSING, List.of());

    CPlaylistStore(Path path) {
        this.path = path;
    }

    synchronized CPlaylistStorageStatus status() {
        try {
            read();
        } catch (IOException ignored) {
        }
        return this.status;
    }

    synchronized List<String> names() throws IOException {
        return new ArrayList<>(read().rawPlaylists().keySet());
    }

    synchronized Map<String, CPlaylistData> readablePlaylists() throws IOException {
        return new LinkedHashMap<>(read().playlists());
    }

    synchronized JsonObject exportPlaylists() throws IOException {
        return read().rawPlaylists().deepCopy();
    }

    synchronized Optional<CPlaylistData> get(String name) throws IOException {
        Document document = read();
        requireReadable(document, name);
        return Optional.ofNullable(document.playlists().get(name));
    }

    synchronized boolean create(String name, CTrackSourceType type) throws IOException {
        Document document = read();
        if (document.rawPlaylists().has(name)) {
            return false;
        }
        put(document, name, new CPlaylistData(type, List.of(), System.currentTimeMillis()));
        publish(document, false);
        return true;
    }

    synchronized void save(String name, CPlaylistData playlist) throws IOException {
        Document document = read();
        requireReadable(document, name);
        CPlaylistData existing = document.playlists().get(name);
        if (existing != null && existing.sourceType() != playlist.sourceType()) {
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.SOURCE_MISMATCH);
        }
        put(document, name, playlist);
        publish(document, false);
    }

    synchronized boolean delete(String name) throws IOException {
        Document document = read();
        requireReadable(document, name);
        if (document.rawPlaylists().remove(name) == null) {
            return false;
        }
        publish(document, false);
        return true;
    }

    synchronized void rename(String oldName, String newName, CPlaylistData playlist) throws IOException {
        Document document = read();
        requireReadable(document, oldName);
        CPlaylistData existing = document.playlists().get(oldName);
        if (existing == null || existing.sourceType() != playlist.sourceType()) {
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.SOURCE_MISMATCH);
        }
        if (!oldName.equals(newName) && document.rawPlaylists().has(newName)) {
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.NAME_CONFLICT);
        }
        put(document, oldName, playlist);
        if (!oldName.equals(newName)) {
            JsonObject renamed = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : document.rawPlaylists().entrySet()) {
                renamed.add(entry.getKey().equals(oldName) ? newName : entry.getKey(), entry.getValue());
            }
            document.root().add("playlists", renamed);
        }
        publish(document, false);
    }

    synchronized boolean move(String name, int offset) throws IOException {
        Document document = read();
        requireReadable(document, name);
        List<String> names = new ArrayList<>(document.rawPlaylists().keySet());
        int current = names.indexOf(name);
        long target = (long) current + offset;
        if (current < 0 || offset == 0 || target < 0 || target >= names.size()) {
            return false;
        }
        names.remove(current);
        names.add((int) target, name);
        JsonObject reordered = new JsonObject();
        for (String entry : names) {
            reordered.add(entry, document.rawPlaylists().get(entry));
        }
        document.root().add("playlists", reordered);
        publish(document, false);
        return true;
    }

    synchronized void importPlaylists(List<CPlaylistRepository.ImportPlaylist> playlists) throws IOException {
        Document document = read();
        for (CPlaylistRepository.ImportPlaylist playlist : playlists) {
            if (document.rawPlaylists().has(playlist.name())) {
                throw new CPlaylistStorageException(CPlaylistStorageException.Reason.NAME_CONFLICT);
            }
            put(document, playlist.name(), new CPlaylistData(playlist.sourceType(), playlist.tracks(), playlist.modifiedAt()));
        }
        if (!playlists.isEmpty()) {
            publish(document, false);
        }
    }

    private Document read() throws IOException {
        try {
            return readDocument();
        } catch (CPlaylistStorageException exception) {
            CPlaylistStorageStatus.State state = exception.reason() == CPlaylistStorageException.Reason.UNSUPPORTED_VERSION
                ? CPlaylistStorageStatus.State.UNSUPPORTED_VERSION : CPlaylistStorageStatus.State.INVALID_DATA;
            this.status = new CPlaylistStorageStatus(state, List.of());
            throw exception;
        } catch (IOException exception) {
            this.status = new CPlaylistStorageStatus(CPlaylistStorageStatus.State.IO_ERROR, List.of());
            throw exception;
        } catch (RuntimeException exception) {
            this.status = new CPlaylistStorageStatus(CPlaylistStorageStatus.State.INVALID_DATA, List.of());
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.INVALID_DATA);
        }
    }

    private Document readDocument() throws IOException {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(this.path, BasicFileAttributes.class);
        } catch (NoSuchFileException exception) {
            JsonObject root = new JsonObject();
            root.addProperty("formatVersion", CPlaylistFormat.CURRENT_VERSION);
            root.add("playlists", new JsonObject());
            this.status = new CPlaylistStorageStatus(CPlaylistStorageStatus.State.MISSING, List.of());
            return new Document(root, new LinkedHashMap<>(), null);
        }
        if (!attributes.isRegularFile()) {
            throw new IOException("Unreadable storage");
        }
        byte[] original = Files.readAllBytes(this.path);
        JsonElement parsed;
        String json = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(original)).toString();
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            parsed = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new CPlaylistStorageException(CPlaylistStorageException.Reason.INVALID_DATA);
            }
        }
        if (!(parsed instanceof JsonObject root)) {
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.INVALID_DATA);
        }
        int version = CPlaylistFormat.version(root.get("formatVersion"));
        if (!(root.get("playlists") instanceof JsonObject rawPlaylists)) {
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.INVALID_DATA);
        }
        LinkedHashMap<String, CPlaylistData> playlists = new LinkedHashMap<>();
        List<String> unreadable = new ArrayList<>();
        long fallbackTime = Math.max(0L, attributes.lastModifiedTime().toMillis());
        for (Map.Entry<String, JsonElement> entry : rawPlaylists.entrySet()) {
            CPlaylistData playlist = parse(entry.getValue(), version, fallbackTime);
            if (playlist == null) {
                unreadable.add(entry.getKey());
            } else {
                playlists.put(entry.getKey(), playlist);
            }
        }
        Document document = new Document(root, playlists, original);
        if (version == 1) {
            for (Map.Entry<String, CPlaylistData> entry : playlists.entrySet()) {
                JsonElement old = rawPlaylists.get(entry.getKey());
                JsonObject migrated;
                if (old instanceof JsonObject object) {
                    migrated = object;
                } else {
                    migrated = new JsonObject();
                    migrated.add("tracks", old);
                }
                migrated.addProperty("sourceType", CTrackSourceType.SOUND_EVENT.name());
                migrated.addProperty("modifiedAt", entry.getValue().modifiedAt());
                rawPlaylists.add(entry.getKey(), migrated);
            }
            root.addProperty("formatVersion", CPlaylistFormat.CURRENT_VERSION);
            publish(document, true);
        }
        this.status = new CPlaylistStorageStatus(unreadable.isEmpty()
            ? CPlaylistStorageStatus.State.READY : CPlaylistStorageStatus.State.PARTIAL, unreadable);
        return version == 1 ? readDocument() : document;
    }

    private static CPlaylistData parse(JsonElement element, int version, long fallbackTime) {
        CTrackSourceType type = CPlaylistFormat.sourceType(element, version);
        if (type == null) {
            return null;
        }
        JsonArray tracks;
        long modifiedAt = fallbackTime;
        if (version == 1 && element instanceof JsonArray array) {
            tracks = array;
        } else if (element instanceof JsonObject object && object.get("tracks") instanceof JsonArray array) {
            tracks = array;
            if (object.get("modifiedAt") instanceof JsonPrimitive value && value.isNumber()) {
                try {
                    long timestamp = value.getAsBigDecimal().longValueExact();
                    if (timestamp >= 0) {
                        modifiedAt = timestamp;
                    }
                } catch (ArithmeticException | NumberFormatException ignored) {
                }
            }
        } else {
            return null;
        }
        List<Identifier> identifiers = new ArrayList<>();
        for (JsonElement track : tracks) {
            if (track instanceof JsonPrimitive value && value.isString()) {
                Identifier id = Identifier.tryParse(value.getAsString());
                if (type.accepts(id)) {
                    identifiers.add(id);
                }
            }
        }
        return new CPlaylistData(type, identifiers, modifiedAt);
    }

    private static void requireReadable(Document document, String name) throws CPlaylistStorageException {
        if (document.rawPlaylists().has(name) && !document.playlists().containsKey(name)) {
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.UNREADABLE_PLAYLIST);
        }
    }

    private static void put(Document document, String name, CPlaylistData playlist) {
        JsonElement existing = document.rawPlaylists().get(name);
        JsonObject object = existing instanceof JsonObject old ? old.deepCopy() : new JsonObject();
        object.addProperty("sourceType", playlist.sourceType().name());
        JsonArray tracks = new JsonArray();
        playlist.tracks().forEach(id -> tracks.add(id.toString()));
        object.add("tracks", tracks);
        object.addProperty("modifiedAt", playlist.modifiedAt());
        document.rawPlaylists().add(name, object);
    }

    private void publish(Document document, boolean migration) throws IOException {
        Files.createDirectories(this.path.getParent());
        Path temporary = Files.createTempFile(this.path.getParent(), "playlists-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(document.root()), StandardCharsets.UTF_8);
            verifyUnchanged(document.original());
            if (migration) {
                archive(document.original());
                verifyUnchanged(document.original());
            }
            Files.move(temporary, this.path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void verifyUnchanged(byte[] original) throws IOException {
        byte[] current;
        try {
            current = Files.readAllBytes(this.path);
        } catch (NoSuchFileException exception) {
            current = null;
        }
        if (!Arrays.equals(original, current)) {
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.STORAGE_CHANGED);
        }
    }

    private void archive(byte[] original) throws IOException {
        Path archive = this.path.resolveSibling(this.path.getFileName() + ".migrated");
        try {
            byte[] existing = Files.readAllBytes(archive);
            if (!Arrays.equals(original, existing)) {
                throw new CPlaylistStorageException(CPlaylistStorageException.Reason.ARCHIVE_CONFLICT);
            }
            return;
        } catch (NoSuchFileException ignored) {
        }
        Files.createFile(archive);
        try {
            Files.write(archive, original, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            Files.deleteIfExists(archive);
            throw exception;
        }
    }

    private record Document(JsonObject root, LinkedHashMap<String, CPlaylistData> playlists, byte[] original) {
        JsonObject rawPlaylists() {
            return this.root.getAsJsonObject("playlists");
        }
    }
}
