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
import org.exmple.newcustommusicclientsideplayer.client.storage.CPlaylistRepository;
import org.jspecify.annotations.Nullable;

public final class CCreatePlaylistScreen extends Screen {
    private static final int MAX_PLAYLIST_NAME_LENGTH = 50;
    private static final int INPUT_MAX_LENGTH = 60;
    private static final int INPUT_BOX_WIDTH = 260;
    private static final Component TITLE = Component.translatable("screen.custommusicclientsideplayer.create_playlist.title");
    private static final Component NAME_LABEL = Component.translatable("screen.custommusicclientsideplayer.playlist_name");
    private static final Component EMPTY_NAME_ERROR = Component.translatable("screen.custommusicclientsideplayer.error.playlist_name_empty");
    private static final Component DUPLICATE_NAME_ERROR = Component.translatable("screen.custommusicclientsideplayer.error.playlist_name_duplicate");
    private static final Component TOO_LONG_NAME_ERROR = Component.translatable("screen.custommusicclientsideplayer.error.playlist_name_too_long");
    private static final Component NAME_HINT = Component.translatable("screen.custommusicclientsideplayer.playlist_name_hint");

    private final Screen lastScreen;
    private final Runnable onCreated;
    private final Set<String> existingNamesLower = new HashSet<>();

    private EditBox nameEdit;
    private Button createButton;

    public CCreatePlaylistScreen(Screen lastScreen, Runnable onCreated) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.onCreated = onCreated;
    }

    @Override
    protected void init() {
        this.loadExistingNames();

        this.nameEdit = new EditBox(this.font, this.width / 2 - INPUT_BOX_WIDTH / 2, 160, INPUT_BOX_WIDTH, 20, NAME_LABEL);
        this.nameEdit.setMaxLength(INPUT_MAX_LENGTH);
        this.nameEdit.setHint(NAME_HINT);
        this.nameEdit.setResponder(name -> this.updateValidationState());
        this.addRenderableWidget(this.nameEdit);

        this.createButton = this.addRenderableWidget(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.create_playlist.create_button"), button -> this.createPlaylist())
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

    private void loadExistingNames() {
        this.existingNamesLower.clear();
        try {
            List<String> names = CPlaylistRepository.listPlaylistNames();
            for (String name : names) {
                this.existingNamesLower.add(name.toLowerCase(Locale.ROOT));
            }
        } catch (IOException ignored) {
        }
    }

    private void updateValidationState() {
        @Nullable Component error = this.validateCurrentName();
        this.createButton.active = error == null;
        this.createButton.setTooltip(error == null ? null : Tooltip.create(error));
    }

    @Nullable
    private Component validateCurrentName() {
        String candidate = this.nameEdit.getValue().trim();
        if (candidate.isEmpty()) {
            return EMPTY_NAME_ERROR;
        }

        if (this.existingNamesLower.contains(candidate.toLowerCase(Locale.ROOT))) {
            return DUPLICATE_NAME_ERROR;
        }

        if (candidate.length() > MAX_PLAYLIST_NAME_LENGTH) {
            return TOO_LONG_NAME_ERROR;
        }

        return null;
    }

    private void createPlaylist() {
        Component error = this.validateCurrentName();
        if (error != null) {
            this.updateValidationState();
            return;
        }

        String playlistName = this.nameEdit.getValue().trim();
        try {
            boolean created = CPlaylistRepository.createPlaylist(playlistName);
            if (!created) {
                this.createButton.active = false;
                this.createButton.setTooltip(Tooltip.create(DUPLICATE_NAME_ERROR));
                return;
            }
        } catch (IOException exception) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.create_playlist_failed"));
            }

            return;
        }

        this.onCreated.run();
        this.minecraft.setScreen(this.lastScreen);
    }
}
