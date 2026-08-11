package org.exmple.newcustommusicclientsideplayer.client.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CTrackNameTransferData {
    public static final int CURRENT_FORMAT_VERSION = 1;

    private int formatVersion = CURRENT_FORMAT_VERSION;
    private String modId = NewcustommusicclientsideplayerClient.MOD_ID;
    private String modVersion = "";
    private String minecraftVersion = "";
    private long exportedAt;
    private LinkedHashMap<String, String> trackNames = new LinkedHashMap<>();

    CTrackNameTransferData() {
    }

    public CTrackNameTransferData(
        String modVersion,
        String minecraftVersion,
        long exportedAt,
        Map<String, String> trackNames
    ) {
        this.modVersion = Objects.requireNonNull(modVersion, "modVersion");
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        this.exportedAt = exportedAt;
        this.trackNames = new LinkedHashMap<>(Objects.requireNonNull(trackNames, "trackNames"));
    }
}
