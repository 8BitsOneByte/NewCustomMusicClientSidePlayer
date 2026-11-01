package org.exmple.newcustommusicclientsideplayer.client.config;

import java.util.Objects;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackSourceType;

public record CModConfig(
    boolean checkForUpdates,
    boolean nowPlayingToastEnabled,
    CNowPlayingFeedbackMode nowPlayingFeedbackMode,
    int playbackVolumePercent,
    boolean keepPlayingInGameMenu,
    CTrackSourceType defaultTrackSource
) {
    public static final CTrackSourceType DEFAULT_TRACK_SOURCE = CTrackSourceType.OGG_RESOURCE;
    public static final boolean DEFAULT_KEEP_PLAYING_IN_GAME_MENU = false;
    public static final boolean DEFAULT_CHECK_FOR_UPDATES = true;
    public static final boolean DEFAULT_NOW_PLAYING_TOAST_ENABLED = true;
    public static final CNowPlayingFeedbackMode DEFAULT_NOW_PLAYING_FEEDBACK_MODE = CNowPlayingFeedbackMode.CHAT;
    public static final int DEFAULT_PLAYBACK_VOLUME_PERCENT = 100;
    public static final int MIN_PLAYBACK_VOLUME_PERCENT = 0;
    public static final int MAX_PLAYBACK_VOLUME_PERCENT = 100;
    public static final int PLAYBACK_VOLUME_SHORTCUT_STEP_PERCENT = 5;

    public CModConfig {
        Objects.requireNonNull(nowPlayingFeedbackMode, "nowPlayingFeedbackMode");
        Objects.requireNonNull(defaultTrackSource, "defaultTrackSource");
        playbackVolumePercent = clampPlaybackVolumePercent(playbackVolumePercent);
    }

    public static CModConfig defaults() {
        return new CModConfig(
            DEFAULT_CHECK_FOR_UPDATES,
            DEFAULT_NOW_PLAYING_TOAST_ENABLED,
            DEFAULT_NOW_PLAYING_FEEDBACK_MODE,
            DEFAULT_PLAYBACK_VOLUME_PERCENT,
            DEFAULT_KEEP_PLAYING_IN_GAME_MENU,
            DEFAULT_TRACK_SOURCE
        );
    }

    public CModConfig withCheckForUpdates(boolean enabled) {
        return new CModConfig(
            enabled,
            this.nowPlayingToastEnabled,
            this.nowPlayingFeedbackMode,
            this.playbackVolumePercent,
            this.keepPlayingInGameMenu,
            this.defaultTrackSource
        );
    }

    public CModConfig withNowPlayingToastEnabled(boolean enabled) {
        return new CModConfig(
            this.checkForUpdates,
            enabled,
            this.nowPlayingFeedbackMode,
            this.playbackVolumePercent,
            this.keepPlayingInGameMenu,
            this.defaultTrackSource
        );
    }

    public CModConfig withNowPlayingFeedbackMode(CNowPlayingFeedbackMode mode) {
        return new CModConfig(
            this.checkForUpdates,
            this.nowPlayingToastEnabled,
            mode,
            this.playbackVolumePercent,
            this.keepPlayingInGameMenu,
            this.defaultTrackSource
        );
    }

    public CModConfig withPlaybackVolumePercent(int playbackVolumePercent) {
        return new CModConfig(
            this.checkForUpdates,
            this.nowPlayingToastEnabled,
            this.nowPlayingFeedbackMode,
            playbackVolumePercent,
            this.keepPlayingInGameMenu,
            this.defaultTrackSource
        );
    }

    public CModConfig withKeepPlayingInGameMenu(boolean enabled) {
        return new CModConfig(
            this.checkForUpdates,
            this.nowPlayingToastEnabled,
            this.nowPlayingFeedbackMode,
            this.playbackVolumePercent,
            enabled,
            this.defaultTrackSource
        );
    }

    public CModConfig withDefaultTrackSource(CTrackSourceType sourceType) {
        return new CModConfig(
            this.checkForUpdates,
            this.nowPlayingToastEnabled,
            this.nowPlayingFeedbackMode,
            this.playbackVolumePercent,
            this.keepPlayingInGameMenu,
            sourceType
        );
    }

    public static int clampPlaybackVolumePercent(int playbackVolumePercent) {
        return Math.max(
            MIN_PLAYBACK_VOLUME_PERCENT,
            Math.min(MAX_PLAYBACK_VOLUME_PERCENT, playbackVolumePercent)
        );
    }
}
