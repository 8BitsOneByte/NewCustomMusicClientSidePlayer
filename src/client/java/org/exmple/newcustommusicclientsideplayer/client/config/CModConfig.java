package org.exmple.newcustommusicclientsideplayer.client.config;

import java.util.Objects;

public record CModConfig(
    boolean checkForUpdates,
    boolean nowPlayingToastEnabled,
    CNowPlayingFeedbackMode nowPlayingFeedbackMode
) {
    public static final boolean DEFAULT_CHECK_FOR_UPDATES = true;
    public static final boolean DEFAULT_NOW_PLAYING_TOAST_ENABLED = true;
    public static final CNowPlayingFeedbackMode DEFAULT_NOW_PLAYING_FEEDBACK_MODE = CNowPlayingFeedbackMode.CHAT;

    public CModConfig {
        Objects.requireNonNull(nowPlayingFeedbackMode, "nowPlayingFeedbackMode");
    }

    public static CModConfig defaults() {
        return new CModConfig(
            DEFAULT_CHECK_FOR_UPDATES,
            DEFAULT_NOW_PLAYING_TOAST_ENABLED,
            DEFAULT_NOW_PLAYING_FEEDBACK_MODE
        );
    }

    public CModConfig withCheckForUpdates(boolean enabled) {
        return new CModConfig(enabled, this.nowPlayingToastEnabled, this.nowPlayingFeedbackMode);
    }

    public CModConfig withNowPlayingToastEnabled(boolean enabled) {
        return new CModConfig(this.checkForUpdates, enabled, this.nowPlayingFeedbackMode);
    }

    public CModConfig withNowPlayingFeedbackMode(CNowPlayingFeedbackMode mode) {
        return new CModConfig(this.checkForUpdates, this.nowPlayingToastEnabled, mode);
    }
}
