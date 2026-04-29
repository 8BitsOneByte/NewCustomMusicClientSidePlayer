package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameRepository;

public final class CRenameTrackScreen extends Screen {
    private static final int INPUT_BOX_WIDTH = 260;
    private static final Component TITLE = Component.translatable("screen.custommusicclientsideplayer.rename_track.title");
    private static final Component NAME_LABEL = Component.translatable("screen.custommusicclientsideplayer.rename_track.name_label");
    private static final Component NAME_HINT = Component.translatable("screen.custommusicclientsideplayer.rename_track.hint");
    private static final Component RENAME_BUTTON_TEXT = Component.translatable("screen.custommusicclientsideplayer.rename_track.rename_button");
    private static final Component EMPTY_NAME_ERROR = Component.translatable("screen.custommusicclientsideplayer.error.track_name_empty");
    private static final Component DUPLICATE_NAME_ERROR = Component.translatable("screen.custommusicclientsideplayer.error.track_name_duplicate_same_namespace");
    private static final Component TOO_LONG_NAME_ERROR = Component.translatable("screen.custommusicclientsideplayer.error.track_name_too_long");

    private final Screen lastScreen;
    private final Identifier selectedTrack;
    private final Runnable onRenamed;
    private final Set<String> existingNamesLower = new HashSet<>();

    private EditBox nameEdit;
    private Button renameButton;

    public CRenameTrackScreen(Screen lastScreen, Identifier selectedTrack, List<Identifier> sameNamespaceTracks, Runnable onRenamed) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.selectedTrack = selectedTrack;
        this.onRenamed = onRenamed;

        for (Identifier trackId : sameNamespaceTracks) {
            if (trackId.equals(selectedTrack)) {
                continue;
            }

            this.existingNamesLower.add(CTrackNameRepository.getDisplayName(trackId).toLowerCase(Locale.ROOT));
        }
    }

    @Override
    protected void init() {
        this.nameEdit = new EditBox(this.font, this.width / 2 - INPUT_BOX_WIDTH / 2, 160, INPUT_BOX_WIDTH, 20, NAME_LABEL);
        this.nameEdit.setMaxLength(CTrackNameRepository.INPUT_MAX_LENGTH);
        this.nameEdit.setHint(NAME_HINT);
        this.nameEdit.setResponder(value -> this.updateValidationState());
        this.addRenderableWidget(this.nameEdit);

        this.renameButton = this.addRenderableWidget(
            Button.builder(RENAME_BUTTON_TEXT, button -> this.renameTrack())
                .bounds(this.width / 2 - 155, this.height - 28, 150, 20)
                .build()
        );

        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                .bounds(this.width / 2 + 5, this.height - 28, 150, 20)
                .build()
        );

        this.updateValidationState();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, 50, -1);
        guiGraphics.centeredText(this.font, NAME_LABEL, this.width / 2, 142, -1);
    }

    private void updateValidationState() {
        Component error = this.validateCurrentName();
        this.renameButton.active = error == null;
        this.renameButton.setTooltip(error == null ? null : Tooltip.create(error));
    }

    private Component validateCurrentName() {
        String candidate = this.nameEdit.getValue().trim();
        if (candidate.isEmpty()) {
            return EMPTY_NAME_ERROR;
        }

        if (this.existingNamesLower.contains(candidate.toLowerCase(Locale.ROOT))) {
            return DUPLICATE_NAME_ERROR;
        }

        if (candidate.length() > CTrackNameRepository.MAX_TRACK_NAME_LENGTH) {
            return TOO_LONG_NAME_ERROR;
        }

        return null;
    }

    private void renameTrack() {
        Component error = this.validateCurrentName();
        if (error != null) {
            this.updateValidationState();
            return;
        }

        try {
            CTrackNameRepository.renameTrack(this.selectedTrack, this.nameEdit.getValue());
            this.onRenamed.run();
        } catch (IOException exception) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.rename_track_failed"));
            }
        }

        this.minecraft.setScreen(this.lastScreen);
    }
}

