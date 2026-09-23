package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;
import org.exmple.newcustommusicclientsideplayer.client.gui.component.CDropdownGeometry.ExpansionDirection;
import org.exmple.newcustommusicclientsideplayer.client.gui.component.CDropdownScreenCoordinator;
import org.exmple.newcustommusicclientsideplayer.client.gui.component.CDropdownSelector;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameImportException;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameImportPreview;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameRepository;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameTransferService;

public final class CSingleTrackSelectScreen extends Screen {
    private static final int LIST_WIDTH = 200;
    private static final int LIST_ROW_HEIGHT = 36;
    private static final int HEADER_ITEM_SPACING = 4;
    private static final int HEADER_VERTICAL_PADDING = 4;
    private static final int SEARCH_BOX_HEIGHT = 20;
    private static final int NAMESPACE_SELECTOR_HEIGHT = 20;
    private static final int MAX_VISIBLE_NAMESPACES = 6;
    private static final int FOOTER_HEIGHT = 60;
    private static final int COLOR_TEXT_WHITE = -1;
    private static final int COLOR_TEXT_GRAY = -8355712;
    private static final int FOOTER_BUTTON_WIDTH = 150;
    private static final int FOOTER_COLUMN_SPACING = 8;
    private static final int FOOTER_ROW_SPACING = 4;
    private static final int TRANSFER_BUTTON_SIZE = 20;
    private static final int TRANSFER_BUTTON_OFFSET = 8;
    private static final int TRANSFER_ROW_SPACING = 4;
    private static final int TRANSFER_BUTTON_BALANCE_WIDTH =
        TRANSFER_BUTTON_OFFSET + TRANSFER_BUTTON_SIZE * 2 + TRANSFER_ROW_SPACING * 2;
    private static final Component TITLE = Component.translatable("screen.custommusicclientsideplayer.singletrack.title");
    private static final Component SEARCH_HINT = Component.translatable("screen.custommusicclientsideplayer.playlist_editor.search_sound_id").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final Component PLAY_SELECTED_TEXT = Component.translatable("screen.custommusicclientsideplayer.singletrack.play_selected");
    private static final Component RENAME_TRACK_TEXT = Component.translatable("screen.custommusicclientsideplayer.rename_track.button");
    private static final Component RESTORE_TRACK_NAME_TEXT = Component.translatable("screen.custommusicclientsideplayer.restore_track_name.button");
    private static final Component RESTORE_TRACK_NAME_TOOLTIP = Component.translatable("screen.custommusicclientsideplayer.restore_track_name.tooltip");
    private static final Component RESTORE_TRACK_NAME_FAILED = Component.translatable("screen.custommusicclientsideplayer.error.restore_track_name_failed");
    private static final Component IMPORT_TRACK_NAMES_TOOLTIP = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_tooltip");
    private static final Component EXPORT_TRACK_NAMES_TOOLTIP = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.export_tooltip");
    private static final Component MUST_DISABLE_FULLSCREEN = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.must_disable_fullscreen");
    private static final Component IMPORT_DIALOG_TITLE = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_dialog_title");
    private static final Component IMPORT_DIALOG_FILTER = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_dialog_filter");
    private static final Component EXPORT_DIALOG_TITLE = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.export_dialog_title");
    private static final Component EXPORT_DIALOG_FILTER = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.export_dialog_filter");
    private static final Component IMPORT_SUCCESS_TOAST_TITLE = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.toast.import_success.title");
    private static final String IMPORT_SUCCESS_TOAST_DESCRIPTION_KEY = "screen.custommusicclientsideplayer.track_name_transfer.toast.import_success.description";
    private static final Component EXPORT_SUCCESS_TOAST_TITLE = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.toast.export_success.title");
    private static final String EXPORT_SUCCESS_TOAST_DESCRIPTION_KEY = "screen.custommusicclientsideplayer.track_name_transfer.toast.export_success.description";
    private static final Component EXPORT_FAILED_TOAST_TITLE = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.toast.export_failed.title");
    private static final String EXPORT_FAILED_TOAST_DESCRIPTION_KEY = "screen.custommusicclientsideplayer.track_name_transfer.toast.export_failed.description";
    private static final String EXPORT_DIALOG_FAILED_TOAST_DESCRIPTION_KEY = "screen.custommusicclientsideplayer.track_name_transfer.toast.export_dialog_failed.description";
    private static final Component IMPORT_BACK = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_back");
    private static final Component IMPORT_CANCEL = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_cancel");
    private static final Component IMPORT_IO_ERROR_TITLE = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_io_error.title");
    private static final Component IMPORT_IO_ERROR_BODY = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_io_error.body");
    private static final Component IMPORT_INVALID_TITLE = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_invalid.title");
    private static final Component IMPORT_INVALID_BODY = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_invalid.body");
    private static final Component IMPORT_NO_COMPATIBLE_TITLE = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_no_compatible.title");
    private static final Component IMPORT_NO_COMPATIBLE_BODY = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_no_compatible.body");
    private static final Component IMPORT_WARNING_TITLE = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_warning.title");
    private static final Component IMPORT_WARNING_BODY = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_warning.body");
    private static final Component IMPORT_WARNING_CONFIRM = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_warning.confirm");
    private static final Component IMPORT_SAVE_FAILED_TITLE = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_save_failed.title");
    private static final Component IMPORT_SAVE_FAILED_BODY = Component.translatable("screen.custommusicclientsideplayer.track_name_transfer.import_save_failed.body");
    private static final Identifier MC_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/mc_mus_icon.png");
    private static final Identifier OTHER_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/other_mus_icon.png");
    private static final Identifier IMPORT_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/import.png");
    private static final Identifier EXPORT_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/export.png");

    private final Screen parent;
    private final CTrackNameTransferService transferService = new CTrackNameTransferService();
    private final HeaderAndFooterLayout layout =
        new HeaderAndFooterLayout(this, 0, FOOTER_HEIGHT);
    private final CDropdownScreenCoordinator dropdownCoordinator =
        new CDropdownScreenCoordinator();
    private final List<Identifier> allSounds = new ArrayList<>();
    private List<String> namespaces = List.of("minecraft");
    private int namespaceIndex;

    private SoundSelectionList soundSelectionList;
    private CDropdownSelector<String> namespaceSelector;
    private EditBox searchBox;
    private Button importButton;
    private Button exportButton;
    private Button playSelectedTrackButton;
    private Button renameTrackButton;
    private Button restoreTrackNameButton;
    private String filter = "";

    public CSingleTrackSelectScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.dropdownCoordinator.resetForRebuild();
        this.layout.removeChildren();
        this.updateHeaderMetrics();

        LinearLayout header = this.layout.addToHeader(
            LinearLayout.vertical().spacing(HEADER_ITEM_SPACING)
        );
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title, this.font));
        this.namespaceSelector = header.addChild(
            this.dropdownCoordinator.register(
                new CDropdownSelector<>(
                    0,
                    0,
                    LIST_WIDTH,
                    NAMESPACE_SELECTOR_HEIGHT,
                    this.namespaces,
                    this.getCurrentNamespace(),
                    Component::literal,
                    this::selectNamespace,
                    ExpansionDirection.DOWN,
                    NAMESPACE_SELECTOR_HEIGHT,
                    MAX_VISIBLE_NAMESPACES
                )
            )
        );
        LinearLayout searchRow = header.addChild(
            LinearLayout.horizontal().spacing(TRANSFER_ROW_SPACING)
        );
        searchRow.defaultCellSetting().alignVerticallyMiddle();
        searchRow.addChild(SpacerElement.width(TRANSFER_BUTTON_BALANCE_WIDTH));
        this.searchBox = searchRow.addChild(
            new EditBox(
                this.font,
                0,
                0,
                LIST_WIDTH,
                SEARCH_BOX_HEIGHT,
                Component.empty()
            )
        );
        this.searchBox.setHint(SEARCH_HINT);
        this.searchBox.setValue(this.filter);
        this.searchBox.setResponder(value -> {
            this.filter = value;
            this.refreshVisibleEntries();
        });
        searchRow.addChild(SpacerElement.width(TRANSFER_BUTTON_OFFSET));
        this.importButton = searchRow.addChild(
            Button.builder(Component.empty(), ignoredButton -> this.onImportTrackNames())
                .width(TRANSFER_BUTTON_SIZE)
                .tooltip(Tooltip.create(IMPORT_TRACK_NAMES_TOOLTIP))
                .build()
        );
        this.exportButton = searchRow.addChild(
            Button.builder(Component.empty(), ignoredButton -> this.onExportTrackNames())
                .width(TRANSFER_BUTTON_SIZE)
                .tooltip(Tooltip.create(EXPORT_TRACK_NAMES_TOOLTIP))
                .build()
        );

        this.soundSelectionList = this.layout.addToContents(new SoundSelectionList(this.minecraft, this, this.width, this.layout.getContentHeight()));

        GridLayout footer = this.layout.addToFooter(
            new GridLayout().columnSpacing(FOOTER_COLUMN_SPACING).rowSpacing(FOOTER_ROW_SPACING)
        );
        footer.defaultCellSetting().alignHorizontallyCenter();
        GridLayout.RowHelper footerRows = footer.createRowHelper(2);
        this.playSelectedTrackButton = footerRows.addChild(
            Button.builder(
                PLAY_SELECTED_TEXT,
                ignoredButton -> this.onPlaySelectedTrack()
            ).width(FOOTER_BUTTON_WIDTH).build()
        );
        this.renameTrackButton = footerRows.addChild(
            Button.builder(
                RENAME_TRACK_TEXT,
                ignoredButton -> this.onRenameSelectedTrack()
            ).width(FOOTER_BUTTON_WIDTH).build()
        );
        this.restoreTrackNameButton = footerRows.addChild(
            Button.builder(
                RESTORE_TRACK_NAME_TEXT,
                ignoredButton -> this.onRestoreSelectedTrackName()
            ).width(FOOTER_BUTTON_WIDTH)
                .build()
        );
        footerRows.addChild(
            Button.builder(
                CommonComponents.GUI_BACK,
                ignoredButton -> this.onClose()
            ).width(FOOTER_BUTTON_WIDTH).build()
        );

        this.layout.visitWidgets(this::addRenderableWidget);
        if (this.soundSelectionList != null) {
            this.soundSelectionList.updateSize(this.width, this.layout);
        }

        this.layout.arrangeElements();
        this.reloadSounds();
        this.updateButtonStates();
        this.setInitialFocus(this.searchBox);
    }

    @Override
    protected void repositionElements() {
        this.updateHeaderMetrics();
        if (this.soundSelectionList != null) {
            this.soundSelectionList.updateSize(this.width, this.layout);
        }

        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.dropdownCoordinator.closeAll();
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.updateButtonStates();
        boolean mouseCoveredByDropdown =
            this.dropdownCoordinator.isMouseOverExpandedOverlay(mouseX, mouseY);
        super.extractRenderState(
            guiGraphics,
            mouseCoveredByDropdown ? Integer.MIN_VALUE : mouseX,
            mouseCoveredByDropdown ? Integer.MIN_VALUE : mouseY,
            partialTick
        );
        this.extractTransferButtonIcon(guiGraphics, this.importButton, IMPORT_ICON);
        this.extractTransferButtonIcon(guiGraphics, this.exportButton, EXPORT_ICON);
        this.dropdownCoordinator.extractOverlays(
            guiGraphics,
            mouseX,
            mouseY,
            partialTick
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.dropdownCoordinator.mouseClicked(
            event,
            doubleClick,
            this::clearFocus
        )
            || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.dropdownCoordinator.mouseReleased(event)
            || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(
        MouseButtonEvent event,
        double dragX,
        double dragY
    ) {
        return this.dropdownCoordinator.mouseDragged(event, dragX, dragY)
            || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(
        double mouseX,
        double mouseY,
        double horizontalAmount,
        double verticalAmount
    ) {
        return this.dropdownCoordinator.mouseScrolled(
            mouseX,
            mouseY,
            horizontalAmount,
            verticalAmount
        ) || super.mouseScrolled(
            mouseX,
            mouseY,
            horizontalAmount,
            verticalAmount
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.dropdownCoordinator.keyPressed(event)
            || super.keyPressed(event);
    }

    @Override
    public void removed() {
        this.dropdownCoordinator.closeAll();
        super.removed();
    }

    private void updateHeaderMetrics() {
        int headerHeight = HEADER_VERTICAL_PADDING * 2
            + this.font.lineHeight
            + NAMESPACE_SELECTOR_HEIGHT
            + SEARCH_BOX_HEIGHT
            + HEADER_ITEM_SPACING * 2;
        this.layout.setHeaderHeight(headerHeight);
        if (this.searchBox != null) {
            this.searchBox.setHeight(SEARCH_BOX_HEIGHT);
        }
    }

    private void reloadSounds() {
        if (this.minecraft.player == null) {
            return;
        }

        Collection<Identifier> available = this.minecraft.getSoundManager().getAvailableSounds();
        this.allSounds.clear();
        this.allSounds.addAll(available.stream().sorted(Comparator.comparing(Identifier::toString)).toList());
        String previousNamespace = this.getCurrentNamespace();
        List<String> availableNamespaces = this.allSounds.stream()
            .map(Identifier::getNamespace)
            .distinct()
            .sorted()
            .toList();
        this.namespaces = availableNamespaces.isEmpty()
            ? List.of("minecraft")
            : availableNamespaces;
        int previousIndex = this.namespaces.indexOf(previousNamespace);
        this.namespaceIndex = previousIndex >= 0 ? previousIndex : 0;
        if (this.namespaceSelector != null) {
            this.namespaceSelector.replaceOptions(this.namespaces);
            this.namespaceSelector.selectValue(this.getCurrentNamespace());
        }
        this.refreshVisibleEntries();
    }

    private void selectNamespace(String namespace) {
        int selectedIndex = this.namespaces.indexOf(namespace);
        if (selectedIndex < 0 || selectedIndex == this.namespaceIndex) {
            return;
        }

        this.namespaceIndex = selectedIndex;
        this.refreshVisibleEntries();
    }

    private void refreshVisibleEntries() {
        if (this.soundSelectionList == null) {
            return;
        }

        String currentFilter = this.filter.toLowerCase(Locale.ROOT);
        String namespace = this.getCurrentNamespace();
        List<Identifier> visible = this.allSounds.stream()
            .filter(id -> id.getNamespace().equals(namespace))
            .filter(id -> currentFilter.isBlank()
                || id.getPath().toLowerCase(Locale.ROOT).contains(currentFilter)
                || CTrackNameRepository.getDisplayName(id).toLowerCase(Locale.ROOT).contains(currentFilter))
            .toList();
        this.soundSelectionList.setEntries(visible);
        this.updateButtonStates();
    }

    private void updateButtonStates() {
        Identifier selectedTrack = this.getSelectedSound();
        boolean hasSelection = selectedTrack != null;
        boolean hasCustomName = hasSelection && CTrackNameRepository.getCustomName(selectedTrack) != null;
        boolean fullscreen = this.minecraft != null && this.minecraft.getWindow().isExclusiveFullscreen();
        Tooltip fullscreenTooltip = fullscreen ? Tooltip.create(MUST_DISABLE_FULLSCREEN) : null;
        if (this.importButton != null) {
            this.importButton.active = !fullscreen;
            this.importButton.setTooltip(fullscreen ? fullscreenTooltip : Tooltip.create(IMPORT_TRACK_NAMES_TOOLTIP));
        }
        if (this.exportButton != null) {
            this.exportButton.active = !fullscreen;
            this.exportButton.setTooltip(fullscreen ? fullscreenTooltip : Tooltip.create(EXPORT_TRACK_NAMES_TOOLTIP));
        }
        if (this.playSelectedTrackButton != null) {
            this.playSelectedTrackButton.active = hasSelection;
        }

        if (this.renameTrackButton != null) {
            this.renameTrackButton.active = hasSelection;
        }
        if (this.restoreTrackNameButton != null) {
            this.restoreTrackNameButton.active = hasCustomName;
            this.restoreTrackNameButton.setTooltip(
                hasCustomName ? Tooltip.create(RESTORE_TRACK_NAME_TOOLTIP) : null
            );
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
            guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0x99000000);
        }
    }

    private void onImportTrackNames() {
        if (this.minecraft == null || this.minecraft.getWindow().isExclusiveFullscreen()) {
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
                this.openImportMessageScreen(IMPORT_IO_ERROR_TITLE, IMPORT_IO_ERROR_BODY, List.of());
                return;
            }
            selectedPath.ifPresent(this::handleImportPath);
        }));
    }

    private void handleImportPath(Path path) {
        CTrackNameImportPreview preview;
        try {
            preview = this.transferService.previewImport(path, this.allSounds);
        } catch (CTrackNameImportException exception) {
            this.openImportExceptionScreen(exception);
            return;
        }

        if (!preview.hasCompatibleNames()) {
            this.openImportMessageScreen(
                IMPORT_NO_COMPATIBLE_TITLE,
                IMPORT_NO_COMPATIBLE_BODY,
                CTrackNameImportWarningFormatter.format(preview)
            );
            return;
        }
        if (preview.hasWarnings()) {
            this.openImportWarningScreen(preview);
            return;
        }

        this.applyImportPreview(preview);
    }

    private boolean applyImportPreview(CTrackNameImportPreview preview) {
        try {
            CTrackNameRepository.ImportResult result = this.transferService.importPreview(preview);
            this.refreshVisibleEntries();
            this.showImportSuccess(result.changedCount());
            return true;
        } catch (IOException | RuntimeException exception) {
            this.openImportMessageScreen(IMPORT_SAVE_FAILED_TITLE, IMPORT_SAVE_FAILED_BODY, List.of());
            return false;
        }
    }

    private void onExportTrackNames() {
        if (this.minecraft == null || this.minecraft.getWindow().isExclusiveFullscreen()) {
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
            this.transferService.exportTrackNames(path);
        } catch (IOException | RuntimeException exception) {
            this.showExportError(path);
            return;
        }

        this.showExportSuccess(path);
        this.updateButtonStates();
    }

    private void openImportExceptionScreen(CTrackNameImportException exception) {
        switch (exception.reason()) {
            case IO_ERROR -> this.openImportMessageScreen(IMPORT_IO_ERROR_TITLE, IMPORT_IO_ERROR_BODY, List.of());
            case INVALID_TRACK_NAME_FILE -> this.openImportMessageScreen(
                IMPORT_INVALID_TITLE,
                IMPORT_INVALID_BODY,
                List.of()
            );
        }
    }

    private void openImportMessageScreen(Component title, Component body, List<Component> items) {
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.gui.setScreen(new CImportMessageScreen(
            this,
            title,
            body,
            null,
            items,
            IMPORT_BACK,
            () -> this.minecraft.gui.setScreen(this),
            null,
            null
        ));
    }

    private void openImportWarningScreen(CTrackNameImportPreview preview) {
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.gui.setScreen(new CImportMessageScreen(
            this,
            IMPORT_WARNING_TITLE,
            IMPORT_WARNING_BODY,
            null,
            CTrackNameImportWarningFormatter.format(preview),
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

    private Identifier getSelectedSound() {
        if (this.soundSelectionList == null) {
            return null;
        }

        SoundSelectionList.Entry selected = this.soundSelectionList.getSelected();
        if (selected instanceof SoundSelectionList.SoundEntry soundEntry) {
            return soundEntry.id;
        }

        return null;
    }

    private String getCurrentNamespace() {
        if (this.namespaces.isEmpty()) {
            return "minecraft";
        }

        if (this.namespaceIndex < 0 || this.namespaceIndex >= this.namespaces.size()) {
            this.namespaceIndex = 0;
        }

        return this.namespaces.get(this.namespaceIndex);
    }

    private void onPlaySelectedTrack() {
        Identifier selected = this.getSelectedSound();
        if (selected == null || this.minecraft.level == null || this.minecraft.player == null) {
            return;
        }

        this.minecraft.gui.setScreen(
            new CSingleTrackPlayConfirmScreen(this, selected, (loop, pitch) -> {
                this.minecraft.gui.setScreen(null);
                CPlaySoundController.playFromUi(this.minecraft, selected, loop, pitch);
            })
        );
    }

    private void onRenameSelectedTrack() {
        Identifier selected = this.getSelectedSound();
        if (selected == null || this.minecraft.player==null ||this.minecraft.level == null) {
            return;
        }

        List<Identifier> sameNamespaceTracks = this.allSounds.stream()
            .filter(id -> id.getNamespace().equals(selected.getNamespace()))
            .toList();
        this.minecraft.gui.setScreen(new CRenameTrackScreen(this, selected, sameNamespaceTracks, this::refreshVisibleEntries));
    }

    private void onRestoreSelectedTrackName() {
        Identifier selected = this.getSelectedSound();
        if (selected == null || CTrackNameRepository.getCustomName(selected) == null) {
            return;
        }

        try {
            CTrackNameRepository.renameTrack(
                selected,
                CTrackNameRepository.getDefaultDisplayName(selected)
            );
            this.refreshVisibleEntries();
        } catch (IOException | RuntimeException exception) {
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(RESTORE_TRACK_NAME_FAILED);
            }
        }
    }

    private void playUiClickSound() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    static String getDisplayName(Identifier id) {
        return CTrackNameRepository.getDisplayName(id);
    }

    private final class SoundSelectionList extends ObjectSelectionList<SoundSelectionList.Entry> {
        private SoundSelectionList(Minecraft minecraft, CSingleTrackSelectScreen ignoredScreen, int width, int height) {
            super(minecraft, width, height, 0, LIST_ROW_HEIGHT);
            this.centerListVertically = false;
        }

        @Override
        public void updateSize(int width, HeaderAndFooterLayout layout) {
            this.updateSizeAndPosition(
                width,
                layout.getContentHeight(),
                0,
                layout.getHeaderHeight()
            );
        }

        @Override
        public int getRowWidth() {
            return 270;
        }

        @Override
        protected int scrollBarX() {
            return this.getRowRight() + 8;
        }

        private void setEntries(List<Identifier> identifiers) {
            this.clearEntries();
            if (identifiers.isEmpty()) {
                this.addEntry(new EmptyEntry());
            } else {
                int index = 1;
                for (Identifier id : identifiers) {
                    this.addEntry(new SoundEntry(id, index++));
                }
            }

            this.refreshScrollAmount();
            this.setSelected(null);
        }

        @Override
        public void setSelected(Entry entry) {
            super.setSelected(entry);
            CSingleTrackSelectScreen.this.updateButtonStates();
        }

        private static abstract class Entry extends ObjectSelectionList.Entry<Entry> {
        }

        private final class EmptyEntry extends Entry {
            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
                guiGraphics.centeredText(CSingleTrackSelectScreen.this.font, Component.translatable("screen.custommusicclientsideplayer.playlist_editor.empty"), this.getX() + this.getWidth() / 2, this.getContentYMiddle() - 4, COLOR_TEXT_GRAY);
            }

            @Override
            @SuppressWarnings("all")//掩耳盗铃？LOL
            public Component getNarration() {
                return Component.translatable("screen.custommusicclientsideplayer.playlist_editor.empty");
            }
        }

        private final class SoundEntry extends Entry {
            private final SoundSelectionList list;
            private final Identifier id;
            private final int order;

            private SoundEntry(Identifier id, int order) {
                this.list = SoundSelectionList.this;
                this.id = id;
                this.order = order;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
                Font font = CSingleTrackSelectScreen.this.font;
                Identifier icon = this.id.getNamespace().equals("minecraft") ? MC_MUS_ICON : OTHER_MUS_ICON;
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, this.getContentX(), this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);

                int textX = this.getContentX() + 34;
                guiGraphics.text(font, Component.translatable("screen.custommusicclientsideplayer.view_tracks.track_order", this.order, CSingleTrackSelectScreen.getDisplayName(this.id)), textX, this.getContentY() + 1, COLOR_TEXT_WHITE, false);
                guiGraphics.text(font, buildDescription(this.id), textX, this.getContentY() + 12, COLOR_TEXT_GRAY, false);

                boolean showHoverOverlay = !this.list.minecraft.getLastInputType().isMouse() || hovered || this.list.getSelected() == this && this.list.isFocused();
                if (showHoverOverlay) {
                    guiGraphics.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
                }
            }

            @Override
            @SuppressWarnings("all")
            public Component getNarration() {
                return Component.literal(this.id.toString());
            }

            @Override
            @SuppressWarnings("all")
            public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
                CSingleTrackSelectScreen.this.playUiClickSound();
                this.list.setSelected(this);
                return true;
            }

            private Component buildDescription(Identifier id) {
                return Component.translatable(
                    "screen.custommusicclientsideplayer.common.music_files_from",
                    Component.literal(id.getNamespace()).withStyle(style -> style.withBold(true))
                );
            }
        }
    }
}



