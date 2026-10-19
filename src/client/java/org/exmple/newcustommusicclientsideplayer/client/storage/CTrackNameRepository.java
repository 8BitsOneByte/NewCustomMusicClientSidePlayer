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

    private static Map<String, String> customNames;
    private static IOException loadFailure;

    private CTrackNameRepository() {
    }

    public static synchronized String getDisplayName(Identifier trackId) {
        String customName = getCustomName(trackId);
        return customName != null ? customName : getDefaultDisplayName(trackId);
    }

    public static synchronized String getDefaultDisplayName(Identifier trackId) {
        String path = trackId.getPath();
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    public static synchronized String getCustomName(Identifier trackId) {
        ensureLoaded();
        return customNames.get(trackId.toString());
    }

    public static synchronized void renameTrack(Identifier trackId, String newName) throws IOException {
        ensureLoaded();
        throwIfLoadFailed();

        LinkedHashMap<String, String> updatedNames = new LinkedHashMap<>(customNames);
        String normalized = newName.trim();
        String defaultName = getDefaultDisplayName(trackId);
        if (normalized.isEmpty() || normalized.equals(defaultName)) {
            updatedNames.remove(trackId.toString());
        } else {
            updatedNames.put(trackId.toString(), normalized);
        }

        writeData(updatedNames);
        customNames = updatedNames;
    }

    public static synchronized Map<String, String> exportCustomNames() throws IOException {
        ensureLoaded();
        throwIfLoadFailed();
        return new LinkedHashMap<>(customNames);
    }

    public static synchronized ImportResult importCustomNames(Map<Identifier, String> names) throws IOException {
        ensureLoaded();
        throwIfLoadFailed();

        LinkedHashMap<String, String> updatedNames = new LinkedHashMap<>(customNames);
        int changedCount = 0;
        for (Map.Entry<Identifier, String> entry : names.entrySet()) {
            String trackId = entry.getKey().toString();
            String importedName = entry.getValue();
            if (!importedName.equals(updatedNames.get(trackId))) {
                updatedNames.put(trackId, importedName);
                changedCount++;
            }
        }

        if (changedCount > 0) {
            writeData(updatedNames);
            customNames = updatedNames;
        }

        return new ImportResult(changedCount);
    }

    private static void ensureLoaded() {
        if (customNames != null) {
            return;
        }

        customNames = new LinkedHashMap<>();
        if (!Files.exists(FILE_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(FILE_PATH, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!(root instanceof JsonObject rootObject)) {
                return;
            }

            JsonElement namesElement = rootObject.get("trackNames");
            if (!(namesElement instanceof JsonObject namesObject)) {
                return;
            }

            for (Map.Entry<String, JsonElement> entry : namesObject.entrySet()) {
                if (entry.getValue() instanceof JsonPrimitive primitive && primitive.isString()) {
                    String value = primitive.getAsString().trim();
                    if (!value.isEmpty()) {
                        customNames.put(entry.getKey(), value);
                    }
                }
            }
        } catch (Exception exception) {
            customNames = new LinkedHashMap<>();
            loadFailure = new IOException("Could not read track name data.", exception);
        }
    }

    private static void throwIfLoadFailed() throws IOException {
        if (loadFailure != null) {
            throw loadFailure;
        }
    }

    private static void writeData(Map<String, String> names) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject namesObject = new JsonObject();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            namesObject.addProperty(entry.getKey(), entry.getValue());
        }

        root.add("trackNames", namesObject);
        String json = GSON.toJson(root);
        Files.createDirectories(FILE_PATH.getParent());
        Path temporaryFile = Files.createTempFile(FILE_PATH.getParent(), "track_names-", ".tmp");
        try {
            Files.writeString(temporaryFile, json, StandardCharsets.UTF_8);
            // Publish the complete document in one operation; unsupported atomic moves fail the save.
            Files.move(temporaryFile, FILE_PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException | Error exception) {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException | RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    public record ImportResult(int changedCount) {
    }
}

