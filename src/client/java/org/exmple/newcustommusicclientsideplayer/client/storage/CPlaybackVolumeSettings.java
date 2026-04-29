package org.exmple.newcustommusicclientsideplayer.client.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CPlaybackVolumeSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve(NewcustommusicclientsideplayerClient.MOD_ID)
        .resolve("playback_volume.json");

    private CPlaybackVolumeSettings() {
    }

    public static synchronized int loadPlaybackVolumePercent() {
        if (!Files.exists(FILE_PATH)) {
            return 100;
        }

        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            StoredData data = GSON.fromJson(reader, StoredData.class);
            if (data == null) {
                return 100;
            }

            return clamp(data.playbackVolumePercent);
        } catch (IOException exception) {
            return 100;
        }
    }

    public static synchronized void savePlaybackVolumePercent(int playbackVolumePercent) throws IOException {
        StoredData data = new StoredData();
        data.playbackVolumePercent = clamp(playbackVolumePercent);
        Files.createDirectories(FILE_PATH.getParent());
        try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
            GSON.toJson(data, writer);
        }
    }

    private static int clamp(int playbackVolumePercent) {
        return Math.max(0, Math.min(100, playbackVolumePercent));
    }

    private static final class StoredData {
        private int playbackVolumePercent = 100;
    }
}

