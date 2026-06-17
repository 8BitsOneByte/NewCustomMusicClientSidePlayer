package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.jspecify.annotations.Nullable;

public final class CSingleTrackPlayConfirmScreen extends Screen {
    private static final int ACTION_BUTTON_WIDTH = 200;
    private static final int SMALL_BUTTON_GAP = 4;
    private static final Component PITCH_HINT = Component.translatable("screen.newcustommusicclientsideplayer.singletrack.pitch_hint").withStyle(EditBox.SEARCH_HINT_STYLE);

    private final Screen lastScreen;
    private final PlayAction onStart;

    private LinearLayout layout = LinearLayout.vertical().spacing(8);
    private EditBox pitchBox;
    private Button loopOnButton;
    private Button loopOffButton;
    private Button startPlayingButton;
    private boolean loopEnabled = true;
    @Nullable
    private Component pitchError;

    @FunctionalInterface
    public interface PlayAction {
        void start(boolean loop, float pitch);
    }

    public CSingleTrackPlayConfirmScreen(Screen lastScreen, Identifier soundId, Consumer<Boolean> onConfirm) {
        this(lastScreen, soundId, (loop, pitch) -> onConfirm.accept(loop));
    }

    public CSingleTrackPlayConfirmScreen(Screen lastScreen, Identifier soundId, PlayAction onStart) {
        super(Component.translatable("screen.custommusicclientsideplayer.play_instance.title"));
        this.lastScreen = lastScreen;
        this.onStart = onStart;
    }

    @Override
    protected void init() {
        this.layout = LinearLayout.vertical().spacing(8);
        this.layout.defaultCellSetting().alignHorizontallyCenter();
        this.layout.addChild(new StringWidget(this.title, this.font));
        this.layout.addChild(new StringWidget(
            Component.translatable("screen.newcustommusicclientsideplayer.singletrack.pitch_label"),
            this.font
        ));

        this.pitchBox = this.layout.addChild(new EditBox(this.font, 0, 0, ACTION_BUTTON_WIDTH, 20, Component.empty()));
        this.pitchBox.setHint(PITCH_HINT);
        this.pitchBox.setMaxLength(8);
        this.pitchBox.setValue("");
        this.pitchBox.setResponder(value -> this.updateValidationState());

        LinearLayout loopRow = this.layout.addChild(LinearLayout.horizontal().spacing(SMALL_BUTTON_GAP));
        loopRow.defaultCellSetting().paddingTop(8);
        this.loopOnButton = loopRow.addChild(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.play_instance.loop_on"), button -> this.setLoopEnabled(true))
                .width((ACTION_BUTTON_WIDTH - SMALL_BUTTON_GAP) / 2)
                .build()
        );
        this.loopOffButton = loopRow.addChild(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.play_instance.loop_off"), button -> this.setLoopEnabled(false))
                .width((ACTION_BUTTON_WIDTH - SMALL_BUTTON_GAP) / 2)
                .build()
        );

        this.startPlayingButton = this.layout.addChild(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.play_instance.start_playing"), button -> this.startPlaying())
                .width(ACTION_BUTTON_WIDTH)
                .build()
        );
        this.layout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).width(ACTION_BUTTON_WIDTH).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.updateLoopButtonLabels();
        this.updateValidationState();
        this.repositionElements();
        this.setInitialFocus(this.pitchBox);
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        if (this.pitchError != null && this.pitchBox != null) {
            guiGraphics.text(this.font, this.pitchError, this.pitchBox.getX(), this.pitchBox.getY() + 24, 0xFFFF5555, false);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    private void setLoopEnabled(boolean loopEnabled) {
        this.loopEnabled = loopEnabled;
        this.updateLoopButtonLabels();
    }

    private void updateLoopButtonLabels() {
        if (this.loopOnButton != null) {
            this.loopOnButton.setMessage(
                Component.translatable("screen.custommusicclientsideplayer.play_instance.loop_on").withStyle(this.loopEnabled ? ChatFormatting.GRAY : ChatFormatting.WHITE)
            );
            this.loopOnButton.active = !this.loopEnabled;
        }

        if (this.loopOffButton != null) {
            this.loopOffButton.setMessage(
                Component.translatable("screen.custommusicclientsideplayer.play_instance.loop_off").withStyle(!this.loopEnabled ? ChatFormatting.GRAY : ChatFormatting.WHITE)
            );
            this.loopOffButton.active = this.loopEnabled;
        }
    }

    private void updateValidationState() {
        Component error = null;
        String rawValue = this.pitchBox == null ? "" : this.pitchBox.getValue().trim();

        if (!rawValue.isEmpty()) {
            try {
                float pitch = Float.parseFloat(rawValue);
                if (!Float.isFinite(pitch)) {
                    error = Component.translatable("screen.newcustommusicclientsideplayer.singletrack.pitch_invalid");
                } else if (pitch < 0.0F) {
                    error = Component.translatable("screen.newcustommusicclientsideplayer.singletrack.pitch_negative");
                }
            } catch (NumberFormatException ignored) {
                error = Component.translatable("screen.newcustommusicclientsideplayer.singletrack.pitch_invalid");
            }
        }

        this.pitchError = error;
        if (this.startPlayingButton != null) {
            this.startPlayingButton.active = error == null;
            this.startPlayingButton.setTooltip(error == null ? null : Tooltip.create(error));
        }
    }

    private float getResolvedPitch() {
        String rawValue = this.pitchBox == null ? "" : this.pitchBox.getValue().trim();
        if (rawValue.isEmpty()) {
            return CPlaySoundController.DEFAULT_PITCH;
        }

        try {
            return Float.parseFloat(rawValue);
        } catch (NumberFormatException ignored) {
            return CPlaySoundController.DEFAULT_PITCH;
        }
    }

    private void startPlaying() {
        this.updateValidationState();
        if (this.pitchError != null) {
            return;
        }

        this.onStart.start(this.loopEnabled, this.getResolvedPitch());
    }
}
