package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
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

public final class CSingleTrackSelectScreen extends Screen {
    private static final int LIST_WIDTH = 200;
    private static final int COLOR_TEXT_WHITE = -1;
    private static final int COLOR_TEXT_GRAY = -8355712;
    private static final int FOOTER_BUTTON_WIDTH = 120;
    private static final Component TITLE = Component.translatable("screen.custommusicclientsideplayer.singletrack.title");
    private static final Component SEARCH_HINT = Component.translatable("screen.custommusicclientsideplayer.playlist_editor.search_sound_id").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final Component NEXT_NAMESPACE_TEXT = Component.translatable("screen.custommusicclientsideplayer.playlist_editor.next_namespace");
    private static final Component PLAY_SELECTED_TEXT = Component.translatable("screen.custommusicclientsideplayer.singletrack.play_selected");
    private static final Component RENAME_TRACK_TEXT = Component.translatable("screen.custommusicclientsideplayer.rename_track.button");
    private static final Identifier MC_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/mc_mus_icon.png");
    private static final Identifier OTHER_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/other_mus_icon.png");

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 8 + 9 + 8 + 20 + 4, 54);
    private final List<Identifier> allSounds = new ArrayList<>();
    private List<String> namespaces = List.of("minecraft");
    private int namespaceIndex;

    private SoundSelectionList soundSelectionList;
    private MultiLineTextWidget namespaceWidget;
    private Button playSelectedTrackButton;
    private Button renameTrackButton;
    private String filter = "";

    public CSingleTrackSelectScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title, this.font));
        this.namespaceWidget = header.addChild(new MultiLineTextWidget(this.namespaceComponent(), this.font).setCentered(true).setMaxWidth((int)(this.width * 0.8F)));
        EditBox searchBox = header.addChild(new EditBox(this.font, 0, 0, LIST_WIDTH, 15, Component.empty()));
        searchBox.setHint(SEARCH_HINT);
        searchBox.setValue(this.filter);
        searchBox.setResponder(value -> {
            this.filter = value;
            this.refreshVisibleEntries();
        });

        this.soundSelectionList = this.layout.addToContents(new SoundSelectionList(this.minecraft, this, this.width, this.layout.getContentHeight()));

        GridLayout footer = this.layout.addToFooter(new GridLayout().columnSpacing(8));
        footer.defaultCellSetting().alignHorizontallyCenter();
        footer.rowSpacing(4);
        GridLayout.RowHelper rowHelper = footer.createRowHelper(2);
        rowHelper.addChild(Button.builder(NEXT_NAMESPACE_TEXT, ignoredButton -> this.nextNamespace()).width(FOOTER_BUTTON_WIDTH).build());
        this.playSelectedTrackButton = rowHelper.addChild(Button.builder(PLAY_SELECTED_TEXT, ignoredButton -> this.onPlaySelectedTrack()).width(FOOTER_BUTTON_WIDTH).build());
        this.renameTrackButton = rowHelper.addChild(Button.builder(RENAME_TRACK_TEXT, ignoredButton -> this.onRenameSelectedTrack()).width(FOOTER_BUTTON_WIDTH).build());
        rowHelper.addChild(Button.builder(CommonComponents.GUI_BACK, ignoredButton -> this.onClose()).width(FOOTER_BUTTON_WIDTH).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        if (this.soundSelectionList != null) {
            this.soundSelectionList.updateSize(this.width, this.layout);
        }

        this.layout.arrangeElements();
        this.reloadSounds();
        this.updateButtonStates();
        this.setInitialFocus(searchBox);
    }

    @Override
    protected void repositionElements() {
        if (this.soundSelectionList != null) {
            this.soundSelectionList.updateSize(this.width, this.layout);
        }

        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    @SuppressWarnings("all")
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void reloadSounds() {
        if (this.minecraft.player == null) {
            return;
        }

        Collection<Identifier> available = this.minecraft.getSoundManager().getAvailableSounds();
        this.allSounds.clear();
        this.allSounds.addAll(available.stream().sorted(Comparator.comparing(Identifier::toString)).toList());
        this.namespaces = this.allSounds.stream().map(Identifier::getNamespace).distinct().sorted().toList();
        if (this.namespaces.isEmpty()) {
            this.namespaces = List.of("minecraft");
            this.namespaceIndex = 0;
        } else if (this.namespaceIndex >= this.namespaces.size()) {
            this.namespaceIndex = 0;
        }

        this.refreshNamespaceLabel();
        this.refreshVisibleEntries();
    }

    private void refreshNamespaceLabel() {
        if (this.namespaceWidget != null) {
            this.namespaceWidget.setMessage(this.namespaceComponent());
            this.layout.arrangeElements();
        }
    }

    private Component namespaceComponent() {
        return Component.translatable(
            "screen.custommusicclientsideplayer.singletrack.namespace",
            Component.literal(this.getCurrentNamespace()).withStyle(ChatFormatting.AQUA)
        );
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
        boolean hasSelection = this.getSelectedSound() != null;
        if (this.playSelectedTrackButton != null) {
            this.playSelectedTrackButton.active = hasSelection;
        }

        if (this.renameTrackButton != null) {
            this.renameTrackButton.active = hasSelection;
        }
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

    private void nextNamespace() {
        if (this.namespaces.isEmpty()) {
            return;
        }

        this.namespaceIndex = (this.namespaceIndex + 1) % this.namespaces.size();
        this.refreshNamespaceLabel();
        this.refreshVisibleEntries();
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

        this.minecraft.setScreen(
            new CSingleTrackPlayConfirmScreen(this, selected, (loop, pitch) -> {
                this.minecraft.setScreen(null);
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
        this.minecraft.setScreen(new CRenameTrackScreen(this, selected, sameNamespaceTracks, this::refreshVisibleEntries));
    }

    private void playUiClickSound() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    static String getDisplayName(Identifier id) {
        return CTrackNameRepository.getDisplayName(id);
    }

    private final class SoundSelectionList extends ObjectSelectionList<SoundSelectionList.Entry> {
        private SoundSelectionList(Minecraft minecraft, CSingleTrackSelectScreen ignoredScreen, int width, int height) {
            super(minecraft, width, height, 0, 36);
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

                boolean showHoverOverlay = this.list.minecraft.options.touchscreen().get() || hovered || this.list.getSelected() == this && this.list.isFocused();
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



