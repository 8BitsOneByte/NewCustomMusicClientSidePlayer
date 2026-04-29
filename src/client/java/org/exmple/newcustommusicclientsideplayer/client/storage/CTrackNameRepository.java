package org.exmple.newcustommusicclientsideplayer.client.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
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

        String normalized = newName.trim();
        String defaultName = getDefaultDisplayName(trackId);
        if (normalized.isEmpty() || normalized.equals(defaultName)) {
            customNames.remove(trackId.toString());
        } else {
            customNames.put(trackId.toString(), normalized);
        }

        writeData();
    }

    private static void ensureLoaded() {
        if (customNames != null) {
            return;
        }

        customNames = new LinkedHashMap<>();
        if (!Files.exists(FILE_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
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
        } catch (Exception ignored) {
            customNames = new LinkedHashMap<>();
        }
    }

    private static void writeData() throws IOException {
        Files.createDirectories(FILE_PATH.getParent());

        JsonObject root = new JsonObject();
        JsonObject namesObject = new JsonObject();
        for (Map.Entry<String, String> entry : customNames.entrySet()) {
            namesObject.addProperty(entry.getKey(), entry.getValue());
        }

        root.add("trackNames", namesObject);
        try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
            GSON.toJson(root, writer);
        }
    }
}

