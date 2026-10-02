package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.exmple.newcustommusicclientsideplayer.client.config.CModConfig;
import org.exmple.newcustommusicclientsideplayer.client.config.CNowPlayingFeedbackMode;

/** Formats unapplied configuration differences in the same order as the config screen. */
public final class CConfigDiscardChangeFormatter {
    private static final String CHANGE_KEY =
        "screen.custommusicclientsideplayer.unsaved_changes.change";
    private static final String PERCENT_KEY =
        "screen.custommusicclientsideplayer.unsaved_changes.percent";

    private static final Component CHECK_FOR_UPDATES =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.check_for_updates");
    private static final Component NOW_PLAYING_TOAST =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.now_playing_toast");
    private static final Component NOW_PLAYING_FEEDBACK =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.now_playing_feedback");
    private static final Component PLAYBACK_VOLUME =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.playback_volume");
    private static final Component KEEP_PLAYING_IN_GAME_MENU =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.keep_playing_in_game_menu");

    private CConfigDiscardChangeFormatter() {
    }

    public static List<Component> format(CModConfig applied, CModConfig draft) {
        Objects.requireNonNull(applied, "applied");
        Objects.requireNonNull(draft, "draft");

        List<Component> changes = new ArrayList<>();
        if (applied.checkForUpdates() != draft.checkForUpdates()) {
            changes.add(change(
                CHECK_FOR_UPDATES,
                CommonComponents.optionStatus(applied.checkForUpdates()),
                CommonComponents.optionStatus(draft.checkForUpdates())
            ));
        }
        if (applied.nowPlayingToastEnabled() != draft.nowPlayingToastEnabled()) {
            changes.add(change(
                NOW_PLAYING_TOAST,
                CommonComponents.optionStatus(applied.nowPlayingToastEnabled()),
                CommonComponents.optionStatus(draft.nowPlayingToastEnabled())
            ));
        }
        if (applied.nowPlayingFeedbackMode() != draft.nowPlayingFeedbackMode()) {
            changes.add(change(
                NOW_PLAYING_FEEDBACK,
                feedbackModeLabel(applied.nowPlayingFeedbackMode()),
                feedbackModeLabel(draft.nowPlayingFeedbackMode())
            ));
        }
        if (applied.playbackVolumePercent() != draft.playbackVolumePercent()) {
            changes.add(change(
                PLAYBACK_VOLUME,
                percent(applied.playbackVolumePercent()),
                percent(draft.playbackVolumePercent())
            ));
        }

        if (applied.keepPlayingInGameMenu() != draft.keepPlayingInGameMenu()) {
            changes.add(change(
                KEEP_PLAYING_IN_GAME_MENU,
                CommonComponents.optionStatus(applied.keepPlayingInGameMenu()),
                CommonComponents.optionStatus(draft.keepPlayingInGameMenu())
            ));
        }

        return List.copyOf(changes);
    }

    private static Component change(Component setting, Component appliedValue, Component draftValue) {
        return Component.translatable(
            CHANGE_KEY,
            setting.copy().withStyle(ChatFormatting.AQUA),
            appliedValue.copy().withStyle(ChatFormatting.GRAY),
            draftValue.copy().withStyle(ChatFormatting.YELLOW)
        ).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static Component feedbackModeLabel(CNowPlayingFeedbackMode mode) {
        return Component.translatable(switch (mode) {
            case CHAT -> "screen.custommusicclientsideplayer.mod_config.now_playing_feedback.chat";
            case OVERLAY -> "screen.custommusicclientsideplayer.mod_config.now_playing_feedback.overlay";
            case OFF -> "screen.custommusicclientsideplayer.mod_config.now_playing_feedback.off";
        });
    }

    private static Component percent(int value) {
        return Component.translatable(PERCENT_KEY, value);
    }
}
