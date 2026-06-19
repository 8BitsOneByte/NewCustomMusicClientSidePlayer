package org.exmple.newcustommusicclientsideplayer.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CModConfigRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;

    public CModConfigRepository() {
        this(FabricLoader.getInstance()
            .getConfigDir()
            .resolve(NewcustommusicclientsideplayerClient.MOD_ID)
            .resolve("config.json"));
    }

    public CModConfigRepository(Path configPath) {
        this.configPath = Objects.requireNonNull(configPath, "configPath").toAbsolutePath().normalize();
    }

    public CModConfig load() {
        if (!Files.isRegularFile(this.configPath)) {
            return CModConfig.defaults();
        }

        try (Reader reader = Files.newBufferedReader(this.configPath, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!(root instanceof JsonObject object)) {
                return CModConfig.defaults();
            }

            return new CModConfig(readBoolean(
                object,
                "checkForUpdates",
                CModConfig.DEFAULT_CHECK_FOR_UPDATES
            ));
        } catch (IOException | RuntimeException exception) {
            return CModConfig.defaults();
        }
    }

    /**
     * Writes the complete configuration to a temporary file in the destination directory before replacing
     * the live file. Keeping both files on the same file system allows an atomic move when supported; the
     * fallback move is used only when the provider explicitly reports that atomic replacement is unavailable.
     */
    public void save(CModConfig config) throws IOException {
        Objects.requireNonNull(config, "config");

        Path parent = this.configPath.getParent();
        if (parent == null) {
            throw new IOException("Configuration path has no parent directory: " + this.configPath);
        }

        Files.createDirectories(parent);
        Path temporaryPath = Files.createTempFile(parent, "config-", ".tmp");
        boolean replaced = false;
        try {
            JsonObject root = new JsonObject();
            root.addProperty("checkForUpdates", config.checkForUpdates());
            try (Writer writer = Files.newBufferedWriter(temporaryPath, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            replaceConfigurationFile(temporaryPath);
            replaced = true;
        } finally {
            if (!replaced) {
                Files.deleteIfExists(temporaryPath);
            }
        }
    }

    private void replaceConfigurationFile(Path temporaryPath) throws IOException {
        try {
            Files.move(
                temporaryPath,
                this.configPath,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryPath, this.configPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean readBoolean(JsonObject object, String key, boolean defaultValue) {
        JsonElement element = object.get(key);
        if (element instanceof JsonPrimitive primitive && primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        return defaultValue;
    }
}
