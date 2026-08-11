package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Reuses the import-warning presentation and its twelve-line scrolling threshold while protecting
 * against an accidental rapid double-Esc. Caller-provided components remain style-aware so later
 * configuration details can use the same panel without changing this screen's navigation contract.
 */
public final class CDiscardChangesWarningScreen extends CImportMessageScreen {
    private static final Component KEEP_EDITING =
        Component.translatable("screen.custommusicclientsideplayer.unsaved_changes.keep_editing");
    private static final Component DISCARD_CHANGES =
        Component.translatable("screen.custommusicclientsideplayer.unsaved_changes.discard");
    private static final Component CHANGES_TO_DISCARD =
        Component.translatable("screen.custommusicclientsideplayer.unsaved_changes.list_title")
            .withStyle(ChatFormatting.GRAY);
    private static final String ESCAPE_COUNTDOWN_MULTIPLE_KEY =
        "screen.custommusicclientsideplayer.unsaved_changes.escape_countdown.multiple";
    private static final String ESCAPE_COUNTDOWN_ONE_KEY =
        "screen.custommusicclientsideplayer.unsaved_changes.escape_countdown.one";
    private static final String ESCAPE_READY_KEY =
        "screen.custommusicclientsideplayer.unsaved_changes.escape_ready";
    private static final Component ESCAPE_KEY =
        Component.literal("Esc").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);

    private static final long ESCAPE_DISCARD_DELAY_NANOS = 3_000_000_000L;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final Runnable discardAction;
    private final long escapeDiscardUnlockAtNanos;
    private int displayedRemainingSeconds = Integer.MIN_VALUE;
    private Component countdown = Component.empty();

    public CDiscardChangesWarningScreen(
        Screen editScreen,
        Component title,
        Component body,
        List<Component> discardedChanges,
        Runnable discardAction
    ) {
        super(
            Objects.requireNonNull(editScreen, "editScreen"),
            title,
            body,
            discardedChanges.isEmpty() ? null : CHANGES_TO_DISCARD,
            discardedChanges,
            KEEP_EDITING,
            () -> returnToEditScreen(editScreen),
            DISCARD_CHANGES,
            discardAction
        );
        this.discardAction = Objects.requireNonNull(discardAction, "discardAction");
        this.escapeDiscardUnlockAtNanos = System.nanoTime() + ESCAPE_DISCARD_DELAY_NANOS;
    }

    @Override
    public void onClose() {
        if (this.isEscapeDiscardUnlocked()) {
            this.discardAction.run();
        }
    }

    @Nullable
    @Override
    protected Component subtitle() {
        int remainingSeconds = this.remainingSeconds();
        if (remainingSeconds != this.displayedRemainingSeconds) {
            this.displayedRemainingSeconds = remainingSeconds;
            Component message = switch (remainingSeconds) {
                case 0 -> Component.translatable(ESCAPE_READY_KEY, ESCAPE_KEY)
                    .withStyle(ChatFormatting.YELLOW);
                case 1 -> countdownMessage(ESCAPE_COUNTDOWN_ONE_KEY, 1);
                default -> countdownMessage(ESCAPE_COUNTDOWN_MULTIPLE_KEY, remainingSeconds);
            };
            this.countdown = Component.empty()
                .append(Component.literal("(").withStyle(ChatFormatting.GRAY))
                .append(message)
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
        }
        return this.countdown;
    }

    @Override
    protected boolean focusPrimaryButtonOnInit() {
        return true;
    }

    private boolean isEscapeDiscardUnlocked() {
        return System.nanoTime() >= this.escapeDiscardUnlockAtNanos;
    }

    private int remainingSeconds() {
        long remainingNanos = this.escapeDiscardUnlockAtNanos - System.nanoTime();
        if (remainingNanos <= 0L) {
            return 0;
        }

        return (int) Math.min(
            Integer.MAX_VALUE,
            (remainingNanos + NANOS_PER_SECOND - 1L) / NANOS_PER_SECOND
        );
    }

    private static Component countdownMessage(String translationKey, int remainingSeconds) {
        Component seconds = Component.literal(Integer.toString(remainingSeconds))
            .withStyle(ChatFormatting.YELLOW);
        return Component.translatable(translationKey, seconds, ESCAPE_KEY);
    }

    private static void returnToEditScreen(Screen editScreen) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setScreen(editScreen);
    }
}
