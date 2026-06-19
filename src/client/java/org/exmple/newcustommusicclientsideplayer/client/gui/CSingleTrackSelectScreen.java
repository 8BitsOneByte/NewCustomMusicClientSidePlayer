package org.exmple.newcustommusicclientsideplayer.client.gui;

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
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
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
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameRepository;

public final class CSingleTrackSelectScreen extends Screen {
    private static final int LIST_WIDTH = 200;
    private static final int LIST_ROW_HEIGHT = 36;
    private static final int LIST_TOP_PADDING = 2;
    private static final int HEADER_ITEM_SPACING = 4;
    private static final int HEADER_VERTICAL_PADDING = 4;
    private static final int SEARCH_BOX_MIN_HEIGHT = 20;
    private static final int SEARCH_BOX_MAX_HEIGHT = LIST_ROW_HEIGHT * 2 / 3;
    private static final int NAMESPACE_SELECTOR_HEIGHT = 20;
    private static final int MAX_VISIBLE_NAMESPACES = 6;
    private static final int FOOTER_HEIGHT = 36;
    private static final int COLOR_TEXT_WHITE = -1;
    private static final int COLOR_TEXT_GRAY = -8355712;
    private static final int FOOTER_BUTTON_WIDTH = 120;
    private static final Component TITLE = Component.translatable("screen.custommusicclientsideplayer.singletrack.title");
    private static final Component SEARCH_HINT = Component.translatable("screen.custommusicclientsideplayer.playlist_editor.search_sound_id").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final Component PLAY_SELECTED_TEXT = Component.translatable("screen.custommusicclientsideplayer.singletrack.play_selected");
    private static final Component RENAME_TRACK_TEXT = Component.translatable("screen.custommusicclientsideplayer.rename_track.button");
    private static final Identifier MC_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/mc_mus_icon.png");
    private static final Identifier OTHER_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/other_mus_icon.png");

    private final Screen parent;
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
    private Button playSelectedTrackButton;
    private Button renameTrackButton;
    private int searchBoxHeight = SEARCH_BOX_MIN_HEIGHT;
    private boolean alignListToCompleteRows = true;
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
        this.searchBox = header.addChild(
            new EditBox(
                this.font,
                0,
                0,
                LIST_WIDTH,
                this.searchBoxHeight,
                Component.empty()
            )
        );
        this.searchBox.setHint(SEARCH_HINT);
        this.searchBox.setValue(this.filter);
        this.searchBox.setResponder(value -> {
            this.filter = value;
            this.refreshVisibleEntries();
        });

        this.soundSelectionList = this.layout.addToContents(new SoundSelectionList(this.minecraft, this, this.width, this.layout.getContentHeight()));

        LinearLayout footer = this.layout.addToFooter(
            LinearLayout.horizontal().spacing(8)
        );
        footer.defaultCellSetting().alignHorizontallyCenter();
        this.playSelectedTrackButton = footer.addChild(
            Button.builder(
                PLAY_SELECTED_TEXT,
                ignoredButton -> this.onPlaySelectedTrack()
            ).width(FOOTER_BUTTON_WIDTH).build()
        );
        this.renameTrackButton = footer.addChild(
            Button.builder(
                RENAME_TRACK_TEXT,
                ignoredButton -> this.onRenameSelectedTrack()
            ).width(FOOTER_BUTTON_WIDTH).build()
        );
        footer.addChild(
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
        boolean mouseCoveredByDropdown =
            this.dropdownCoordinator.isMouseOverExpandedOverlay(mouseX, mouseY);
        super.extractRenderState(
            guiGraphics,
            mouseCoveredByDropdown ? Integer.MIN_VALUE : mouseX,
            mouseCoveredByDropdown ? Integer.MIN_VALUE : mouseY,
            partialTick
        );
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

    /**
     * Absorbs the viewport remainder into the search box while that box remains
     * no taller than two thirds of one sound row. Once the maximum is reached,
     * the header is clamped and the list receives the remaining height even
     * when it cannot end on a complete row. This prevents small scaled
     * viewports from turning the search field into the dominant part of the
     * screen.
     */
    private void updateHeaderMetrics() {
        int fixedHeaderHeight = HEADER_VERTICAL_PADDING * 2
            + this.font.lineHeight
            + NAMESPACE_SELECTOR_HEIGHT
            + HEADER_ITEM_SPACING * 2;
        int minimumHeaderHeight = fixedHeaderHeight
            + SEARCH_BOX_MIN_HEIGHT;
        int completeRows = Math.max(
            1,
            (
                this.height
                    - FOOTER_HEIGHT
                    - minimumHeaderHeight
                    - LIST_TOP_PADDING
            ) / LIST_ROW_HEIGHT
        );
        int completeListHeight = LIST_TOP_PADDING
            + completeRows * LIST_ROW_HEIGHT;
        int headerHeight = Math.max(
            minimumHeaderHeight,
            this.height - FOOTER_HEIGHT - completeListHeight
        );
        int idealSearchBoxHeight = Math.max(
            SEARCH_BOX_MIN_HEIGHT,
            headerHeight - fixedHeaderHeight
        );

        this.searchBoxHeight = Math.min(
            idealSearchBoxHeight,
            SEARCH_BOX_MAX_HEIGHT
        );
        this.alignListToCompleteRows =
            idealSearchBoxHeight <= SEARCH_BOX_MAX_HEIGHT;
        headerHeight = fixedHeaderHeight + this.searchBoxHeight;
        this.layout.setHeaderHeight(headerHeight);
        if (this.searchBox != null) {
            this.searchBox.setHeight(this.searchBoxHeight);
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

        /**
         * Uses complete rows while the header can absorb the viewport remainder.
         * If the search box has reached its height cap, the list instead fills
         * all remaining space so preserving complete rows cannot enlarge the
         * search box beyond that cap.
         */
        @Override
        public void updateSize(int width, HeaderAndFooterLayout layout) {
            int availableHeight = layout.getContentHeight();
            if (!CSingleTrackSelectScreen.this.alignListToCompleteRows) {
                this.updateSizeAndPosition(
                    width,
                    availableHeight,
                    0,
                    layout.getHeaderHeight()
                );
                return;
            }

            int completeRows = Math.max(
                1,
                (availableHeight - LIST_TOP_PADDING) / LIST_ROW_HEIGHT
            );
            int listHeight = Math.min(
                availableHeight,
                LIST_TOP_PADDING + completeRows * LIST_ROW_HEIGHT
            );
            int unusedHeight = availableHeight - listHeight;
            int listY = layout.getHeaderHeight() + unusedHeight / 2;
            this.updateSizeAndPosition(width, listHeight, 0, listY);
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



