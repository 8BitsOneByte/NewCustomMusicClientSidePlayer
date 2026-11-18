package org.exmple.newcustommusicclientsideplayer.client.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CTrackNameRepository {
    public static final int MAX_TRACK_NAME_LENGTH = 50;
    public static final int INPUT_MAX_LENGTH = 60;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve(NewcustommusicclientsideplayerClient.MOD_ID)
        .resolve("track_names.json");

    private static JsonObject document;
    private static byte[] original;
    private static IOException loadFailure;

    private CTrackNameRepository() { }

    public static synchronized String getDisplayName(Identifier id) {
        return getDisplayName(CTrackSourceType.SOUND_EVENT, id);
    }

    public static synchronized String getDisplayName(CTrackSourceType type, Identifier id) {
        String name = getCustomName(type, id);
        return name == null ? getDefaultDisplayName(type, id) : name;
    }

    public static String getDefaultDisplayName(Identifier id) {
        return getDefaultDisplayName(CTrackSourceType.SOUND_EVENT, id);
    }

    public static String getDefaultDisplayName(CTrackSourceType type, Identifier id) {
        if (type == CTrackSourceType.OGG_RESOURCE)
            return org.exmple.newcustommusicclientsideplayer.client.audio.catalog.COggTrackNames.defaultName(id);
        String path = id.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static synchronized String getCustomName(Identifier id) {
        return getCustomName(CTrackSourceType.SOUND_EVENT, id);
    }

    public static synchronized String getCustomName(CTrackSourceType type, Identifier id) {
        ensureLoaded();
        JsonElement group = document.getAsJsonObject("trackNames").get(type.name());
        JsonElement value = group instanceof JsonObject names ? names.get(id.toString()) : null;
        if (!type.accepts(id) || !(value instanceof JsonPrimitive primitive) || !primitive.isString()) return null;
        String name = primitive.getAsString().trim();
        return name.isEmpty() || name.length() > MAX_TRACK_NAME_LENGTH ? null : name;
    }

    public static synchronized void renameTrack(Identifier id, String name) throws IOException {
        renameTrack(CTrackSourceType.SOUND_EVENT, id, name);
    }

    public static synchronized void renameTrack(CTrackSourceType type, Identifier id, String name) throws IOException {
        ensureLoaded();
        checkWritable();
        if (!type.accepts(id)) throw new IOException("Invalid track ID");
        String normalized = name.trim();
        boolean restore = normalized.isEmpty() || normalized.equals(getDefaultDisplayName(type, id));
        if (!restore && normalized.length() > MAX_TRACK_NAME_LENGTH) throw new IOException("Invalid track name");
        JsonObject updated = document.deepCopy();
        JsonObject group = writableGroup(updated, type);
        if (restore) group.remove(id.toString());
        else group.addProperty(id.toString(), normalized);
        publish(updated);
    }

    public static synchronized Map<String, String> exportCustomNames() throws IOException {
        return exportCustomNames(CTrackSourceType.SOUND_EVENT);
    }

    public static synchronized Map<String, String> exportCustomNames(CTrackSourceType type) throws IOException {
        ensureLoaded();
        checkWritable();
        Map<String, String> names = new LinkedHashMap<>();
        JsonElement group = document.getAsJsonObject("trackNames").get(type.name());
        if (group instanceof JsonObject object) {
            for (var entry : object.entrySet()) {
                if (entry.getValue() instanceof JsonPrimitive value && value.isString())
                    names.put(entry.getKey(), value.getAsString().trim());
            }
        } else if (group != null) throw new IOException("Unreadable source group");
        return names;
    }

    public static synchronized JsonObject exportGroupedNames() throws IOException {
        ensureLoaded();
        checkWritable();
        return document.getAsJsonObject("trackNames").deepCopy();
    }

    public static synchronized ImportResult importCustomNames(Map<Identifier, String> names) throws IOException {
        return importGroupedNames(Map.of(CTrackSourceType.SOUND_EVENT, names));
    }

    public static synchronized ImportResult importGroupedNames(
        Map<CTrackSourceType, Map<Identifier, String>> groups
    ) throws IOException {
        ensureLoaded();
        checkWritable();
        JsonObject updated = document.deepCopy();
        int changed = 0;
        for (var source : groups.entrySet()) {
            if (source.getValue().isEmpty()) continue;
            JsonObject group = writableGroup(updated, source.getKey());
            for (var entry : source.getValue().entrySet()) {
                if (!source.getKey().accepts(entry.getKey())) throw new IOException("Invalid track ID");
                String name = entry.getValue().trim();
                if (name.isEmpty() || name.length() > MAX_TRACK_NAME_LENGTH) throw new IOException("Invalid track name");
                String id = entry.getKey().toString();
                JsonElement old = group.get(id);
                if (!(old instanceof JsonPrimitive value) || !value.isString() || !name.equals(value.getAsString())) {
                    group.addProperty(id, name);
                    changed++;
                }
            }
        }
        if (changed > 0) publish(updated);
        return new ImportResult(changed);
    }

    private static JsonObject writableGroup(JsonObject root, CTrackSourceType type) throws IOException {
        JsonObject groups = root.getAsJsonObject("trackNames");
        JsonElement group = groups.get(type.name());
        if (group == null) {
            JsonObject names = new JsonObject();
            groups.add(type.name(), names);
            return names;
        }
        if (group instanceof JsonObject names) return names;
        throw new IOException("Unreadable source group");
    }

    private static JsonObject emptyDocument() {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", 2);
        root.add("trackNames", new JsonObject());
        return root;
    }

    private static void ensureLoaded() {
        if (document != null) return;
        document = emptyDocument();
        try {
            if (Files.notExists(FILE_PATH)) return;
            original = Files.readAllBytes(FILE_PATH);
            JsonElement parsed = JsonParser.parseString(new String(original, StandardCharsets.UTF_8));
            if (!(parsed instanceof JsonObject root) || !(root.get("trackNames") instanceof JsonObject names))
                throw new IOException("Invalid name data");
            int version = CPlaylistFormat.version(root.get("formatVersion"));
            if (version == 1) {
                for (var entry : names.entrySet()) {
                    Identifier id = Identifier.tryParse(entry.getKey());
                    if (!CTrackSourceType.SOUND_EVENT.accepts(id)
                        || !(entry.getValue() instanceof JsonPrimitive value) || !value.isString()
                        || value.getAsString().trim().isEmpty() || value.getAsString().trim().length() > MAX_TRACK_NAME_LENGTH)
                        throw new IOException("Invalid legacy data");
                }
                JsonObject updated = root.deepCopy();
                JsonObject groups = new JsonObject();
                groups.add(CTrackSourceType.SOUND_EVENT.name(), names.deepCopy());
                updated.add("trackNames", groups);
                updated.addProperty("formatVersion", 2);
                verifyUnchanged();
                Path archive = FILE_PATH.resolveSibling("track_names.json.migrated");
                if (Files.exists(archive)) {
                    if (!java.util.Arrays.equals(original, Files.readAllBytes(archive))) throw new IOException("Archive conflict");
                } else Files.write(archive, original, java.nio.file.StandardOpenOption.CREATE_NEW);
                publish(updated);
            } else document = root;
        } catch (Exception exception) {
            document = emptyDocument();
            loadFailure = new IOException("Unreadable name data", exception);
        }
    }

    private static void checkWritable() throws IOException {
        if (loadFailure != null) throw loadFailure;
    }

    private static void verifyUnchanged() throws IOException {
        byte[] current = Files.notExists(FILE_PATH) ? null : Files.readAllBytes(FILE_PATH);
        if (!java.util.Arrays.equals(original, current)) throw new IOException("Name data changed");
    }

    private static void publish(JsonObject updated) throws IOException {
        Files.createDirectories(FILE_PATH.getParent());
        Path temporary = Files.createTempFile(FILE_PATH.getParent(), "track_names-", ".tmp");
        try {
            byte[] bytes = GSON.toJson(updated).getBytes(StandardCharsets.UTF_8);
            Files.write(temporary, bytes);
            verifyUnchanged();
            Files.move(temporary, FILE_PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            original = bytes;
            document = updated;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public record ImportResult(int changedCount) {
    }
}

