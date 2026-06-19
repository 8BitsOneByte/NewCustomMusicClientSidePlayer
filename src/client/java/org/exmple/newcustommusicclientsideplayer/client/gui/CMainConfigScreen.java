package org.exmple.newcustommusicclientsideplayer.client.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;
import org.exmple.newcustommusicclientsideplayer.client.gui.update.CUpdateBadgeRenderer;
import org.exmple.newcustommusicclientsideplayer.client.gui.update.CUpdateTooltipBuilder;
import org.exmple.newcustommusicclientsideplayer.client.update.CUpdateChecker;
import org.exmple.newcustommusicclientsideplayer.client.update.CUpdateStatus;

public final class CMainConfigScreen extends Screen {
    private static final int SPACING = 8;
    private static final int BUTTON_WIDTH = 210;
    private static final int MODE_BUTTON_WIDTH = 150;
    private static final int LINK_BUTTON_WIDTH = MODE_BUTTON_WIDTH;
    private static final int MORE_BUTTON_WIDTH = MODE_BUTTON_WIDTH;

    private static final Component TITLE = Component.translatable("screen.custommusicclientsideplayer.main_menu.title");
    private static final Component MOD_NAME = Component.translatable("screen.custommusicclientsideplayer.main_menu.mod_name");
    private static final Component PLAY_MUSIC = Component.translatable("screen.custommusicclientsideplayer.main_menu.play_music");
    private static final Component MOD_LINKS = Component.translatable("screen.custommusicclientsideplayer.main_menu.mod_links");
    private static final Component SINGLETRACK_MODE = Component.translatable("screen.custommusicclientsideplayer.main_menu.singletrack_mode");
    private static final Component PLAYLIST_MODE = Component.translatable("screen.custommusicclientsideplayer.main_menu.playlist_mode");
    private static final Component SOURCE = Component.translatable("screen.custommusicclientsideplayer.main_menu.source");
    private static final Component REPORT_BUGS = Component.translatable("screen.custommusicclientsideplayer.main_menu.report_bugs");
    private static final Component MODRINTH = Component.translatable("screen.custommusicclientsideplayer.main_menu.modrinth");
    private static final Component GITHUB = Component.translatable("screen.custommusicclientsideplayer.main_menu.github");
    private static final Component MORE =
        Component.translatable("screen.custommusicclientsideplayer.main_menu.more");
    private static final Component MOD_CONFIGS =
        Component.translatable("screen.custommusicclientsideplayer.main_menu.mod_configs");
    private static final Component TIPS_AND_TRICKS =
        Component.translatable("screen.custommusicclientsideplayer.tips.title");

    private static final String SOURCE_URL = "https://github.com/8BitsOneByte/NewCustomMusicClientSidePlayer/tree/master";
    private static final String REPORT_BUGS_URL = "https://github.com/8BitsOneByte/NewCustomMusicClientSidePlayer/issues";
    private static final String MODRINTH_URL = "https://modrinth.com/mod/new-custom-music-client-side-player";
    private static final String GITHUB_URL = "https://github.com/8BitsOneByte";

    private final Screen parent;
    private HeaderAndFooterLayout layout;
    private Button modrinthButton;
    private CUpdateStatus updateStatus;

    public CMainConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.layout = new HeaderAndFooterLayout(this, 28, 20);

        GridLayout contentGrid = this.layout.addToContents(new GridLayout()).spacing(SPACING);
        contentGrid.defaultCellSetting().alignHorizontallyCenter();
        GridLayout.RowHelper adder = contentGrid.createRowHelper(2);

        adder.addChild(new StringWidget(this.modNameWithVersion(), this.font), 2);
        adder.addChild(new StringWidget(PLAY_MUSIC, this.font), 2);
        adder.addChild(Button.builder(SINGLETRACK_MODE, button -> this.onSingleTrackMode()).width(MODE_BUTTON_WIDTH).build());
        adder.addChild(Button.builder(PLAYLIST_MODE, button -> this.onPlaylistMode()).width(MODE_BUTTON_WIDTH).build());
        adder.addChild(new StringWidget(MOD_LINKS, this.font), 2);
        adder.addChild(Button.builder(SOURCE, ConfirmLinkScreen.confirmLink(this, SOURCE_URL)).width(LINK_BUTTON_WIDTH).build());
        adder.addChild(Button.builder(REPORT_BUGS, ConfirmLinkScreen.confirmLink(this, REPORT_BUGS_URL)).width(LINK_BUTTON_WIDTH).build());
        this.modrinthButton = Button.builder(MODRINTH, ConfirmLinkScreen.confirmLink(this, MODRINTH_URL)).width(LINK_BUTTON_WIDTH).build();
        this.synchronizeModrinthUpdateDisplay();
        adder.addChild(this.modrinthButton);
        adder.addChild(Button.builder(GITHUB, ConfirmLinkScreen.confirmLink(this, GITHUB_URL)).width(LINK_BUTTON_WIDTH).build());

        adder.addChild(new StringWidget(MORE, this.font), 2);
        adder.addChild(
            Button.builder(MOD_CONFIGS, button -> this.openModConfigScreen())
                .width(MORE_BUTTON_WIDTH)
                .build()
        );
        adder.addChild(
            Button.builder(TIPS_AND_TRICKS, button -> this.openTipsScreen())
                .width(MORE_BUTTON_WIDTH)
                .build()
        );
        adder.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(BUTTON_WIDTH).build(), 2);

        this.layout.arrangeElements();
        this.layout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.synchronizeModrinthUpdateDisplay();
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        CUpdateBadgeRenderer.render(guiGraphics, this.modrinthButton, this.updateStatus);
    }

    private void onSingleTrackMode() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(new CSingleTrackSelectScreen(this));
        }
    }

    private void onPlaylistMode() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(new CPlaylistConfigScreen(this));
        }
    }

    private void openModConfigScreen() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(new CModConfigScreen(this));
        }
    }

    private void openTipsScreen() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(new CTipsScreen(this));
        }
    }

    /**
     * Synchronizes the button before its render state is extracted so an asynchronous checker result,
     * or an Apply action that disables checks, affects the badge and tooltip in the same frame. Status
     * instances are immutable and replaced on transitions, so the tooltip is rebuilt only when the
     * checker publishes a different instance.
     */
    private void synchronizeModrinthUpdateDisplay() {
        CUpdateStatus currentStatus = CUpdateChecker.getStatus();
        if (currentStatus == this.updateStatus) {
            return;
        }

        this.updateStatus = currentStatus;
        switch (currentStatus.state()) {
            case UPDATE_AVAILABLE ->
                this.modrinthButton.setTooltip(CUpdateTooltipBuilder.build(currentStatus));
            case UP_TO_DATE, DISABLED, UNKNOWN, CHECK_FAILED ->
                this.modrinthButton.setTooltip(null);
        }
    }

    private Component modNameWithVersion() {
        String version = FabricLoader.getInstance()
            .getModContainer(NewcustommusicclientsideplayerClient.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("")
            .trim();
        if (version.isEmpty()) {
            return MOD_NAME;
        }

        return Component.empty().append(MOD_NAME).append(Component.literal(" " + version));
    }
}

