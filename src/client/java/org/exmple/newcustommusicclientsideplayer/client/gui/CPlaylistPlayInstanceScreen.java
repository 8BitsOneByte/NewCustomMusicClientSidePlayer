package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameRepository;
import org.jspecify.annotations.Nullable;

public final class CPlaylistPlayInstanceScreen extends Screen {
    private static final int LIST_MIN_WIDTH = 220;
    private static final int LIST_ROW_HEIGHT = 36;
    private static final int PREVIEW_ICON_SIZE = 32;
    private static final int ACTION_BUTTON_MAX_WIDTH = 200;
    private static final int LIST_PADDING_LEFT = 8;
    private static final int RIGHT_PANEL_GAP = 20;
    private static final int PANEL_SAFE_GAP = 20;
    private static final int FOOTER_Y_OFFSET = 28;
    private static final int SMALL_BUTTON_GAP = 4;
    private static final Identifier MC_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/mc_mus_icon.png");
    private static final Identifier OTHER_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/other_mus_icon.png");
    private static final int COLOR_INCOMPATIBLE_ROW = -8978432;
    private static final Component UNKNOWN_NAMESPACE_TOOLTIP = Component.translatable("screen.custommusicclientsideplayer.common.unknown_namespace_track");
    private static final Component UNPLAYABLE_TRACK_TOOLTIP = Component.translatable("screen.custommusicclientsideplayer.common.unplayable_track");
    private static final Component START_INDEX_HINT = Component.translatable("screen.custommusicclientsideplayer.play_instance.start_index_hint").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final Tooltip SHUFFLE_REQUIRES_LOOP_TOOLTIP = Tooltip.create(Component.translatable("screen.newcustommusicclientsideplayer.play_instance.shuffle_requires_loop"));

    private final Screen parent;
    private final List<Identifier> tracks;
    private final StartAction onStart;
    private final Set<String> availableNamespaces = new HashSet<>();

    private TrackPreviewList trackPreviewList;
    private EditBox startIndexBox;
    private Button loopOnButton;
    private Button loopOffButton;
    private Button shuffleOnButton;
    private Button shuffleOffButton;
    private Button startPlayingButton;
    private Button cancelButton;
    private boolean loopEnabled = true;
    private boolean shuffleEnabled;
    private int previewLabelCenterX;
    private int previewLabelY;
    private int startLabelX;
    private int startLabelY;
    @Nullable
    private Component startIndexError;

    @FunctionalInterface
    public interface StartAction {
        int start(boolean loop, boolean shuffle, int startIndex);
    }

    public CPlaylistPlayInstanceScreen(Screen parent, String playlistName, List<Identifier> tracks, StartAction onStart) {
        super(Component.translatable("screen.custommusicclientsideplayer.play_instance.title"));
        this.parent = parent;
        this.tracks = List.copyOf(tracks);
        this.onStart = onStart;
    }

    @Override
    protected void init() {
        Set<String> namespaces = this.minecraft.getSoundManager().getAvailableSounds().stream().map(Identifier::getNamespace).collect(Collectors.toSet());
        this.availableNamespaces.clear();
        if (!namespaces.isEmpty()) {
            this.availableNamespaces.addAll(namespaces);
        }

        this.trackPreviewList = this.addRenderableWidget(new TrackPreviewList(this.minecraft, this, this.width, this.height - 90));

        this.startIndexBox = this.addRenderableWidget(new EditBox(this.font, 0, 0, ACTION_BUTTON_MAX_WIDTH, 20, Component.empty()));
        this.startIndexBox.setHint(START_INDEX_HINT);
        this.startIndexBox.setMaxLength(6);
        this.startIndexBox.setValue("");
        this.startIndexBox.setResponder(value -> this.updateValidationState());

        this.loopOnButton = this.addRenderableWidget(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.play_instance.loop_on"), button -> this.setLoopEnabled(true))
                .bounds(0, 0, (ACTION_BUTTON_MAX_WIDTH - SMALL_BUTTON_GAP) / 2, 20)
                .build()
        );
        this.loopOffButton = this.addRenderableWidget(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.play_instance.loop_off"), button -> this.setLoopEnabled(false))
                .bounds(0, 0, (ACTION_BUTTON_MAX_WIDTH - SMALL_BUTTON_GAP) / 2, 20)
                .build()
        );
        this.shuffleOnButton = this.addRenderableWidget(
            Button.builder(Component.translatable("screen.newcustommusicclientsideplayer.play_instance.shuffle_on"), button -> this.setShuffleEnabled(true))
                .bounds(0, 0, (ACTION_BUTTON_MAX_WIDTH - SMALL_BUTTON_GAP) / 2, 20)
                .build()
        );
        this.shuffleOffButton = this.addRenderableWidget(
            Button.builder(Component.translatable("screen.newcustommusicclientsideplayer.play_instance.shuffle_off"), button -> this.setShuffleEnabled(false))
                .bounds(0, 0, (ACTION_BUTTON_MAX_WIDTH - SMALL_BUTTON_GAP) / 2, 20)
                .build()
        );
        this.startPlayingButton = this.addRenderableWidget(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.play_instance.start_playing"), button -> this.startPlaying())
                .bounds(0, 0, ACTION_BUTTON_MAX_WIDTH, 20)
                .build()
        );
        this.cancelButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).bounds(0, 0, ACTION_BUTTON_MAX_WIDTH, 20).build());

        this.trackPreviewList.setTracks(this.tracks);
        this.updateLoopButtonLabels();
        this.updateShuffleButtonLabels();
        this.updateValidationState();
        this.repositionElements();
        this.setInitialFocus(this.startIndexBox);
    }

    @Override
    protected void repositionElements() {
        int contentTop = 46;
        int listX = LIST_PADDING_LEFT;
        int listY = contentTop + 18;
        int rightX = this.width / 2 + RIGHT_PANEL_GAP;
        int rightY = contentTop + 18;
        int listWidth = Math.max(LIST_MIN_WIDTH, rightX - listX - PANEL_SAFE_GAP);
        int listHeight = Math.max(60, this.height - listY - FOOTER_Y_OFFSET - 8);
        int rightAvailableWidth = this.width - rightX - LIST_PADDING_LEFT;
        int rightPanelWidth = Math.min(ACTION_BUTTON_MAX_WIDTH, Math.max(110, rightAvailableWidth));
        int smallButtonWidth = (rightPanelWidth - SMALL_BUTTON_GAP) / 2;

        this.previewLabelCenterX = listX + listWidth / 2;
        this.previewLabelY = listY - 12;
        this.startLabelX = rightX;
        this.startLabelY = listY - 12;

        if (this.trackPreviewList != null) {
            this.trackPreviewList.updateSizeAndPosition(listWidth, listHeight, listX, listY);
        }

        if (this.startIndexBox != null) {
            this.startIndexBox.setX(rightX);
            this.startIndexBox.setY(rightY + 2);
            this.startIndexBox.setWidth(rightPanelWidth);
        }

        if (this.loopOnButton != null) {
            this.loopOnButton.setX(rightX);
            this.loopOnButton.setY(rightY + 32);
            this.loopOnButton.setWidth(smallButtonWidth);
        }

        if (this.loopOffButton != null) {
            this.loopOffButton.setX(rightX + smallButtonWidth + SMALL_BUTTON_GAP);
            this.loopOffButton.setY(rightY + 32);
            this.loopOffButton.setWidth(smallButtonWidth);
        }

        if (this.shuffleOnButton != null) {
            this.shuffleOnButton.setX(rightX);
            this.shuffleOnButton.setY(rightY + 62);
            this.shuffleOnButton.setWidth(smallButtonWidth);
        }

        if (this.shuffleOffButton != null) {
            this.shuffleOffButton.setX(rightX + smallButtonWidth + SMALL_BUTTON_GAP);
            this.shuffleOffButton.setY(rightY + 62);
            this.shuffleOffButton.setWidth(smallButtonWidth);
        }

        if (this.startPlayingButton != null) {
            this.startPlayingButton.setX(rightX);
            this.startPlayingButton.setY(rightY + 92);
            this.startPlayingButton.setWidth(rightPanelWidth);
        }

        if (this.cancelButton != null) {
            this.cancelButton.setX(this.width / 2 - ACTION_BUTTON_MAX_WIDTH / 2);
            this.cancelButton.setY(this.height - FOOTER_Y_OFFSET);
            this.cancelButton.setWidth(ACTION_BUTTON_MAX_WIDTH);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font, Component.translatable("screen.custommusicclientsideplayer.play_instance.preview"), this.previewLabelCenterX, this.previewLabelY, 0xFFFFFFFF);
        guiGraphics.text(this.font, Component.translatable("screen.custommusicclientsideplayer.play_instance.start_index_label"), this.startLabelX, this.startLabelY, 0xFFFFFFFF, false);

        if (this.startIndexError != null && this.startIndexBox != null) {
            guiGraphics.text(this.font, this.startIndexError, this.startIndexBox.getX(), this.startIndexBox.getY() + 24, 0xFFFF5555, false);
        }
    }

    private void setLoopEnabled(boolean loopEnabled) {
        this.loopEnabled = loopEnabled;
        if (!loopEnabled) {
            this.shuffleEnabled = false;
            this.clearStartIndexInput();
        }
        this.playUiClickSound();
        this.updateLoopButtonLabels();
        this.updateShuffleButtonLabels();
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

    private void setShuffleEnabled(boolean shuffleEnabled) {
        if (!this.loopEnabled) {
            return;
        }

        this.shuffleEnabled = shuffleEnabled;
        if (shuffleEnabled) {
            fillRandomStartIndexIfEmpty();
        } else {
            clearStartIndexInput();
        }

        this.playUiClickSound();
        this.updateShuffleButtonLabels();
        this.updateValidationState();
    }

    private void updateShuffleButtonLabels() {
        boolean shuffleControlsEnabled = this.loopEnabled;
        if (this.shuffleOnButton != null) {
            ChatFormatting style = !shuffleControlsEnabled || this.shuffleEnabled ? ChatFormatting.GRAY : ChatFormatting.WHITE;
            this.shuffleOnButton.setMessage(
                Component.translatable("screen.newcustommusicclientsideplayer.play_instance.shuffle_on")
                    .withStyle(style)
            );
            this.shuffleOnButton.active = shuffleControlsEnabled && !this.shuffleEnabled;
            this.shuffleOnButton.setTooltip(shuffleControlsEnabled ? null : SHUFFLE_REQUIRES_LOOP_TOOLTIP);
        }

        if (this.shuffleOffButton != null) {
            ChatFormatting style = !shuffleControlsEnabled || !this.shuffleEnabled ? ChatFormatting.GRAY : ChatFormatting.WHITE;
            this.shuffleOffButton.setMessage(
                Component.translatable("screen.newcustommusicclientsideplayer.play_instance.shuffle_off")
                    .withStyle(style)
            );
            this.shuffleOffButton.active = shuffleControlsEnabled && this.shuffleEnabled;
            this.shuffleOffButton.setTooltip(shuffleControlsEnabled ? null : SHUFFLE_REQUIRES_LOOP_TOOLTIP);
        }
    }

    private void fillRandomStartIndexIfEmpty() {
        if (this.startIndexBox == null || !this.startIndexBox.getValue().trim().isEmpty() || this.tracks.isEmpty()) {
            return;
        }

        this.startIndexBox.setValue(String.valueOf(ThreadLocalRandom.current().nextInt(this.tracks.size()) + 1));
    }

    private void clearStartIndexInput() {
        if (this.startIndexBox != null) {
            this.startIndexBox.setValue("");
        }
    }

    private void updateValidationState() {
        Component error = null;
        String rawValue = this.startIndexBox == null ? "" : this.startIndexBox.getValue().trim();

        if (this.tracks.isEmpty()) {
            error = Component.translatable("message.custommusicclientsideplayer.playlist_empty");
        } else if (!rawValue.isEmpty()) {
            int startIndex = this.getResolvedStartIndex();
            if (startIndex < 1) {
                error = Component.translatable("screen.custommusicclientsideplayer.play_instance.start_index_invalid");
            } else if (startIndex > this.tracks.size()) {
                error = Component.translatable("screen.custommusicclientsideplayer.play_instance.start_index_too_large", this.tracks.size());
            }
        }

        this.startIndexError = error;
        if (this.startPlayingButton != null) {
            this.startPlayingButton.active = error == null;
            this.startPlayingButton.setTooltip(error == null ? null : Tooltip.create(error));
        }
    }

    private int getResolvedStartIndex() {
        String raw = this.startIndexBox == null ? "" : this.startIndexBox.getValue().trim();
        if (raw.isEmpty()) {
            return 1;
        }

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void startPlaying() {
        int startIndex = this.getResolvedStartIndex();
        if (startIndex < 1 || startIndex > this.tracks.size()) {
            this.updateValidationState();
            return;
        }

        if (this.onStart.start(this.loopEnabled, this.shuffleEnabled, startIndex) > 0) {
            this.minecraft.setScreen(null);
        }
    }

    private void playUiClickSound() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static String getDisplayName(Identifier id) {
        return CTrackNameRepository.getDisplayName(id);
    }

    private static Component buildDescription(Identifier id) {
        return Component.translatable("screen.custommusicclientsideplayer.common.music_files_from", Component.literal(id.getNamespace()).withStyle(style -> style.withBold(true)));
    }

    private static final class TrackPreviewList extends ObjectSelectionList<TrackPreviewList.Entry> {
        private final CPlaylistPlayInstanceScreen screen;

        private TrackPreviewList(Minecraft minecraft, CPlaylistPlayInstanceScreen screen, int width, int height) {
            super(minecraft, width, height, 0, LIST_ROW_HEIGHT);
            this.screen = screen;
            this.centerListVertically = false;
        }

        @Override
        public void updateSizeAndPosition(int width, int height, int x, int y) {
            super.updateSizeAndPosition(width, height, x, y);
        }

        @Override
        public int getRowWidth() {
            return Math.max(120, this.getWidth() - 12);
        }

        @Override
        protected int scrollBarX() {
            return this.getX() + this.getWidth() - 6;
        }

        private void setTracks(List<Identifier> tracks) {
            this.clearEntries();
            if (tracks.isEmpty()) {
                this.addEntry(new EmptyEntry(this.screen));
            } else {
                int index = 1;
                for (Identifier track : tracks) {
                    this.addEntry(new TrackEntry(this.screen, track, index++));
                }
            }

            this.setSelected(null);
            this.setScrollAmount(0.0);
        }

        private abstract static class Entry extends ObjectSelectionList.Entry<Entry> {
        }

        private static final class EmptyEntry extends Entry {
            private final CPlaylistPlayInstanceScreen screen;

            private EmptyEntry(CPlaylistPlayInstanceScreen screen) {
                this.screen = screen;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
                guiGraphics.centeredText(this.screen.font, Component.translatable("screen.custommusicclientsideplayer.view_tracks.empty"), this.getX() + this.getWidth() / 2, this.getContentYMiddle() - 4, 0xFF808080);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("screen.custommusicclientsideplayer.view_tracks.empty_narration");
            }
        }

        private static final class TrackEntry extends Entry {
            private final CPlaylistPlayInstanceScreen screen;
            private final Identifier track;
            private final int order;

            private TrackEntry(CPlaylistPlayInstanceScreen screen, Identifier track, int order) {
                this.screen = screen;
                this.track = track;
                this.order = order;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
                Font font = this.screen.font;
                boolean missingNamespace = !this.screen.availableNamespaces.contains(this.track.getNamespace());
                boolean unplayableTrack = !CPlaySoundController.isTrackPlayable(this.screen.minecraft, this.track);
                if (missingNamespace || unplayableTrack) {
                    guiGraphics.fill(this.getContentX() - 1, this.getContentY() - 1, this.getContentRight() + 1, this.getContentBottom() + 1, COLOR_INCOMPATIBLE_ROW);
                }

                Identifier icon = this.track.getNamespace().equals("minecraft") ? MC_MUS_ICON : OTHER_MUS_ICON;
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, this.getContentX(), this.getContentY(), 0.0F, 0.0F, PREVIEW_ICON_SIZE, PREVIEW_ICON_SIZE, PREVIEW_ICON_SIZE, PREVIEW_ICON_SIZE);

                int textX = this.getContentX() + 34;
                guiGraphics.text(font, Component.translatable("screen.custommusicclientsideplayer.view_tracks.track_order", this.order, CPlaylistPlayInstanceScreen.getDisplayName(this.track)), textX, this.getContentY() + 1, 0xFFFFFFFF, false);
                guiGraphics.text(font, CPlaylistPlayInstanceScreen.buildDescription(this.track), textX, this.getContentY() + 12, 0xFF808080, false);

                if (hovered) {
                    if (missingNamespace) {
                        guiGraphics.setTooltipForNextFrame(UNKNOWN_NAMESPACE_TOOLTIP, mouseX, mouseY);
                    } else if (unplayableTrack) {
                        guiGraphics.setTooltipForNextFrame(UNPLAYABLE_TRACK_TOOLTIP, mouseX, mouseY);
                    }
                }
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.track.toString());
            }
        }
    }
}










