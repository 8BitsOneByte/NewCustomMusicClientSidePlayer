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

    synchronized CPlaylistStorageStatus status(CTrackSourceType type) {
        try {
            read(type);
        } catch (IOException ignored) {
        }
        return this.status;
    }

    synchronized List<String> names(CTrackSourceType type) throws IOException {
        return new ArrayList<>(read(type).rawPlaylists().keySet());
    }

    synchronized Map<String, CPlaylistData> readablePlaylists(CTrackSourceType type) throws IOException {
        return new LinkedHashMap<>(read(type).playlists());
    }

    synchronized JsonObject exportPlaylists() throws IOException {
        return read(CTrackSourceType.SOUND_EVENT).root().getAsJsonObject("playlists").deepCopy();
    }

    synchronized Optional<CPlaylistData> get(String name, CTrackSourceType type) throws IOException {
        Document document = read(type);
        requireReadable(document, name);
        return Optional.ofNullable(document.playlists().get(name));
    }

    synchronized boolean create(String name, CTrackSourceType type) throws IOException {
        Document document = read(type);
        if (document.rawPlaylists().has(name)) {
            return false;
        }
        put(document, name, new CPlaylistData(type, List.of(), System.currentTimeMillis()));
        publish(document, false);
        return true;
    }

    synchronized void save(String name, CPlaylistData playlist) throws IOException {
        Document document = read(playlist.sourceType());
        requireReadable(document, name);
        CPlaylistData existing = document.playlists().get(name);
        if (existing != null && existing.sourceType() != playlist.sourceType()) {
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.SOURCE_MISMATCH);
        }
        put(document, name, playlist);
        publish(document, false);
    }

    synchronized boolean delete(String name, CTrackSourceType type) throws IOException {
        Document document = read(type);
        requireReadable(document, name);
        if (document.rawPlaylists().remove(name) == null) {
            return false;
        }
        publish(document, false);
        return true;
    }

    synchronized void rename(String oldName, String newName, CPlaylistData playlist) throws IOException {
        Document document = read(playlist.sourceType());
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
            document.root().getAsJsonObject("playlists").add(playlist.sourceType().name(), renamed);
        }
        publish(document, false);
    }

    synchronized boolean move(String name, int offset, CTrackSourceType type) throws IOException {
        Document document = read(type);
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
        document.root().getAsJsonObject("playlists").add(type.name(), reordered);
        publish(document, false);
        return true;
    }

    synchronized void importPlaylists(List<CPlaylistRepository.ImportPlaylist> playlists) throws IOException {
        Document document = read(CTrackSourceType.SOUND_EVENT);
        for (CPlaylistRepository.ImportPlaylist playlist : playlists) {
            Document group = view(document, playlist.sourceType());
            if (group.rawPlaylists().has(playlist.name())) {
                throw new CPlaylistStorageException(CPlaylistStorageException.Reason.NAME_CONFLICT);
            }
            put(group, playlist.name(), new CPlaylistData(playlist.sourceType(), playlist.tracks(), playlist.modifiedAt()));
        }
        if (!playlists.isEmpty()) publish(document, false);
    }

    private static Document view(Document document, CTrackSourceType type) throws IOException {
        JsonObject groups = document.root().getAsJsonObject("playlists");
        JsonElement raw = groups.get(type.name());
        if (raw == null) groups.add(type.name(), new JsonObject());
        else if (!(raw instanceof JsonObject)) throw new CPlaylistStorageException(CPlaylistStorageException.Reason.INVALID_DATA);
        return new Document(document.root(), new LinkedHashMap<>(), document.original(), type, document.version());
    }

    private Document read(CTrackSourceType type) throws IOException {
        try {
            return readDocument(type);
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

    private Document readDocument(CTrackSourceType type) throws IOException {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(this.path, BasicFileAttributes.class);
        } catch (NoSuchFileException exception) {
            JsonObject root = new JsonObject();
            root.addProperty("formatVersion", CPlaylistFormat.CURRENT_VERSION);
            root.add("playlists", new JsonObject());
            this.status = new CPlaylistStorageStatus(CPlaylistStorageStatus.State.MISSING, List.of());
            return view(new Document(root, new LinkedHashMap<>(), null, type, CPlaylistFormat.CURRENT_VERSION), type);
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
        long fallbackTime = Math.max(0L, attributes.lastModifiedTime().toMillis());
        if (version < 3) {
            JsonObject groups = new JsonObject();
            JsonObject unknown = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : rawPlaylists.entrySet()) {
                CTrackSourceType source = CPlaylistFormat.sourceType(entry.getValue(), version);
                if (source == null) {
                    unknown.add(entry.getKey(), entry.getValue());
                    continue;
                }
                if (!groups.has(source.name())) groups.add(source.name(), new JsonObject());
                JsonElement value = entry.getValue();
                if (version == 1) {
                    CPlaylistData parsedPlaylist = parse(value, version, fallbackTime);
                    if (parsedPlaylist != null) {
                        JsonObject migrated = value instanceof JsonObject object ? object : new JsonObject();
                        if (value instanceof JsonArray) migrated.add("tracks", value);
                        migrated.addProperty("sourceType", source.name());
                        migrated.addProperty("modifiedAt", parsedPlaylist.modifiedAt());
                        value = migrated;
                    }
                }
                groups.getAsJsonObject(source.name()).add(entry.getKey(), value);
            }
            if (!unknown.isEmpty()) groups.add("UNKNOWN_LEGACY", unknown);
            root.add("playlists", groups);
            root.addProperty("formatVersion", CPlaylistFormat.CURRENT_VERSION);
            publish(new Document(root, new LinkedHashMap<>(), original, type, version), true);
            return readDocument(type);
        }
        Document document = view(new Document(root, new LinkedHashMap<>(), original, type, version), type);
        List<String> unreadable = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : document.rawPlaylists().entrySet()) {
            CPlaylistData playlist = parse(entry.getValue(), version, fallbackTime);
            if (playlist == null || playlist.sourceType() != type) unreadable.add(entry.getKey());
            else document.playlists().put(entry.getKey(), playlist);
        }
        this.status = new CPlaylistStorageStatus(unreadable.isEmpty()
            ? CPlaylistStorageStatus.State.READY : CPlaylistStorageStatus.State.PARTIAL, unreadable);
        return document;
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
                archive(document.original(), document.version());
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

    private void archive(byte[] original, int version) throws IOException {
        Path archive = this.path.resolveSibling(this.path.getFileName() + (version == 1 ? ".migrated" : ".v" + version + ".migrated"));
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

    private record Document(JsonObject root, LinkedHashMap<String, CPlaylistData> playlists, byte[] original, CTrackSourceType type, int version) {
        JsonObject rawPlaylists() {
            return this.root.getAsJsonObject("playlists").getAsJsonObject(this.type.name());
        }
    }
}
