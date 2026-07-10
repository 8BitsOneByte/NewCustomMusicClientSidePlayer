package org.exmple.newcustommusicclientsideplayer.client.config;

import java.util.Objects;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CModConfigTransferData {
    public static final int CURRENT_FORMAT_VERSION = 1;

    private int formatVersion = CURRENT_FORMAT_VERSION;
    private String modId = NewcustommusicclientsideplayerClient.MOD_ID;
    private String modVersion = "";
    private String minecraftVersion = "";
    private long exportedAt;
    private CModConfig config = CModConfig.defaults();

    CModConfigTransferData() {
    }

    public CModConfigTransferData(
        String modVersion,
        String minecraftVersion,
        long exportedAt,
        CModConfig config
    ) {
        this.modVersion = Objects.requireNonNull(modVersion, "modVersion");
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        this.exportedAt = exportedAt;
        this.config = Objects.requireNonNull(config, "config");
    }

    public int formatVersion() {
        return this.formatVersion;
    }

    public String modId() {
        return this.modId;
    }

    public String modVersion() {
        return this.modVersion;
    }

    public String minecraftVersion() {
        return this.minecraftVersion;
    }

    public long exportedAt() {
        return this.exportedAt;
    }

    public CModConfig config() {
        return this.config;
    }
}
