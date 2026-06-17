package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;
import org.exmple.newcustommusicclientsideplayer.client.gui.update.CUpdateBadgeRenderer;
import org.exmple.newcustommusicclientsideplayer.client.gui.update.CUpdateTooltipBuilder;
import org.exmple.newcustommusicclientsideplayer.client.update.CUpdateChecker;
import org.exmple.newcustommusicclientsideplayer.client.update.CUpdateStatus;

public final class CMainConfigScreen extends Screen {
    private static final String TIP_KEY_PREFIX = "screen.custommusicclientsideplayer.main_menu.tip.";
    private static final int[] INITIAL_TIP_BLACKLIST = {5, 7, 8};
    private static final int SPACING = 8;
    private static final int BUTTON_WIDTH = 210;
    private static final int HALF_BUTTON_WIDTH = 101;
    private static final int MODE_BUTTON_WIDTH = 150;
    private static final int LINK_BUTTON_WIDTH = MODE_BUTTON_WIDTH;
    private static final int TIP_CONTROL_BUTTON_WIDTH = HALF_BUTTON_WIDTH;

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
    private static final Component TIP_PREFIX = Component.translatable("screen.custommusicclientsideplayer.main_menu.tip_prefix").withStyle(ChatFormatting.GREEN);
    private static final Component PREVIOUS_TIP = Component.translatable("screen.custommusicclientsideplayer.main_menu.previous_tip");
    private static final Component RANDOM_TIP = Component.translatable("screen.custommusicclientsideplayer.main_menu.random_tip");
    private static final Component NEXT_TIP = Component.translatable("screen.custommusicclientsideplayer.main_menu.next_tip");

    private static final String SOURCE_URL = "https://github.com/8BitsOneByte/NewCustomMusicClientSidePlayer/tree/master";
    private static final String REPORT_BUGS_URL = "https://github.com/8BitsOneByte/NewCustomMusicClientSidePlayer/issues";
    private static final String MODRINTH_URL = "https://modrinth.com/mod/new-custom-music-client-side-player";
    private static final String GITHUB_URL = "https://github.com/8BitsOneByte";

    private final Screen parent;
    private HeaderAndFooterLayout layout;
    private MultiLineTextWidget tipWidget;
    private Button modrinthButton;
    private CUpdateStatus updateStatus;
    private List<Component> tips;
    private int tipIndex;

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
        this.applyModrinthUpdateTooltip();
        adder.addChild(this.modrinthButton);
        adder.addChild(Button.builder(GITHUB, ConfirmLinkScreen.confirmLink(this, GITHUB_URL)).width(LINK_BUTTON_WIDTH).build());

        this.tips = this.loadTips();
        this.tipIndex = this.pickInitialTipIndex();
        adder.addChild(new StringWidget(TIP_PREFIX, this.font), 2);
        this.tipWidget = adder.addChild(new MultiLineTextWidget(this.currentTip(), this.font).setCentered(true).setMaxWidth((int)(this.width * 0.85F)), 2);
        GridLayout tipButtons = adder.addChild(new GridLayout().spacing(6), 2);
        tipButtons.defaultCellSetting().alignHorizontallyCenter();
        GridLayout.RowHelper tipButtonRow = tipButtons.createRowHelper(3);
        tipButtonRow.addChild(Button.builder(PREVIOUS_TIP, button -> this.showPreviousTip()).width(TIP_CONTROL_BUTTON_WIDTH).build());
        tipButtonRow.addChild(Button.builder(RANDOM_TIP, button -> this.rollRandomTip()).width(TIP_CONTROL_BUTTON_WIDTH).build());
        tipButtonRow.addChild(Button.builder(NEXT_TIP, button -> this.showNextTip()).width(TIP_CONTROL_BUTTON_WIDTH).build());
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
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        this.refreshModrinthUpdateStatus();
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

    private void applyModrinthUpdateTooltip() {
        this.refreshModrinthUpdateStatus();
        if (this.updateStatus.updateAvailable()) {
            this.modrinthButton.setTooltip(CUpdateTooltipBuilder.build(this.updateStatus));
        }
    }

    private void refreshModrinthUpdateStatus() {
        this.updateStatus = CUpdateChecker.getStatus();
    }

    private void rollRandomTip() {
        if (this.tips.size() > 1) {
            int nextIndex = this.tipIndex;
            while (nextIndex == this.tipIndex) {
                nextIndex = ThreadLocalRandom.current().nextInt(this.tips.size());
            }
            this.tipIndex = nextIndex;
        } else if (this.tips.size() == 1) {
            this.tipIndex = 0;
        }
        this.tipWidget.setMessage(this.currentTip());
        this.layout.arrangeElements();
    }

    private void showPreviousTip() {
        if (this.tips.isEmpty()) {
            this.tipWidget.setMessage(Component.literal(" "));
            this.layout.arrangeElements();
            return;
        }

        this.tipIndex = Math.floorMod(this.tipIndex - 1, this.tips.size());
        this.tipWidget.setMessage(this.currentTip());
        this.layout.arrangeElements();
    }

    private void showNextTip() {
        if (this.tips.isEmpty()) {
            this.tipWidget.setMessage(Component.literal(" "));
            this.layout.arrangeElements();
            return;
        }

        this.tipIndex = Math.floorMod(this.tipIndex + 1, this.tips.size());
        this.tipWidget.setMessage(this.currentTip());
        this.layout.arrangeElements();
    }

    private Component currentTip() {
        if (this.tips.isEmpty()) {
            return Component.literal(" ");
        }

        return this.tips.get(this.tipIndex);
    }

    private List<Component> loadTips() {
        List<Component> loaded = new ArrayList<>();
        Language language = Language.getInstance();
        for (int i = 1; ; i++) {
            String key = TIP_KEY_PREFIX + i;
            if (!language.has(key)) {
                break;
            }

            loaded.add(Component.translatable(key));
        }
        if (loaded.isEmpty()) {
            loaded.add(Component.literal(" "));
        }

        return loaded;
    }

    private int pickInitialTipIndex() {
        if (this.tips.isEmpty()) {
            return 0;
        }

        List<Integer> allowed = new ArrayList<>();
        for (int i = 0; i < this.tips.size(); i++) {
            int tipNumber = i + 1;
            boolean blacklisted = false;
            for (int forbidden : INITIAL_TIP_BLACKLIST) {
                if (forbidden == tipNumber) {
                    blacklisted = true;
                    break;
                }
            }

            if (!blacklisted) {
                allowed.add(i);
            }
        }

        if (allowed.isEmpty()) {
            return ThreadLocalRandom.current().nextInt(this.tips.size());
        }

        return allowed.get(ThreadLocalRandom.current().nextInt(allowed.size()));
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

