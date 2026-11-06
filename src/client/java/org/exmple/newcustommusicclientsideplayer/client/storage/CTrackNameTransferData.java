package org.exmple.newcustommusicclientsideplayer.client.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CTrackNameTransferData {
    public static final int CURRENT_FORMAT_VERSION = 2;

    private int formatVersion = CURRENT_FORMAT_VERSION;
    private String modId = NewcustommusicclientsideplayerClient.MOD_ID;
    private String modVersion = "";
    private String minecraftVersion = "";
    private long exportedAt;
    private com.google.gson.JsonObject trackNames = new com.google.gson.JsonObject();

    CTrackNameTransferData() {
    }

    public CTrackNameTransferData(
        String modVersion,
        String minecraftVersion,
        long exportedAt,
        com.google.gson.JsonObject trackNames
    ) {
        this.modVersion = Objects.requireNonNull(modVersion, "modVersion");
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        this.exportedAt = exportedAt;
        this.trackNames = Objects.requireNonNull(trackNames, "trackNames").deepCopy();
    }
}
