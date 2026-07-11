package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.exmple.newcustommusicclientsideplayer.client.storage.CPlaylistImportException;
import org.exmple.newcustommusicclientsideplayer.client.storage.CPlaylistImportPreview;
import org.exmple.newcustommusicclientsideplayer.client.storage.CPlaylistRepository;
import org.exmple.newcustommusicclientsideplayer.client.storage.CPlaylistTransferService;

public class CPlaylistConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.custommusicclientsideplayer.config_playlists.title");
    private static final Component SEARCH_HINT = Component.translatable("screen.custommusicclientsideplayer.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final Component IMPORT_PLAYLISTS_TOOLTIP = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_tooltip");
    private static final Component EXPORT_PLAYLISTS_TOOLTIP = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.export_tooltip");
    private static final Component MUST_DISABLE_FULLSCREEN = Component.translatable("screen.custommusicclientsideplayer.mod_config.must_disable_fullscreen");
    private static final Component IMPORT_DIALOG_TITLE = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_dialog_title");
    private static final Component IMPORT_DIALOG_FILTER = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_dialog_filter");
    private static final Component EXPORT_DIALOG_TITLE = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.export_dialog_title");
    private static final Component EXPORT_DIALOG_FILTER = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.export_dialog_filter");
    private static final Component IMPORT_SUCCESS_TOAST_TITLE = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.toast.import_success.title");
    private static final String IMPORT_SUCCESS_TOAST_DESCRIPTION_KEY = "screen.custommusicclientsideplayer.playlist_transfer.toast.import_success.description";
    private static final Component EXPORT_SUCCESS_TOAST_TITLE = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.toast.export_success.title");
    private static final String EXPORT_SUCCESS_TOAST_DESCRIPTION_KEY = "screen.custommusicclientsideplayer.playlist_transfer.toast.export_success.description";
    private static final Component EXPORT_FAILED_TOAST_TITLE = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.toast.export_failed.title");
    private static final String EXPORT_FAILED_TOAST_DESCRIPTION_KEY = "screen.custommusicclientsideplayer.playlist_transfer.toast.export_failed.description";
    private static final String EXPORT_DIALOG_FAILED_TOAST_DESCRIPTION_KEY = "screen.custommusicclientsideplayer.playlist_transfer.toast.export_dialog_failed.description";
    private static final Component IMPORT_BACK = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_back");
    private static final Component IMPORT_CANCEL = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_cancel");
    private static final Component IMPORT_IO_ERROR_TITLE = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_io_error.title");
    private static final Component IMPORT_IO_ERROR_BODY = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_io_error.body");
    private static final Component IMPORT_INVALID_TITLE = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_invalid.title");
    private static final Component IMPORT_INVALID_BODY = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_invalid.body");
    private static final Component IMPORT_NO_COMPATIBLE_TITLE = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_no_compatible.title");
    private static final Component IMPORT_NO_COMPATIBLE_BODY = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_no_compatible.body");
    private static final Component IMPORT_WARNING_TITLE = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_warning.title");
    private static final Component IMPORT_WARNING_BODY = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_warning.body");
    private static final Component IMPORT_WARNING_CONFIRM = Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.import_warning.confirm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm");
    private static final int ICON_SIZE = 32;
    private static final int TRANSFER_BUTTON_SIZE = 20;
    private static final int TRANSFER_BUTTON_OFFSET = 8;
    private static final int HEADER_ROW_SPACING = 4;
    private static final int TRANSFER_BUTTON_BALANCE_WIDTH = TRANSFER_BUTTON_OFFSET + TRANSFER_BUTTON_SIZE * 2 + HEADER_ROW_SPACING * 2;
    private static final int SMALL_BUTTON_WIDTH = 71;
    private static final Identifier LIST_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/listicon.png");
    private static final Identifier IMPORT_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/import.png");
    private static final Identifier EXPORT_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/export.png");
    private static final Identifier VIEW_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/join_highlighted");
    private static final Identifier VIEW_SPRITE = Identifier.withDefaultNamespace("world_list/join");
    private static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_up_highlighted");
    private static final Identifier MOVE_UP_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_up");
    private static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_down_highlighted");
    private static final Identifier MOVE_DOWN_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_down");
    private static final int VIEW_RENDER_X_OFFSET = -2;
    private static final int MOVE_RENDER_X_OFFSET = 2;
    private static final int VIEW_MIN_X = 8;
    private static final int VIEW_MAX_X_EXCLUSIVE = 22;
    private static final int VIEW_MIN_Y = 5;
    private static final int VIEW_MAX_Y_EXCLUSIVE = 27;
    private static final int MOVE_UP_MIN_X = 20;
    private static final int MOVE_UP_MAX_X_EXCLUSIVE = 31;
    private static final int MOVE_UP_MIN_Y = 5;
    private static final int MOVE_UP_MAX_Y_EXCLUSIVE = 12;
    private static final int MOVE_DOWN_MIN_X = 20;
    private static final int MOVE_DOWN_MAX_X_EXCLUSIVE = 31;
    private static final int MOVE_DOWN_MIN_Y = 20;
    private static final int MOVE_DOWN_MAX_Y_EXCLUSIVE = 27;

    private final Screen parent;
    private final CPlaylistTransferService transferService = new CPlaylistTransferService();
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 8 + 9 + 8 + 20 + 4, 60);
    private PlaylistSelectionList playlistSelectionList;
    private EditBox searchBox;
    private String filter = "";

    private Button importButton;
    private Button exportButton;
    private Button playSelectedListButton;
    private Button editButton;
    private Button deleteButton;
    private Button recreateButton;

    private List<CPlaylistRepository.PlaylistSummary> allSummaries = List.of();

    public CPlaylistConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title, this.font));
        LinearLayout searchRow = header.addChild(LinearLayout.horizontal().spacing(HEADER_ROW_SPACING));
        searchRow.addChild(SpacerElement.width(TRANSFER_BUTTON_BALANCE_WIDTH));
        this.searchBox = searchRow.addChild(
            new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, this.searchBox, Component.translatable("screen.custommusicclientsideplayer.search"))
        );
        this.searchBox.setHint(SEARCH_HINT);
        this.searchBox.setValue(this.filter);
        this.searchBox.setResponder(value -> {
            this.filter = value;
            this.refreshVisibleEntries();
        });
        searchRow.addChild(SpacerElement.width(TRANSFER_BUTTON_OFFSET));
        this.importButton = searchRow.addChild(
            Button.builder(Component.empty(), button -> this.onImportPlaylists())
                .width(TRANSFER_BUTTON_SIZE)
                .tooltip(Tooltip.create(IMPORT_PLAYLISTS_TOOLTIP))
                .build()
        );
        this.exportButton = searchRow.addChild(
            Button.builder(Component.empty(), button -> this.onExportPlaylists())
                .width(TRANSFER_BUTTON_SIZE)
                .tooltip(Tooltip.create(EXPORT_PLAYLISTS_TOOLTIP))
                .build()
        );

        this.playlistSelectionList = this.layout.addToContents(new PlaylistSelectionList(this.minecraft, this, this.width, this.layout.getContentHeight()));

        GridLayout footer = this.layout.addToFooter(new GridLayout().columnSpacing(8).rowSpacing(4));
        footer.defaultCellSetting().alignHorizontallyCenter();
        GridLayout.RowHelper rowHelper = footer.createRowHelper(4);
        this.playSelectedListButton = rowHelper.addChild(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.config_playlists.play_selected"), button -> this.onPlaySelected()).build(),
            2
        );
        rowHelper.addChild(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.config_playlists.create_new"), button -> this.onCreateNew()).build(),
            2
        );
        this.editButton = rowHelper.addChild(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.config_playlists.edit"), button -> this.onEditSelected()).width(SMALL_BUTTON_WIDTH).build()
        );
        this.deleteButton = rowHelper.addChild(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.config_playlists.delete"), button -> this.onDeleteSelected()).width(SMALL_BUTTON_WIDTH).build()
        );
        this.recreateButton = rowHelper.addChild(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.config_playlists.recreate"), button -> this.onRecreateSelected()).width(SMALL_BUTTON_WIDTH).build()
        );
        rowHelper.addChild(
            Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).width(SMALL_BUTTON_WIDTH).build()
        );

        this.layout.visitWidgets(this::addRenderableWidget);

        this.repositionElements();

        this.reloadEntries();
        this.updateButtonStates();
    }

    @Override
    protected void setInitialFocus() {
        if (this.searchBox != null) {
            this.setInitialFocus(this.searchBox);
        } else {
            super.setInitialFocus();
        }
    }

    @Override
    protected void repositionElements() {
        if (this.playlistSelectionList != null) {
            this.playlistSelectionList.updateSize(this.width, this.layout);
        }

        this.layout.arrangeElements();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.updateButtonStates();
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        this.extractTransferButtonIcon(guiGraphics, this.importButton, IMPORT_ICON);
        this.extractTransferButtonIcon(guiGraphics, this.exportButton, EXPORT_ICON);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private void reloadEntries() {
        try {
            this.allSummaries = CPlaylistRepository.listPlaylistSummaries();
            this.refreshVisibleEntries();
        } catch (IOException exception) {
            this.allSummaries = List.of();
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.read_playlists_failed"));
            }
            this.playlistSelectionList.setEntries(List.of());
        }
    }

    private void refreshVisibleEntries() {
        if (this.playlistSelectionList == null) {
            return;
        }

        String currentFilter = this.filter.toLowerCase(Locale.ROOT);
        List<CPlaylistRepository.PlaylistSummary> visible = this.allSummaries
            .stream()
            .filter(summary -> currentFilter.isBlank() || summary.name().toLowerCase(Locale.ROOT).contains(currentFilter))
            .toList();
        this.playlistSelectionList.setEntries(visible);
        this.updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = this.getSelectedSummary() != null;
        boolean fullscreen = this.minecraft != null && this.minecraft.getWindow().isFullscreen();
        Tooltip fullscreenTooltip = fullscreen ? Tooltip.create(MUST_DISABLE_FULLSCREEN) : null;
        if (this.importButton != null) {
            this.importButton.active = !fullscreen;
            this.importButton.setTooltip(fullscreen ? fullscreenTooltip : Tooltip.create(IMPORT_PLAYLISTS_TOOLTIP));
        }

        if (this.exportButton != null) {
            this.exportButton.active = !fullscreen;
            this.exportButton.setTooltip(fullscreen ? fullscreenTooltip : Tooltip.create(EXPORT_PLAYLISTS_TOOLTIP));
        }

        if (this.playSelectedListButton != null) {
            this.playSelectedListButton.active = hasSelection;
        }

        if (this.editButton != null) {
            this.editButton.active = hasSelection;
        }

        if (this.deleteButton != null) {
            this.deleteButton.active = hasSelection;
        }

        if (this.recreateButton != null) {
            this.recreateButton.active = hasSelection;
        }
    }

    private void extractTransferButtonIcon(GuiGraphicsExtractor guiGraphics, Button button, Identifier icon) {
        if (button == null || !button.visible) {
            return;
        }

        int iconSize = 16;
        int iconX = button.getX() + (button.getWidth() - iconSize) / 2;
        int iconY = button.getY() + (button.getHeight() - iconSize) / 2;
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            icon,
            iconX,
            iconY,
            0.0F,
            0.0F,
            iconSize,
            iconSize,
            32,
            32,
            32,
            32
        );
        if (!button.active) {
            guiGraphics.fill(
                iconX,
                iconY,
                iconX + iconSize,
                iconY + iconSize,
                0x99000000
            );
        }
    }

    private CPlaylistRepository.PlaylistSummary getSelectedSummary() {
        if (this.playlistSelectionList == null) {
            return null;
        }

        PlaylistSelectionList.Entry selected = this.playlistSelectionList.getSelected();
        if (selected instanceof PlaylistSelectionList.PlaylistEntry playlistEntry) {
            return playlistEntry.summary;
        }

        return null;
    }

    private void onImportPlaylists() {
        if (this.minecraft == null || this.minecraft.getWindow().isFullscreen()) {
            return;
        }

        CFileDialogUtil.fileSelectDialog(
            CFileDialogUtil.DialogType.OPEN,
            IMPORT_DIALOG_TITLE.getString(),
            null,
            IMPORT_DIALOG_FILTER.getString(),
            "*.txt"
        ).whenComplete((selectedPath, throwable) -> this.minecraft.execute(() -> {
            if (this.minecraft.gui.screen() != this) {
                return;
            }

            if (throwable != null) {
                this.openImportMessageScreen(IMPORT_IO_ERROR_TITLE, IMPORT_IO_ERROR_BODY);
                return;
            }

            selectedPath.ifPresent(this::handleImportPath);
        }));
    }

    private void handleImportPath(Path path) {
        CPlaylistImportPreview preview;
        try {
            preview = this.transferService.previewImport(path);
        } catch (CPlaylistImportException exception) {
            this.openImportExceptionScreen(exception);
            return;
        }

        if (preview.hasWarnings()) {
            this.openImportWarningScreen(preview);
            return;
        }

        this.applyImportPreview(preview);
    }

    private boolean applyImportPreview(CPlaylistImportPreview preview) {
        try {
            CPlaylistRepository.ImportResult result = this.transferService.importPreview(preview);
            this.reloadEntries();
            this.showImportSuccess(result.importedCount());
            return true;
        } catch (IOException | RuntimeException exception) {
            this.openImportMessageScreen(IMPORT_IO_ERROR_TITLE, IMPORT_IO_ERROR_BODY);
            return false;
        }
    }

    private void onExportPlaylists() {
        if (this.minecraft == null || this.minecraft.getWindow().isFullscreen()) {
            return;
        }

        CFileDialogUtil.fileSelectDialog(
            CFileDialogUtil.DialogType.SAVE,
            EXPORT_DIALOG_TITLE.getString(),
            Path.of(this.transferService.defaultFileName()),
            EXPORT_DIALOG_FILTER.getString(),
            "*.txt"
        ).whenComplete((selectedPath, throwable) -> this.minecraft.execute(() -> {
            if (this.minecraft.gui.screen() != this) {
                return;
            }

            if (throwable != null) {
                this.showExportDialogError();
                return;
            }

            selectedPath.ifPresent(this::handleExportPath);
        }));
    }

    private void handleExportPath(Path path) {
        try {
            this.transferService.exportPlaylists(path);
        } catch (IOException | RuntimeException exception) {
            this.showExportError(path);
            return;
        }

        this.showExportSuccess(path);
        this.updateButtonStates();
    }

    private void openImportExceptionScreen(CPlaylistImportException exception) {
        switch (exception.reason()) {
            case IO_ERROR -> this.openImportMessageScreen(IMPORT_IO_ERROR_TITLE, IMPORT_IO_ERROR_BODY);
            case INVALID_PLAYLIST_FILE -> this.openImportMessageScreen(IMPORT_INVALID_TITLE, IMPORT_INVALID_BODY);
            case NO_COMPATIBLE_PLAYLISTS -> this.openImportMessageScreen(
                IMPORT_NO_COMPATIBLE_TITLE,
                IMPORT_NO_COMPATIBLE_BODY
            );
        }
    }

    private void openImportMessageScreen(Component title, Component body) {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(new CImportMessageScreen(
                this,
                title,
                body,
                null,
                List.of(),
                IMPORT_BACK,
                () -> this.minecraft.gui.setScreen(this),
                null,
                null
            ));
        }
    }

    private void openImportWarningScreen(CPlaylistImportPreview preview) {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(new CImportMessageScreen(
                this,
                IMPORT_WARNING_TITLE,
                IMPORT_WARNING_BODY,
                null,
                CPlaylistImportWarningFormatter.format(preview),
                IMPORT_CANCEL,
                () -> this.minecraft.gui.setScreen(this),
                IMPORT_WARNING_CONFIRM,
                () -> {
                    if (this.applyImportPreview(preview)) {
                        this.minecraft.gui.setScreen(this);
                    }
                }
            ));
        }
    }

    private void showImportSuccess(int importedCount) {
        if (this.minecraft != null) {
            CTransferToast.show(
                this.minecraft,
                CTransferToast.Kind.SUCCESS,
                IMPORT_SUCCESS_TOAST_TITLE,
                Component.translatable(IMPORT_SUCCESS_TOAST_DESCRIPTION_KEY, importedCount)
            );
        }
    }

    private void showExportSuccess(Path path) {
        if (this.minecraft != null) {
            CTransferToast.show(
                this.minecraft,
                CTransferToast.Kind.SUCCESS,
                EXPORT_SUCCESS_TOAST_TITLE,
                Component.translatable(EXPORT_SUCCESS_TOAST_DESCRIPTION_KEY, displayPath(path))
            );
        }
    }

    private void showExportError(Path path) {
        if (this.minecraft != null) {
            CTransferToast.show(
                this.minecraft,
                CTransferToast.Kind.FAILURE,
                EXPORT_FAILED_TOAST_TITLE,
                Component.translatable(EXPORT_FAILED_TOAST_DESCRIPTION_KEY, displayPath(path))
            );
        }
        this.updateButtonStates();
    }

    private void showExportDialogError() {
        if (this.minecraft != null) {
            CTransferToast.show(
                this.minecraft,
                CTransferToast.Kind.FAILURE,
                EXPORT_FAILED_TOAST_TITLE,
                Component.translatable(EXPORT_DIALOG_FAILED_TOAST_DESCRIPTION_KEY)
            );
        }
        this.updateButtonStates();
    }

    private static String displayPath(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private void onPlaySelected() {
        CPlaylistRepository.PlaylistSummary selectedSummary = this.getSelectedSummary();
        if (selectedSummary == null || this.minecraft == null) {
            return;
        }

        String playlistName = selectedSummary.name();
        List<Identifier> playlist;
        try {
            playlist = CPlaylistRepository.getPlaylist(playlistName);
        } catch (IOException exception) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.read_playlist_failed"));
            }

            return;
        }

        this.minecraft.gui.setScreen(
            new CPlaylistPlayInstanceScreen(
                this,
                playlistName,
                playlist,
                (loop, shuffle, startIndex) -> this.playSelectedPlaylist(playlistName, playlist, loop, shuffle, startIndex)
            )
        );
    }

    private int playSelectedPlaylist(String playlistName, List<Identifier> playlist, boolean loop, boolean shuffle, int startTrackIndex) {
        return CPlaySoundController.playPlaylistFromUi(this.minecraft, playlistName, playlist, loop, shuffle, startTrackIndex - 1);
    }

    private void onEditSelected() {
        CPlaylistRepository.PlaylistSummary selectedSummary = this.getSelectedSummary();
        if (selectedSummary == null || this.minecraft == null) {
            return;
        }

        String playlistName = selectedSummary.name();
        List<Identifier> initialPlaylist;
        try {
            initialPlaylist = CPlaylistRepository.getPlaylist(playlistName);
        } catch (IOException exception) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.open_playlist_failed"));
            }

            return;
        }

        this.minecraft.gui.setScreen(
            new CPlaylistTestScreen(
                this,
                playlistName,
                initialPlaylist,
                updatedPlaylist -> CPlaylistRepository.savePlaylist(playlistName, updatedPlaylist),
                this::reloadEntries
            )
        );
    }

    private void onDeleteSelected() {
        CPlaylistRepository.PlaylistSummary selectedSummary = this.getSelectedSummary();
        if (selectedSummary == null || this.minecraft == null) {
            return;
        }

        String playlistName = selectedSummary.name();
        Component message = Component.empty()
            .append(Component.translatable("screen.custommusicclientsideplayer.config_playlists.confirm_delete_line1"))
            .append(Component.literal("\n"))
            .append(Component.literal("\"" + playlistName + "\"").withStyle(ChatFormatting.AQUA))
            .append(Component.translatable("screen.custommusicclientsideplayer.config_playlists.confirm_delete_line2"));
        ConfirmScreen confirmScreen = new ConfirmScreen(
            shouldDelete -> {
                this.minecraft.gui.setScreen(this);
                if (shouldDelete) {
                    this.deleteSelectedPlaylist(playlistName);
                }
            },
            Component.empty(),
            message,
            Component.translatable("screen.custommusicclientsideplayer.config_playlists.delete"),
            Component.translatable("screen.custommusicclientsideplayer.config_playlists.cancel")
        ) {
            @Override
            public void onClose() {
                CPlaylistConfigScreen.this.minecraft.gui.setScreen(CPlaylistConfigScreen.this);
            }
        };
        this.minecraft.gui.setScreen(confirmScreen);
    }

    private void deleteSelectedPlaylist(String playlistName) {
        try {
            boolean deleted = CPlaylistRepository.deletePlaylist(playlistName);
            if (!deleted) {
                if (this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.playlist_not_found"));
                }

                return;
            }
        } catch (IOException exception) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.delete_playlist_failed"));
            }

            return;
        }

        this.reloadEntries();
    }

    private void onCreateNew() {
        if (this.minecraft == null) {
            return;
        }

        this.minecraft.gui.setScreen(new CCreatePlaylistScreen(this, this::reloadEntries));
    }

    private void onRecreateSelected() {
        CPlaylistRepository.PlaylistSummary selectedSummary = this.getSelectedSummary();
        if (selectedSummary == null || this.minecraft == null) {
            return;
        }

        String playlistName = selectedSummary.name();
        Component message = Component.empty()
            .append(Component.translatable("screen.custommusicclientsideplayer.config_playlists.confirm_recreate_line1"))
            .append(Component.literal(playlistName).withStyle(ChatFormatting.AQUA))
            .append(Component.translatable("screen.custommusicclientsideplayer.config_playlists.confirm_recreate_line2"));
        ConfirmScreen confirmScreen = new ConfirmScreen(
            shouldRecreate -> {
                this.minecraft.gui.setScreen(this);
                if (shouldRecreate) {
                    this.recreateSelectedPlaylist(playlistName);
                }
            },
            Component.empty(),
            message,
            Component.translatable("screen.custommusicclientsideplayer.config_playlists.recreate"),
            Component.translatable("screen.custommusicclientsideplayer.config_playlists.cancel")
        ) {
            @Override
            public void onClose() {
                CPlaylistConfigScreen.this.minecraft.gui.setScreen(CPlaylistConfigScreen.this);
            }
        };
        this.minecraft.gui.setScreen(confirmScreen);
    }

    private void recreateSelectedPlaylist(String playlistName) {
        try {
            CPlaylistRepository.savePlaylist(playlistName, List.of());
        } catch (IOException exception) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.recreate_playlist_failed"));
            }

            return;
        }

        this.reloadEntries();
    }

    private void openViewPlaylistTracks(String playlistName) {
        List<Identifier> tracks;
        try {
            tracks = CPlaylistRepository.getPlaylist(playlistName);
        } catch (IOException exception) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.open_playlist_tracks_failed"));
            }

            return;
        }

        this.minecraft.gui.setScreen(new CPlaylistTracksViewScreen(this, playlistName, tracks));
    }

    private boolean canMovePlaylistUp(String playlistName) {
        int index = this.findPlaylistIndex(playlistName);
        return index > 0;
    }

    private boolean canMovePlaylistDown(String playlistName) {
        int index = this.findPlaylistIndex(playlistName);
        return index >= 0 && index < this.allSummaries.size() - 1;
    }

    private int findPlaylistIndex(String playlistName) {
        for (int i = 0; i < this.allSummaries.size(); i++) {
            if (this.allSummaries.get(i).name().equals(playlistName)) {
                return i;
            }
        }

        return -1;
    }

    private void movePlaylist(String playlistName, int offset) {
        try {
            boolean moved = CPlaylistRepository.movePlaylist(playlistName, offset);
            if (!moved) {
                return;
            }
        } catch (IOException exception) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.reorder_playlists_failed"));
            }

            return;
        }

        this.reloadEntries();
    }

    private void playUiClickSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private static String buildDescription(CPlaylistRepository.PlaylistSummary summary) {
        String formattedTime = TIME_FORMATTER.format(Instant.ofEpochMilli(summary.modifiedAt()).atZone(ZoneId.systemDefault()));
        return Component.translatable("screen.custommusicclientsideplayer.config_playlists.description", summary.trackCount(), formattedTime).getString();
    }

    private static final class PlaylistSelectionList extends ObjectSelectionList<PlaylistSelectionList.Entry> {
        private final CPlaylistConfigScreen screen;

        private PlaylistSelectionList(Minecraft minecraft, CPlaylistConfigScreen screen, int width, int height) {
            super(minecraft, width, height, 0, 36);
            this.screen = screen;
            this.centerListVertically = false;
        }

        @Override
        public void updateSize(int width, HeaderAndFooterLayout layout) {
            this.updateSizeAndPosition(width, layout.getContentHeight(), 0, layout.getHeaderHeight());
        }

        @Override
        public int getRowWidth() {
            return 270;
        }

        @Override
        protected int scrollBarX() {
            return this.getRowRight() + 8;
        }

        private void setEntries(List<CPlaylistRepository.PlaylistSummary> summaries) {
            this.clearEntries();
            if (summaries.isEmpty()) {
                this.addEntry(new EmptyEntry(this.screen));
            } else {
                for (CPlaylistRepository.PlaylistSummary summary : summaries) {
                    this.addEntry(new PlaylistEntry(this.screen, summary));
                }
            }

            this.refreshScrollAmount();
            this.setSelected(null);
            this.screen.updateButtonStates();
        }

        @Override
        public void setSelected(Entry entry) {
            if (entry instanceof EmptyEntry) {
                super.setSelected(null);
            } else {
                super.setSelected(entry);
            }

            this.screen.updateButtonStates();
        }

        private abstract static class Entry extends ObjectSelectionList.Entry<Entry> {
        }

        private static final class EmptyEntry extends Entry {
            private final CPlaylistConfigScreen screen;

            private EmptyEntry(CPlaylistConfigScreen screen) {
                this.screen = screen;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
                guiGraphics.centeredText(this.screen.font, Component.translatable("screen.custommusicclientsideplayer.config_playlists.empty"), this.getX() + this.getWidth() / 2, this.getContentYMiddle() - 4, 0xFF808080);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("screen.custommusicclientsideplayer.config_playlists.empty_narration");
            }
        }

        private static final class PlaylistEntry extends Entry {
            private final CPlaylistConfigScreen screen;
            private final CPlaylistRepository.PlaylistSummary summary;

            private PlaylistEntry(CPlaylistConfigScreen screen, CPlaylistRepository.PlaylistSummary summary) {
                this.screen = screen;
                this.summary = summary;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
                Font font = this.screen.font;
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LIST_ICON, this.getContentX(), this.getContentY(), 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

                int textX = this.getContentX() + ICON_SIZE + 6;
                guiGraphics.text(font, this.summary.name(), textX, this.getContentY() + 1, 0xFFFFFFFF, false);
                guiGraphics.text(font, buildDescription(this.summary), textX, this.getContentY() + 12, 0xFF808080, false);

                boolean showOverlay = !this.screen.minecraft.getLastInputType().isMouse() || hovered;
                if (showOverlay) {
                    guiGraphics.fill(this.getContentX(), this.getContentY(), this.getContentX() + ICON_SIZE, this.getContentY() + ICON_SIZE, 0xA0000000);

                    int localX = mouseX - this.getContentX();
                    int localY = mouseY - this.getContentY();
                    boolean overMoveUp = this.screen.canMovePlaylistUp(this.summary.name()) && mouseOverMoveUpIcon(localX, localY);
                    boolean overMoveDown = this.screen.canMovePlaylistDown(this.summary.name()) && mouseOverMoveDownIcon(localX, localY);
                    boolean overView = mouseOverViewIcon(localX, localY) && !overMoveUp && !overMoveDown;
                    if (overView) {
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, VIEW_HIGHLIGHTED_SPRITE, this.getContentX() + VIEW_RENDER_X_OFFSET, this.getContentY(), ICON_SIZE, ICON_SIZE);
                    } else {
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, VIEW_SPRITE, this.getContentX() + VIEW_RENDER_X_OFFSET, this.getContentY(), ICON_SIZE, ICON_SIZE);
                    }

                    if (this.screen.canMovePlaylistUp(this.summary.name())) {
                        if (overMoveUp) {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_UP_HIGHLIGHTED_SPRITE, this.getContentX() + MOVE_RENDER_X_OFFSET, this.getContentY(), ICON_SIZE, ICON_SIZE);
                        } else {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_UP_SPRITE, this.getContentX() + MOVE_RENDER_X_OFFSET, this.getContentY(), ICON_SIZE, ICON_SIZE);
                        }
                    }

                    if (this.screen.canMovePlaylistDown(this.summary.name())) {
                        if (overMoveDown) {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_HIGHLIGHTED_SPRITE, this.getContentX() + MOVE_RENDER_X_OFFSET, this.getContentY(), ICON_SIZE, ICON_SIZE);
                        } else {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_SPRITE, this.getContentX() + MOVE_RENDER_X_OFFSET, this.getContentY(), ICON_SIZE, ICON_SIZE);
                        }
                    }
                }
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.summary.name());
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
                int localX = (int)mouseButtonEvent.x() - this.getContentX();
                int localY = (int)mouseButtonEvent.y() - this.getContentY();

                if (this.screen.canMovePlaylistUp(this.summary.name()) && mouseOverMoveUpIcon(localX, localY)) {
                    this.screen.playUiClickSound();
                    this.screen.movePlaylist(this.summary.name(), -1);
                    return true;
                }

                if (this.screen.canMovePlaylistDown(this.summary.name()) && mouseOverMoveDownIcon(localX, localY)) {
                    this.screen.playUiClickSound();
                    this.screen.movePlaylist(this.summary.name(), 1);
                    return true;
                }

                if (mouseOverViewIcon(localX, localY)) {
                    this.screen.playUiClickSound();
                    this.screen.openViewPlaylistTracks(this.summary.name());
                    return true;
                }

                return super.mouseClicked(mouseButtonEvent, bl);
            }

            private static boolean mouseOverViewIcon(int x, int y) {
                return isInsideRect(x, y, VIEW_MIN_X, VIEW_MIN_Y, VIEW_MAX_X_EXCLUSIVE, VIEW_MAX_Y_EXCLUSIVE);
            }

            private static boolean mouseOverMoveUpIcon(int x, int y) {
                return isInsideRect(x, y, MOVE_UP_MIN_X, MOVE_UP_MIN_Y, MOVE_UP_MAX_X_EXCLUSIVE, MOVE_UP_MAX_Y_EXCLUSIVE);
            }

            private static boolean mouseOverMoveDownIcon(int x, int y) {
                return isInsideRect(x, y, MOVE_DOWN_MIN_X, MOVE_DOWN_MIN_Y, MOVE_DOWN_MAX_X_EXCLUSIVE, MOVE_DOWN_MAX_Y_EXCLUSIVE);
            }

            private static boolean isInsideRect(int x, int y, int minX, int minY, int maxXExclusive, int maxYExclusive) {
                return x >= minX && x < maxXExclusive && y >= minY && y < maxYExclusive;
            }
        }
    }
}


