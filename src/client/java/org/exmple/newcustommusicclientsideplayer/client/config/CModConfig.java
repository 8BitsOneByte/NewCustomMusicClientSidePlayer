package org.exmple.newcustommusicclientsideplayer.client.config;

public record CModConfig(boolean checkForUpdates) {
    public static final boolean DEFAULT_CHECK_FOR_UPDATES = true;

    public static CModConfig defaults() {
        return new CModConfig(DEFAULT_CHECK_FOR_UPDATES);
    }

    public CModConfig withCheckForUpdates(boolean enabled) {
        return new CModConfig(enabled);
    }
}
