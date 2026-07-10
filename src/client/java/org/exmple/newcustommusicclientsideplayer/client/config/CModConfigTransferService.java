package org.exmple.newcustommusicclientsideplayer.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CModConfigTransferService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FALLBACK_MOD_NAME = "NewCustomMusicClientSidePlayer";
    private static final String UNKNOWN_VERSION = "unknown";
    private static final String MINECRAFT_MOD_ID = "minecraft";
    private static final String CONFIG_KEY = "config";
    private static final String MOD_ID_KEY = "modId";
    private static final String CHECK_FOR_UPDATES_KEY = "checkForUpdates";

    public String defaultFileName() {
        return sanitizeFileName(modName()) + "-" + sanitizeFileName(modVersion()) + "-config.txt";
    }

    public void exportConfig(CModConfig config, Path path) throws IOException {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(path, "path");

        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        CModConfigTransferData data = new CModConfigTransferData(
            modVersion(),
            minecraftVersion(),
            System.currentTimeMillis(),
            config
        );
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    public CConfigImportPreview previewImport(Path path, CModConfig currentConfig) throws CConfigImportException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(currentConfig, "currentConfig");

        JsonObject root = readRootObject(path);
        validateModId(root);
        JsonObject configObject = readConfigObject(root);

        return scanConfig(configObject, currentConfig);
    }

    private static JsonObject readRootObject(Path path) throws CConfigImportException {
        if (!Files.isRegularFile(path)) {
            throw CConfigImportException.ioError(null);
        }

        try {
            if (Files.size(path) == 0L) {
                throw CConfigImportException.ioError(null);
            }
        } catch (IOException exception) {
            throw CConfigImportException.ioError(exception);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root instanceof JsonObject rootObject) {
                return rootObject;
            }
        } catch (JsonSyntaxException | IllegalStateException exception) {
            throw CConfigImportException.invalidConfigFile(exception);
        } catch (IOException exception) {
            throw CConfigImportException.ioError(exception);
        }

        throw CConfigImportException.invalidConfigFile();
    }

    private static void validateModId(JsonObject root) throws CConfigImportException {
        JsonElement modIdElement = root.get(MOD_ID_KEY);
        if (!(modIdElement instanceof JsonPrimitive primitive) || !primitive.isString()) {
            throw CConfigImportException.invalidConfigFile();
        }

        if (!NewcustommusicclientsideplayerClient.MOD_ID.equals(primitive.getAsString())) {
            throw CConfigImportException.invalidConfigFile();
        }
    }

    private static JsonObject readConfigObject(JsonObject root) throws CConfigImportException {
        JsonElement configElement = root.get(CONFIG_KEY);
        if (configElement instanceof JsonObject configObject) {
            return configObject;
        }

        throw CConfigImportException.invalidConfigFile();
    }

    private static CConfigImportPreview scanConfig(JsonObject configObject, CModConfig currentConfig) throws CConfigImportException {
        CModConfig compatibleConfig = currentConfig;
        boolean hasCompatibleSettings = false;
        List<String> skippedFields = new ArrayList<>();

        // Compatibility policy:
        // - missing known fields keep the current draft value and do not warn;
        // - known fields with the expected type are imported;
        // - known fields with any other JSON type are skipped and reported;
        // - unknown fields, including formatVersion metadata, are ignored.
        if (configObject.has(CHECK_FOR_UPDATES_KEY)) {
            JsonElement element = configObject.get(CHECK_FOR_UPDATES_KEY);
            if (element instanceof JsonPrimitive primitive && primitive.isBoolean()) {
                compatibleConfig = compatibleConfig.withCheckForUpdates(primitive.getAsBoolean());
                hasCompatibleSettings = true;
            } else {
                skippedFields.add(CHECK_FOR_UPDATES_KEY);
            }
        }

        if (!hasCompatibleSettings) {
            throw CConfigImportException.noCompatibleSettings();
        }

        return new CConfigImportPreview(
            compatibleConfig,
            !compatibleConfig.equals(currentConfig),
            skippedFields
        );
    }

    private static String modName() {
        return FabricLoader.getInstance()
            .getModContainer(NewcustommusicclientsideplayerClient.MOD_ID)
            .map(container -> container.getMetadata().getName())
            .filter(name -> !name.isBlank())
            .orElse(FALLBACK_MOD_NAME);
    }

    private static String modVersion() {
        return modVersion(NewcustommusicclientsideplayerClient.MOD_ID);
    }

    private static String minecraftVersion() {
        return modVersion(MINECRAFT_MOD_ID);
    }

    private static String modVersion(String modId) {
        return FabricLoader.getInstance()
            .getModContainer(modId)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .filter(version -> !version.isBlank())
            .orElse(UNKNOWN_VERSION);
    }

    private static String sanitizeFileName(String value) {
        String sanitized = value.trim().replaceAll("[\\\\/:*?\"<>|]+", "_");
        if (sanitized.isBlank()) {
            return UNKNOWN_VERSION;
        }

        return sanitized.toLowerCase(Locale.ROOT).equals("unknown") ? UNKNOWN_VERSION : sanitized;
    }
}
