package org.exmple.newcustommusicclientsideplayer.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackSourceType;

public final class CModConfigRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String KEEP_PLAYING_IN_GAME_MENU_KEY = "keepPlayingInGameMenu";
    private static final String CHECK_FOR_UPDATES_KEY = "checkForUpdates";
    private static final String NOW_PLAYING_TOAST_MODE_KEY = "nowPlayingToastMode";
    private static final String NOW_PLAYING_TOAST_ENABLED_KEY = "nowPlayingToastEnabled";
    private static final String NOW_PLAYING_FEEDBACK_MODE_KEY = "nowPlayingFeedbackMode";
    private static final String PLAYBACK_VOLUME_PERCENT_KEY = "playbackVolumePercent";
    private static final String DEFAULT_TRACK_SOURCE_KEY = "defaultTrackSource";
    private static final String LEGACY_PLAYBACK_VOLUME_FILE_NAME = "playback_volume.json";
    private static final String MIGRATED_PLAYBACK_VOLUME_FILE_NAME = "playback_volume.json.migrated";

    private final Path configPath;
    private final Path legacyPlaybackVolumePath;
    private final Path migratedPlaybackVolumePath;

    public CModConfigRepository() {
        this(FabricLoader.getInstance()
            .getConfigDir()
            .resolve(NewcustommusicclientsideplayerClient.MOD_ID)
            .resolve("config.json"));
    }

    public CModConfigRepository(Path configPath) {
        this.configPath = Objects.requireNonNull(configPath, "configPath").toAbsolutePath().normalize();
        this.legacyPlaybackVolumePath = this.configPath.resolveSibling(LEGACY_PLAYBACK_VOLUME_FILE_NAME);
        this.migratedPlaybackVolumePath = this.configPath.resolveSibling(MIGRATED_PLAYBACK_VOLUME_FILE_NAME);
    }

    public CModConfig load() {
        if (Files.notExists(this.configPath)) {
            return this.initializeWithoutConfig();
        }
        if (!Files.isRegularFile(this.configPath)) {
            return this.legacyDefaults();
        }

        JsonElement root;
        try (Reader reader = Files.newBufferedReader(this.configPath, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader);
        } catch (IOException | RuntimeException exception) {
            return this.legacyDefaults();
        }

        if (!(root instanceof JsonObject object)) {
            return this.legacyDefaults();
        }

        boolean playbackVolumeMissing = !object.has(PLAYBACK_VOLUME_PERCENT_KEY);
        Integer storedPlaybackVolume = playbackVolumeMissing
            ? null
            : readInteger(object, PLAYBACK_VOLUME_PERCENT_KEY);
        int playbackVolumePercent = playbackVolumeMissing
            ? this.readLegacyPlaybackVolume()
            : storedPlaybackVolume == null
                ? CModConfig.DEFAULT_PLAYBACK_VOLUME_PERCENT
                : CModConfig.clampPlaybackVolumePercent(storedPlaybackVolume);
        boolean normalizePlaybackVolume = storedPlaybackVolume != null
            && storedPlaybackVolume != playbackVolumePercent;

        CModConfig config = new CModConfig(
            readBoolean(object, CHECK_FOR_UPDATES_KEY, CModConfig.DEFAULT_CHECK_FOR_UPDATES),
            readNowPlayingToastMode(object),
            readNowPlayingFeedbackMode(
                object,
                NOW_PLAYING_FEEDBACK_MODE_KEY,
                CModConfig.DEFAULT_NOW_PLAYING_FEEDBACK_MODE
            ),
            playbackVolumePercent,
            readBoolean(object, KEEP_PLAYING_IN_GAME_MENU_KEY, CModConfig.DEFAULT_KEEP_PLAYING_IN_GAME_MENU),
            readDefaultTrackSource(object)
        );
        boolean migrationSaved = this.migrateMissingFields(object, config, normalizePlaybackVolume);
        if ((!playbackVolumeMissing && storedPlaybackVolume != null)
            || (playbackVolumeMissing && migrationSaved)) {
            this.retireLegacyPlaybackVolumeFile();
        }
        return config;
    }

    private CModConfig initializeWithoutConfig() {
        CModConfig config = CModConfig.defaults()
            .withDefaultTrackSource(this.hasExistingUserData()
                ? CTrackSourceType.SOUND_EVENT : CTrackSourceType.OGG_RESOURCE)
            .withPlaybackVolumePercent(this.readLegacyPlaybackVolume());
        try {
            this.save(config);
            this.retireLegacyPlaybackVolumeFile();
        } catch (IOException | RuntimeException ignored) {
        }
        return config;
    }

    private CModConfig legacyDefaults() {
        return CModConfig.defaults().withDefaultTrackSource(CTrackSourceType.SOUND_EVENT)
            .withPlaybackVolumePercent(this.readLegacyPlaybackVolume());
    }

    private boolean hasExistingUserData() {
        for (String name : new String[]{
            "playlists.json", "playlists.json.migrated",
            "track_names.json", "track_names.json.migrated",
            LEGACY_PLAYBACK_VOLUME_FILE_NAME, MIGRATED_PLAYBACK_VOLUME_FILE_NAME
        }) {
            if (!Files.notExists(this.configPath.resolveSibling(name))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes the complete configuration to a temporary file in the destination directory before replacing
     * the live file. Saving requires atomic replacement; unsupported atomic moves propagate as save failures.
     */
    public void save(CModConfig config) throws IOException {
        Objects.requireNonNull(config, "config");

        JsonObject root = new JsonObject();
        root.addProperty(KEEP_PLAYING_IN_GAME_MENU_KEY, config.keepPlayingInGameMenu());
        root.addProperty(CHECK_FOR_UPDATES_KEY, config.checkForUpdates());
        root.addProperty(NOW_PLAYING_TOAST_MODE_KEY, config.nowPlayingToastMode().name());
        root.addProperty(NOW_PLAYING_FEEDBACK_MODE_KEY, config.nowPlayingFeedbackMode().name());
        root.addProperty(PLAYBACK_VOLUME_PERCENT_KEY, config.playbackVolumePercent());
        root.addProperty(DEFAULT_TRACK_SOURCE_KEY, config.defaultTrackSource().name());
        this.writeConfiguration(root);
    }

    private boolean migrateMissingFields(
        JsonObject root,
        CModConfig config,
        boolean normalizePlaybackVolume
    ) {
        boolean migrationNeeded = false;
        if (!root.has(DEFAULT_TRACK_SOURCE_KEY)) {
            root.addProperty(DEFAULT_TRACK_SOURCE_KEY, config.defaultTrackSource().name());
            migrationNeeded = true;
        }
        if (!root.has(KEEP_PLAYING_IN_GAME_MENU_KEY)) {
            root.addProperty(KEEP_PLAYING_IN_GAME_MENU_KEY, config.keepPlayingInGameMenu());
            migrationNeeded = true;
        }
        if (!root.has(CHECK_FOR_UPDATES_KEY)) {
            root.addProperty(CHECK_FOR_UPDATES_KEY, config.checkForUpdates());
            migrationNeeded = true;
        }
        if (!root.has(NOW_PLAYING_TOAST_MODE_KEY)) {
            root.addProperty(NOW_PLAYING_TOAST_MODE_KEY, config.nowPlayingToastMode().name());
            root.remove(NOW_PLAYING_TOAST_ENABLED_KEY);
            migrationNeeded = true;
        }
        if (!root.has(NOW_PLAYING_FEEDBACK_MODE_KEY)) {
            root.addProperty(NOW_PLAYING_FEEDBACK_MODE_KEY, config.nowPlayingFeedbackMode().name());
            migrationNeeded = true;
        }
        if (!root.has(PLAYBACK_VOLUME_PERCENT_KEY) || normalizePlaybackVolume) {
            root.addProperty(PLAYBACK_VOLUME_PERCENT_KEY, config.playbackVolumePercent());
            migrationNeeded = true;
        }
        if (!migrationNeeded) {
            return true;
        }

        try {
            this.writeConfiguration(root);
            return true;
        } catch (IOException | RuntimeException ignored) {
            // Migration is best-effort; the successfully loaded in-memory configuration remains usable.
            return false;
        }
    }

    private int readLegacyPlaybackVolume() {
        if (!Files.isRegularFile(this.legacyPlaybackVolumePath)) {
            return CModConfig.DEFAULT_PLAYBACK_VOLUME_PERCENT;
        }

        try (Reader reader = Files.newBufferedReader(this.legacyPlaybackVolumePath, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root instanceof JsonObject object) {
                Integer storedVolume = readInteger(object, PLAYBACK_VOLUME_PERCENT_KEY);
                if (storedVolume != null) {
                    return CModConfig.clampPlaybackVolumePercent(storedVolume);
                }
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return CModConfig.DEFAULT_PLAYBACK_VOLUME_PERCENT;
    }

    private void retireLegacyPlaybackVolumeFile() {
        if (!Files.isRegularFile(this.legacyPlaybackVolumePath)
            || Files.exists(this.migratedPlaybackVolumePath)) {
            return;
        }

        try {
            Files.move(
                this.legacyPlaybackVolumePath,
                this.migratedPlaybackVolumePath,
                StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            try {
                Files.move(this.legacyPlaybackVolumePath, this.migratedPlaybackVolumePath);
            } catch (IOException ignored) {
            }
        } catch (IOException ignored) {
        }
    }

    private void writeConfiguration(JsonObject root) throws IOException {
        String json = GSON.toJson(root);
        Path parent = this.configPath.getParent();
        if (parent == null) {
            throw new IOException("Configuration path has no parent directory: " + this.configPath);
        }

        Files.createDirectories(parent);
        Path temporaryPath = Files.createTempFile(parent, "config-", ".tmp");
        try {
            Files.writeString(temporaryPath, json, StandardCharsets.UTF_8);
            // Publish the complete document in one operation; unsupported atomic moves fail the save.
            Files.move(
                temporaryPath,
                this.configPath,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException | RuntimeException | Error exception) {
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (IOException | RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    private static CNowPlayingToastMode readNowPlayingToastMode(JsonObject object) {
        if (object.has(NOW_PLAYING_TOAST_MODE_KEY)) {
            CNowPlayingToastMode mode = parseNowPlayingToastMode(object.get(NOW_PLAYING_TOAST_MODE_KEY));
            return mode == null ? CModConfig.DEFAULT_NOW_PLAYING_TOAST_MODE : mode;
        }
        return readBoolean(object, NOW_PLAYING_TOAST_ENABLED_KEY, true)
            ? CNowPlayingToastMode.ON : CNowPlayingToastMode.OFF;
    }

    static CNowPlayingToastMode parseNowPlayingToastMode(JsonElement element) {
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            try {
                return CNowPlayingToastMode.valueOf(primitive.getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private static boolean readBoolean(JsonObject object, String key, boolean defaultValue) {
        JsonElement element = object.get(key);
        if (element instanceof JsonPrimitive primitive && primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        return defaultValue;
    }

    private static CTrackSourceType readDefaultTrackSource(JsonObject object) {
        JsonElement element = object.get(DEFAULT_TRACK_SOURCE_KEY);
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            try {
                return CTrackSourceType.valueOf(primitive.getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return CTrackSourceType.SOUND_EVENT;
    }

    private static Integer readInteger(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
            try {
                return primitive.getAsBigDecimal().intValueExact();
            } catch (ArithmeticException | NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static CNowPlayingFeedbackMode readNowPlayingFeedbackMode(
        JsonObject object,
        String key,
        CNowPlayingFeedbackMode defaultValue
    ) {
        JsonElement element = object.get(key);
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            try {
                return CNowPlayingFeedbackMode.valueOf(primitive.getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return defaultValue;
    }
}
