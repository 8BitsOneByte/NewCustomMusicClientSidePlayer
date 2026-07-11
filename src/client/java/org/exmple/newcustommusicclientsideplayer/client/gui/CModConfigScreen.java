package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;
import org.exmple.newcustommusicclientsideplayer.client.config.CConfigImportException;
import org.exmple.newcustommusicclientsideplayer.client.config.CConfigImportPreview;
import org.exmple.newcustommusicclientsideplayer.client.config.CModConfigEditSession;
import org.exmple.newcustommusicclientsideplayer.client.config.CModConfigRepository;
import org.exmple.newcustommusicclientsideplayer.client.config.CModConfigTransferService;
import org.exmple.newcustommusicclientsideplayer.client.config.CNowPlayingFeedbackMode;

public final class CModConfigScreen extends Screen {
    private static final Component TITLE =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.title");
    private static final Component CHECK_FOR_UPDATES =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.check_for_updates");
    private static final Component NOW_PLAYING_TOAST =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.now_playing_toast");
    private static final Component NOW_PLAYING_FEEDBACK =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.now_playing_feedback");
    private static final Component APPLY =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.apply");
    private static final Component IMPORT_CONFIG =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import");
    private static final Component EXPORT_CONFIG =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.export");
    private static final Component RESET_TO_DEFAULTS =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.reset_to_defaults");
    private static final Component SAVE_FAILED =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.save_failed")
            .withStyle(ChatFormatting.RED);
    private static final Component DONE_UNSAVED_CHANGES =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.done_unsaved_changes");
    private static final Component MUST_DISABLE_FULLSCREEN =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.must_disable_fullscreen");
    private static final Component IMPORT_DIALOG_TITLE =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_dialog_title");
    private static final Component IMPORT_DIALOG_FILTER =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_dialog_filter");
    private static final Component EXPORT_DIALOG_TITLE =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.export_dialog_title");
    private static final Component EXPORT_DIALOG_FILTER =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.export_dialog_filter");
    private static final Component IMPORT_SUCCESS_TOAST_TITLE =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.toast.import_success.title");
    private static final String IMPORT_SUCCESS_TOAST_DESCRIPTION_KEY =
        "screen.custommusicclientsideplayer.mod_config.toast.import_success.description";
    private static final Component EXPORT_SUCCESS_TOAST_TITLE =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.toast.export_success.title");
    private static final String EXPORT_SUCCESS_TOAST_DESCRIPTION_KEY =
        "screen.custommusicclientsideplayer.mod_config.toast.export_success.description";
    private static final Component EXPORT_FAILED_TOAST_TITLE =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.toast.export_failed.title");
    private static final String EXPORT_FAILED_TOAST_DESCRIPTION_KEY =
        "screen.custommusicclientsideplayer.mod_config.toast.export_failed.description";
    private static final String EXPORT_DIALOG_FAILED_TOAST_DESCRIPTION_KEY =
        "screen.custommusicclientsideplayer.mod_config.toast.export_dialog_failed.description";
    private static final Component IMPORT_BACK =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_back");
    private static final Component IMPORT_CANCEL =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_cancel");
    private static final Component IMPORT_IO_ERROR_TITLE =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_io_error.title");
    private static final Component IMPORT_IO_ERROR_BODY =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_io_error.body");
    private static final Component IMPORT_INVALID_TITLE =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_invalid.title");
    private static final Component IMPORT_INVALID_BODY =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_invalid.body");
    private static final Component IMPORT_NO_COMPATIBLE_TITLE =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_no_compatible.title");
    private static final Component IMPORT_NO_COMPATIBLE_BODY =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_no_compatible.body");
    private static final Component IMPORT_WARNING_TITLE =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_warning.title");
    private static final Component IMPORT_WARNING_BODY =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_warning.body");
    private static final Component IMPORT_WARNING_SKIPPED_FIELDS =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_warning.skipped");
    private static final Component IMPORT_WARNING_CONFIRM =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.import_warning.confirm");

    private static final int HEADER_HEIGHT = 33;
    private static final int FOOTER_HEIGHT = 60;
    private static final int OPTION_WIDTH = 150;
    private static final int SPACING = 8;
    private static final int FOOTER_SMALL_BUTTON_WIDTH = 71;
    private static final int FOOTER_WIDE_BUTTON_WIDTH = FOOTER_SMALL_BUTTON_WIDTH * 2 + SPACING;
    private static final int FOOTER_GROUP_SPACING = 24;
    private static final int CONTENT_WIDTH = OPTION_WIDTH * 2 + SPACING;
    private static final int SEPARATOR_HEIGHT = 2;
    private static final int SEPARATOR_TEXTURE_WIDTH = 32;

    private final Screen parent;
    private final CModConfigRepository repository;
    private final CModConfigEditSession editSession;
    private final CModConfigTransferService transferService = new CModConfigTransferService();
    private final HeaderAndFooterLayout layout =
        new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);

    private StringWidget saveErrorWidget;
    private CycleButton<Boolean> checkForUpdatesButton;
    private CycleButton<Boolean> nowPlayingToastButton;
    private CycleButton<CNowPlayingFeedbackMode> nowPlayingFeedbackButton;
    private Button importButton;
    private Button exportButton;
    private Button applyButton;
    private Button doneButton;

    public CModConfigScreen(Screen parent) {
        this(parent, new CModConfigRepository());
    }

    CModConfigScreen(Screen parent, CModConfigRepository repository) {
        super(TITLE);
        this.parent = parent;
        this.repository = Objects.requireNonNull(repository, "repository");
        this.editSession = new CModConfigEditSession(
            this.repository.load()
        );
    }

    @Override
    protected void init() {
        this.layout.removeChildren();
        this.layout.addTitleHeader(this.title, this.font);

        GridLayout options = new GridLayout().columnSpacing(SPACING).rowSpacing(SPACING);
        options.defaultCellSetting().alignHorizontallyCenter();
        GridLayout.RowHelper rows = options.createRowHelper(2);
        this.checkForUpdatesButton = rows.addChild(
            CycleButton.onOffBuilder(this.editSession.draft().checkForUpdates())
                .create(
                    CHECK_FOR_UPDATES,
                    (button, enabled) -> {
                        this.editSession.setCheckForUpdates(enabled);
                        this.clearSaveError();
                        this.updateButtonStates();
                    }
                )
        );
        this.nowPlayingToastButton = rows.addChild(
            CycleButton.onOffBuilder(this.editSession.draft().nowPlayingToastEnabled())
                .create(
                    NOW_PLAYING_TOAST,
                    (button, enabled) -> {
                        this.editSession.setNowPlayingToastEnabled(enabled);
                        this.clearSaveError();
                        this.updateButtonStates();
                    }
                )
        );
        this.nowPlayingFeedbackButton = rows.addChild(
            CycleButton.<CNowPlayingFeedbackMode>builder(
                    CModConfigScreen::feedbackModeLabel,
                    this.editSession.draft().nowPlayingFeedbackMode()
                )
                .withValues(CNowPlayingFeedbackMode.values())
                .create(
                    NOW_PLAYING_FEEDBACK,
                    (button, mode) -> {
                        this.editSession.setNowPlayingFeedbackMode(mode);
                        this.clearSaveError();
                        this.updateButtonStates();
                    }
                )
        );
        rows.addChild(SpacerElement.width(OPTION_WIDTH));
        this.saveErrorWidget = rows.addChild(
            new StringWidget(CONTENT_WIDTH, this.font.lineHeight, Component.empty(), this.font),
            2
        );
        this.layout.addToContents(
            options,
            settings -> settings.alignHorizontallyCenter().alignVerticallyTop().paddingTop(SPACING)
        );

        LinearLayout footer = this.layout.addToFooter(
            LinearLayout.horizontal().spacing(FOOTER_GROUP_SPACING)
        );
        LinearLayout configActions = footer.addChild(
            LinearLayout.vertical().spacing(4)
        );
        LinearLayout transferActions = configActions.addChild(
            LinearLayout.horizontal().spacing(SPACING)
        );
        this.importButton = transferActions.addChild(
            Button.builder(IMPORT_CONFIG, button -> this.onImportConfig())
                .width(FOOTER_SMALL_BUTTON_WIDTH)
                .build()
        );
        this.exportButton = transferActions.addChild(
            Button.builder(EXPORT_CONFIG, button -> this.onExportConfig())
                .width(FOOTER_SMALL_BUTTON_WIDTH)
                .build()
        );
        configActions.addChild(
            Button.builder(RESET_TO_DEFAULTS, button -> this.onResetToDefaults())
                .width(FOOTER_WIDE_BUTTON_WIDTH)
                .build()
        );

        LinearLayout navigationActions = footer.addChild(
            LinearLayout.vertical().spacing(4)
        );
        LinearLayout applyActions = navigationActions.addChild(
            LinearLayout.horizontal().spacing(SPACING)
        );
        this.applyButton = applyActions.addChild(
            Button.builder(APPLY, button -> this.onApply())
                .width(FOOTER_SMALL_BUTTON_WIDTH)
                .build()
        );
        applyActions.addChild(
            Button.builder(CommonComponents.GUI_BACK, button -> this.onBack())
                .width(FOOTER_SMALL_BUTTON_WIDTH)
                .build()
        );
        this.doneButton = navigationActions.addChild(
            Button.builder(CommonComponents.GUI_DONE, button -> this.onDone())
                .width(FOOTER_WIDE_BUTTON_WIDTH)
                .build()
        );

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
        this.updateButtonStates();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor guiGraphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        this.extractSeparators(guiGraphics);
        this.updateButtonStates();
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.onBack();
    }

    /**
     * Back and Esc share this path: only draft changes since the last successful Apply are discarded,
     * and neither persistence nor runtime application is performed. Done uses a separate clean-only
     * path so it never repeats an already completed Apply operation while leaving the screen.
     */
    private void onBack() {
        this.editSession.discardDraft();
        this.returnToParent();
    }

    /**
     * Apply is intentionally ordered as persistence, runtime application, then edit-session commit.
     * A save failure stops the sequence before the update checker is touched and before the clean
     * baseline changes, leaving the screen dirty so the user can retry or discard the draft.
     */
    private void onApply() {
        if (!this.editSession.isDirty()) {
            return;
        }

        try {
            this.repository.save(this.editSession.draft());
        } catch (IOException | RuntimeException exception) {
            this.showSaveError();
            this.updateButtonStates();
            return;
        }

        NewcustommusicclientsideplayerClient.applyModConfig(this.editSession.draft());
        this.editSession.applyDraft();
        this.clearSaveError();
        this.updateButtonStates();
    }

    private void onImportConfig() {
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
        CConfigImportPreview preview;
        try {
            preview = this.transferService.previewImport(path, this.editSession.draft());
        } catch (CConfigImportException exception) {
            this.openImportExceptionScreen(exception);
            return;
        }

        if (!preview.hasSkippedFields()) {
            this.applyImportedConfig(preview);
            this.showImportSuccess(path);
            return;
        }

        this.minecraft.gui.setScreen(new CImportMessageScreen(
            this,
            IMPORT_WARNING_TITLE,
            IMPORT_WARNING_BODY,
            IMPORT_WARNING_SKIPPED_FIELDS,
            preview.skippedFields().stream()
                .map(field -> (Component) Component.literal(field))
                .toList(),
            IMPORT_CANCEL,
            () -> this.minecraft.gui.setScreen(this),
            IMPORT_WARNING_CONFIRM,
            () -> {
                this.applyImportedConfig(preview);
                this.showImportSuccess(path);
                this.minecraft.gui.setScreen(this);
            }
        ));
    }

    private void applyImportedConfig(CConfigImportPreview preview) {
        this.editSession.replaceDraft(preview.compatibleConfig());
        this.syncOptionWidgets();
        this.clearSaveError();
        this.updateButtonStates();
    }

    private void onExportConfig() {
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
            this.transferService.exportConfig(this.editSession.draft(), path);
        } catch (IOException | RuntimeException exception) {
            this.showExportError(path);
            return;
        }

        this.showExportSuccess(path);
        this.updateButtonStates();
    }

    private void onResetToDefaults() {
        this.editSession.resetDraftToDefaults();
        this.syncOptionWidgets();
        this.clearSaveError();
        this.updateButtonStates();
    }

    private void onDone() {
        if (!this.editSession.isDirty()) {
            this.returnToParent();
        }
    }

    /**
     * Back remains available in every state. Apply is enabled exactly while the draft differs from
     * the last successfully applied baseline, while Done is enabled only for a clean session. The
     * Done tooltip is present only in the dirty state; the self-evident disabled Apply state has none.
     */
    private void updateButtonStates() {
        boolean dirty = this.editSession.isDirty();
        boolean fullscreen = this.minecraft != null && this.minecraft.getWindow().isFullscreen();
        Tooltip fullscreenTooltip = fullscreen ? Tooltip.create(MUST_DISABLE_FULLSCREEN) : null;
        if (this.importButton != null) {
            this.importButton.active = !fullscreen;
            this.importButton.setTooltip(fullscreenTooltip);
        }
        if (this.exportButton != null) {
            this.exportButton.active = !fullscreen;
            this.exportButton.setTooltip(fullscreenTooltip);
        }
        if (this.applyButton != null) {
            this.applyButton.active = dirty;
        }
        if (this.doneButton != null) {
            this.doneButton.active = !dirty;
            this.doneButton.setTooltip(
                dirty ? Tooltip.create(DONE_UNSAVED_CHANGES) : null
            );
        }
    }

    private void showSaveError() {
        if (this.saveErrorWidget != null) {
            this.saveErrorWidget.setMessage(SAVE_FAILED);
        }
    }

    private void showImportSuccess(Path path) {
        if (this.minecraft != null) {
            CTransferToast.show(
                this.minecraft,
                CTransferToast.Kind.SUCCESS,
                IMPORT_SUCCESS_TOAST_TITLE,
                Component.translatable(IMPORT_SUCCESS_TOAST_DESCRIPTION_KEY, displayPath(path))
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

    private void clearSaveError() {
        if (this.saveErrorWidget != null) {
            this.saveErrorWidget.setMessage(Component.empty());
        }
    }

    private void openImportExceptionScreen(CConfigImportException exception) {
        switch (exception.reason()) {
            case IO_ERROR -> this.openImportMessageScreen(IMPORT_IO_ERROR_TITLE, IMPORT_IO_ERROR_BODY);
            case INVALID_CONFIG_FILE -> this.openImportMessageScreen(IMPORT_INVALID_TITLE, IMPORT_INVALID_BODY);
            case NO_COMPATIBLE_SETTINGS -> this.openImportMessageScreen(
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

    private void syncOptionWidgets() {
        if (this.checkForUpdatesButton != null) {
            this.checkForUpdatesButton.setValue(this.editSession.draft().checkForUpdates());
        }
        if (this.nowPlayingToastButton != null) {
            this.nowPlayingToastButton.setValue(this.editSession.draft().nowPlayingToastEnabled());
        }
        if (this.nowPlayingFeedbackButton != null) {
            this.nowPlayingFeedbackButton.setValue(this.editSession.draft().nowPlayingFeedbackMode());
        }
    }

    private static String displayPath(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private static Component feedbackModeLabel(CNowPlayingFeedbackMode mode) {
        return Component.translatable(switch (mode) {
            case CHAT -> "screen.custommusicclientsideplayer.mod_config.now_playing_feedback.chat";
            case OVERLAY -> "screen.custommusicclientsideplayer.mod_config.now_playing_feedback.overlay";
            case OFF -> "screen.custommusicclientsideplayer.mod_config.now_playing_feedback.off";
        });
    }

    private void returnToParent() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    private void extractSeparators(GuiGraphicsExtractor guiGraphics) {
        Identifier headerSeparator = this.minecraft.level == null
            ? HEADER_SEPARATOR
            : INWORLD_HEADER_SEPARATOR;
        Identifier footerSeparator = this.minecraft.level == null
            ? FOOTER_SEPARATOR
            : INWORLD_FOOTER_SEPARATOR;

        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            headerSeparator,
            0,
            this.layout.getHeaderHeight() - SEPARATOR_HEIGHT,
            0.0F,
            0.0F,
            this.width,
            SEPARATOR_HEIGHT,
            SEPARATOR_TEXTURE_WIDTH,
            SEPARATOR_HEIGHT
        );
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            footerSeparator,
            0,
            this.height - this.layout.getFooterHeight(),
            0.0F,
            0.0F,
            this.width,
            SEPARATOR_HEIGHT,
            SEPARATOR_TEXTURE_WIDTH,
            SEPARATOR_HEIGHT
        );
    }
}
