package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.io.IOException;
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
import org.exmple.newcustommusicclientsideplayer.client.config.CModConfigEditSession;
import org.exmple.newcustommusicclientsideplayer.client.config.CModConfigRepository;
import org.exmple.newcustommusicclientsideplayer.client.update.CUpdateChecker;

public final class CModConfigScreen extends Screen {
    private static final Component TITLE =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.title");
    private static final Component CHECK_FOR_UPDATES =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.check_for_updates");
    private static final Component APPLY =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.apply");
    private static final Component SAVE_FAILED =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.save_failed")
            .withStyle(ChatFormatting.RED);
    private static final Component DONE_UNSAVED_CHANGES =
        Component.translatable("screen.custommusicclientsideplayer.mod_config.done_unsaved_changes");

    private static final int HEADER_HEIGHT = 33;
    private static final int FOOTER_HEIGHT = 36;
    private static final int OPTION_WIDTH = 150;
    private static final int FOOTER_BUTTON_WIDTH = 100;
    private static final int SPACING = 8;
    private static final int FOOTER_GROUP_SPACING = 24;
    private static final int CONTENT_WIDTH = OPTION_WIDTH * 2 + SPACING;
    private static final int SEPARATOR_HEIGHT = 2;
    private static final int SEPARATOR_TEXTURE_WIDTH = 32;

    private final Screen parent;
    private final CModConfigRepository repository;
    private final CModConfigEditSession editSession;
    private final HeaderAndFooterLayout layout =
        new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);

    private StringWidget saveErrorWidget;
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
        rows.addChild(
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
        footer.addChild(
            Button.builder(CommonComponents.GUI_BACK, button -> this.onBack())
                .width(FOOTER_BUTTON_WIDTH)
                .build()
        );
        LinearLayout applyActions = footer.addChild(
            LinearLayout.horizontal().spacing(SPACING)
        );
        this.applyButton = applyActions.addChild(
            Button.builder(APPLY, button -> this.onApply())
                .width(FOOTER_BUTTON_WIDTH)
                .build()
        );
        this.doneButton = applyActions.addChild(
            Button.builder(CommonComponents.GUI_DONE, button -> this.onDone())
                .width(FOOTER_BUTTON_WIDTH)
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

        CUpdateChecker.applyCheckForUpdates(this.editSession.draft().checkForUpdates());
        this.editSession.applyDraft();
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

    private void clearSaveError() {
        if (this.saveErrorWidget != null) {
            this.saveErrorWidget.setMessage(Component.empty());
        }
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
