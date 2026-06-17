package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.exmple.newcustommusicclientsideplayer.client.storage.CPlaylistRepository;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameRepository;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public class CPlaylistTestScreen extends Screen {
    private static final int LIST_WIDTH = 200;
    private static final int COLOR_TEXT_WHITE = -1;
    private static final int COLOR_TEXT_GRAY = -8355712;
    private static final int COLOR_INCOMPATIBLE_ROW = -8978432;
    private static final Component UNKNOWN_NAMESPACE_TOOLTIP = Component.translatable("screen.custommusicclientsideplayer.common.unknown_namespace_track");
    private static final Component UNPLAYABLE_TRACK_TOOLTIP = Component.translatable("screen.custommusicclientsideplayer.common.unplayable_track");
    private static final Component SEARCH_HINT = Component.translatable("screen.custommusicclientsideplayer.playlist_editor.search_sound_id").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final Component AVAILABLE_TITLE = Component.translatable("screen.custommusicclientsideplayer.playlist_editor.available_sounds");
    private static final Identifier MC_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/mc_mus_icon.png");
    private static final Identifier OTHER_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/other_mus_icon.png");
    private static final Identifier SELECT_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/select_highlighted");
    private static final Identifier SELECT_SPRITE = Identifier.withDefaultNamespace("transferable_list/select");
    private static final Identifier UNSELECT_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/unselect_highlighted");
    private static final Identifier UNSELECT_SPRITE = Identifier.withDefaultNamespace("transferable_list/unselect");
    private static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_up_highlighted");
    private static final Identifier MOVE_UP_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_up");
    private static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_down_highlighted");
    private static final Identifier MOVE_DOWN_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_down");

    private final Screen parent;
    private final SaveAction saveAction;
    private final Runnable onSavedSuccess;
    private final String playlistName;
    private final List<Identifier> initialPlaylistSnapshot;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private List<Identifier> allSounds = List.of();
    private List<String> namespaces = List.of("minecraft");
    private final List<Identifier> playlist = new ArrayList<>();
    private int namespaceIndex = 0;
    private String availableFilter = "";
    private EditBox searchBox;
    private Button nextNamespaceButton;
    private Button renamePlaylistButton;
    private IdentifierSelectionList availableSoundList;
    private IdentifierSelectionList playlistSoundList;

    @FunctionalInterface
    public interface SaveAction {
        void save(List<Identifier> playlist) throws Exception;
    }

    public CPlaylistTestScreen(Screen parent) {
        this(parent, "playlist", List.of(), playlist -> {
        });
    }

    public CPlaylistTestScreen(Screen parent, String playlistName, List<Identifier> initialPlaylist, SaveAction saveAction) {
        this(parent, playlistName, initialPlaylist, saveAction, () -> {
        });
    }

    public CPlaylistTestScreen(Screen parent, String playlistName, List<Identifier> initialPlaylist, SaveAction saveAction, Runnable onSavedSuccess) {
        super(Component.translatable("screen.custommusicclientsideplayer.playlist_editor.title"));
        this.parent = parent;
        this.playlistName = playlistName;
        this.saveAction = saveAction;
        this.onSavedSuccess = onSavedSuccess;
        this.playlist.addAll(initialPlaylist);
        this.initialPlaylistSnapshot = List.copyOf(initialPlaylist);
    }

    @Override
    protected void init() {
        this.layout.setHeaderHeight(36);
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.getTitle(), this.font));
        this.searchBox = header.addChild(new EditBox(this.font, 0, 0, LIST_WIDTH, 15, Component.empty()));
        this.searchBox.setHint(SEARCH_HINT);
        this.searchBox.setValue(this.availableFilter);
        this.searchBox.setResponder(value -> {
            this.availableFilter = value;
            this.refreshVisibleLists();
        });

        this.availableSoundList = this.layout.addToContents(new IdentifierSelectionList(this.minecraft, this, LIST_WIDTH, this.height - 66, AVAILABLE_TITLE, false));
        this.playlistSoundList = this.layout.addToContents(
            new IdentifierSelectionList(this.minecraft, this, LIST_WIDTH, this.height - 66, Component.translatable("screen.custommusicclientsideplayer.playlist_editor.playlist_header", this.playlistName), true)
        );

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.nextNamespaceButton = footer.addChild(Button.builder(Component.translatable("screen.custommusicclientsideplayer.playlist_editor.next_namespace"), button -> this.nextNamespace()).width(120).build());
        this.renamePlaylistButton = footer.addChild(Button.builder(Component.translatable("screen.custommusicclientsideplayer.rename_playlist.title"), button -> this.openRenameScreen()).width(130).build());
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onDoneClicked()).build());

        this.layout.visitWidgets(guiEventListener -> this.addRenderableWidget(guiEventListener));

        this.repositionElements();
        this.reloadLists();
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
        this.layout.arrangeElements();
        int contentHeight = this.layout.getContentHeight();
        int headerHeight = this.layout.getHeaderHeight();

        if (this.availableSoundList != null) {
            this.availableSoundList.updateSizeAndPosition(LIST_WIDTH, contentHeight, this.width / 2 - 15 - LIST_WIDTH, headerHeight);
        }

        if (this.playlistSoundList != null) {
            this.playlistSoundList.updateSizeAndPosition(LIST_WIDTH, contentHeight, this.width / 2 + 15, headerHeight);
        }

    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private void onDoneClicked() {
        if (this.minecraft == null) {
            return;
        }

        if (!this.hasPlaylistChanged()) {
            this.minecraft.gui.setScreen(this.parent);
            return;
        }

        List<Identifier> snapshot = List.copyOf(this.playlist);
        CompletableFuture.runAsync(() -> {
            try {
                this.saveAction.save(snapshot);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).whenComplete((unused, throwable) -> this.minecraft.execute(() -> {
            if (throwable != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.save_playlist_failed"));
                this.minecraft.gui.setScreen(this.parent);
                return;
            }

            this.minecraft.reloadResourcePacks().whenComplete((ignored, reloadThrowable) -> this.minecraft.execute(() -> {
                this.onSavedSuccess.run();
                this.minecraft.gui.setScreen(this.parent);
            }));
        }));
    }

    private boolean hasPlaylistChanged() {
        return !this.playlist.equals(this.initialPlaylistSnapshot);
    }

    private void openRenameScreen() {
        if (this.minecraft == null) {
            return;
        }

        this.minecraft.gui.setScreen(new CRenamePlaylistScreen(this, this.playlistName, this::onRenameConfirmed));
    }

    private void onRenameConfirmed(String newPlaylistName) {
        if (this.minecraft == null) {
            return;
        }

        List<Identifier> snapshot = List.copyOf(this.playlist);
        CompletableFuture.runAsync(() -> {
            try {
                CPlaylistRepository.savePlaylist(newPlaylistName, snapshot);
                if (!Objects.equals(newPlaylistName, this.playlistName)) {
                    CPlaylistRepository.deletePlaylist(this.playlistName);
                }
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }).whenComplete((unused, throwable) -> this.minecraft.execute(() -> {
            if (throwable != null) {
                if (this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(Component.translatable("screen.custommusicclientsideplayer.error.rename_playlist_failed"));
                }

                this.minecraft.gui.setScreen(this);
                return;
            }

            this.minecraft.reloadResourcePacks().whenComplete((ignored, reloadThrowable) -> this.minecraft.execute(() -> {
                this.onSavedSuccess.run();
                this.minecraft.gui.setScreen(this.parent);
            }));
        }));
    }


    private void reloadLists() {
        if (this.availableSoundList == null || this.playlistSoundList == null) {
            return;
        }

        Collection<Identifier> all = this.minecraft.getSoundManager().getAvailableSounds();
        this.allSounds = all.stream()
            .sorted(Comparator.comparing(Identifier::toString))
            .toList();
        this.namespaces = this.allSounds.stream().map(Identifier::getNamespace).distinct().sorted().toList();
        if (this.namespaces.isEmpty()) {
            this.namespaces = List.of("minecraft");
            this.namespaceIndex = 0;
        } else if (this.namespaceIndex >= this.namespaces.size()) {
            this.namespaceIndex = 0;
        }

        this.refreshAvailableList();
        this.refreshPlaylistList();
    }

    private void refreshVisibleLists() {
        this.refreshAvailableList();
        this.refreshPlaylistList();
    }

    private void refreshAvailableList() {
        if (this.availableSoundList == null || this.playlistSoundList == null) {
            return;
        }

        String namespace = this.getCurrentNamespace();
        this.availableSoundList.setTitle(Component.translatable("screen.custommusicclientsideplayer.playlist_editor.available_sounds_with_namespace", namespace));
        String filter = this.availableFilter.toLowerCase(Locale.ROOT);
        List<Identifier> available = this.allSounds.stream()
            .filter(id -> !this.playlist.contains(id))
            .filter(id -> id.getNamespace().equals(namespace))
            .filter(id -> filter.isBlank()
                || id.getPath().toLowerCase(Locale.ROOT).contains(filter)
                || CTrackNameRepository.getDisplayName(id).toLowerCase(Locale.ROOT).contains(filter))
            .toList();
        this.availableSoundList.setIdentifiers(available);
    }

    private void refreshPlaylistList() {
        if (this.playlistSoundList == null) {
            return;
        }

        this.playlistSoundList.setIdentifiers(List.copyOf(this.playlist));
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

    private void nextNamespace() {
        if (this.namespaces.isEmpty()) {
            return;
        }

        this.namespaceIndex = (this.namespaceIndex + 1) % this.namespaces.size();
        this.refreshAvailableList();
    }

    private void addToPlaylist(Identifier id) {
        if (!this.playlist.contains(id)) {
            this.playlist.add(id);
            this.refreshAvailableList();
            this.refreshPlaylistList();
        }
    }

    private void removeFromPlaylist(Identifier id) {
        if (this.playlist.remove(id)) {
            this.refreshAvailableList();
            this.refreshPlaylistList();
        }
    }

    private void moveUp(Identifier id) {
        int index = this.playlist.indexOf(id);
        if (index > 0) {
            Identifier prev = this.playlist.get(index - 1);
            this.playlist.set(index - 1, id);
            this.playlist.set(index, prev);
            this.refreshPlaylistList();
        }
    }

    private void moveDown(Identifier id) {
        int index = this.playlist.indexOf(id);
        if (index >= 0 && index < this.playlist.size() - 1) {
            Identifier next = this.playlist.get(index + 1);
            this.playlist.set(index + 1, id);
            this.playlist.set(index, next);
            this.refreshPlaylistList();
        }
    }

    private void playUiClickSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean canMoveUp(Identifier id) {
        return this.playlist.indexOf(id) > 0;
    }

    private boolean canMoveDown(Identifier id) {
        int idx = this.playlist.indexOf(id);
        return idx >= 0 && idx < this.playlist.size() - 1;
    }

    private static String getDisplayName(Identifier id) {
        return CTrackNameRepository.getDisplayName(id);
    }

    private static final class IdentifierSelectionList extends ObjectSelectionList<IdentifierSelectionList.Entry> {
        private final Font font;
        private final CPlaylistTestScreen screen;
        private Component title;
        private final boolean playlistSide;

        private IdentifierSelectionList(Minecraft minecraft, CPlaylistTestScreen screen, int width, int height, Component title, boolean playlistSide) {
            super(minecraft, width, height, 33, 36);
            this.font = minecraft.font;
            this.screen = screen;
            this.title = title;
            this.playlistSide = playlistSide;
            this.centerListVertically = false;
        }

        void setTitle(Component title) {
            this.title = title;
        }

        @Override
        public int getRowWidth() {
            return this.width - 4;
        }

        @Override
        protected int scrollBarX() {
            return this.getRight() - 6;
        }

        void setIdentifiers(List<Identifier> identifiers) {
            this.clearEntries();
            this.addEntry(new HeaderEntry(this.font, this.title), 14);

            if (identifiers.isEmpty()) {
                this.addEntry(new PlaceholderEntry(this.font, Component.translatable("screen.custommusicclientsideplayer.playlist_editor.empty")));
            } else {
                for (Identifier id : identifiers) {
                    this.addEntry(new SoundEntry(this.font, this, id));
                }
            }

            this.refreshScrollAmount();
            this.setSelected(null);
        }

        private abstract static class Entry extends ObjectSelectionList.Entry<Entry> {
        }

        private static final class HeaderEntry extends Entry {
            private final Font font;
            private final Component text;

            private HeaderEntry(Font font, Component text) {
                this.font = font;
                this.text = text;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
                guiGraphics.centeredText(this.font, this.text, this.getX() + this.getWidth() / 2, this.getContentYMiddle() - this.font.lineHeight / 2, COLOR_TEXT_WHITE);
            }

            @Override
            public Component getNarration() {
                return this.text;
            }
        }

        private static final class PlaceholderEntry extends Entry {
            private final Font font;
            private final Component text;

            private PlaceholderEntry(Font font, Component text) {
                this.font = font;
                this.text = text;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
                guiGraphics.centeredText(this.font, this.text, this.getX() + this.getWidth() / 2, this.getContentYMiddle() - this.font.lineHeight / 2, COLOR_TEXT_GRAY);
            }

            @Override
            public Component getNarration() {
                return this.text;
            }
        }

        private static final class SoundEntry extends Entry {
            private final Font font;
            private final IdentifierSelectionList list;
            private final Identifier id;

            private SoundEntry(Font font, IdentifierSelectionList list, Identifier id) {
                this.font = font;
                this.list = list;
                this.id = id;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
                String fileName = CPlaylistTestScreen.getDisplayName(this.id);
                boolean missingNamespace = this.list.playlistSide && this.list.screen.namespaces.stream().noneMatch(namespace -> namespace.equals(this.id.getNamespace()));
                boolean unplayableTrack = this.list.playlistSide && !CPlaySoundController.isTrackPlayable(this.list.minecraft, this.id);
                if (missingNamespace || unplayableTrack) {
                    guiGraphics.fill(this.getContentX() - 1, this.getContentY() - 1, this.getContentRight() + 1, this.getContentBottom() + 1, COLOR_INCOMPATIBLE_ROW);
                }

                Identifier icon = this.id.getNamespace().equals("minecraft") ? MC_MUS_ICON : OTHER_MUS_ICON;
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, this.getContentX(), this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);

                int textX = this.getContentX() + 34;
                guiGraphics.text(this.font, fileName, textX, this.getContentY() + 1, COLOR_TEXT_WHITE, false);
                guiGraphics.text(this.font, buildDescription(this.id), textX, this.getContentY() + 12, COLOR_TEXT_GRAY, false);

                if (hovered) {
                    if (missingNamespace) {
                        guiGraphics.setTooltipForNextFrame(UNKNOWN_NAMESPACE_TOOLTIP, mouseX, mouseY);
                    } else if (unplayableTrack) {
                        guiGraphics.setTooltipForNextFrame(UNPLAYABLE_TRACK_TOOLTIP, mouseX, mouseY);
                    }
                }

                boolean showHoverOverlay = !this.list.minecraft.getLastInputType().isMouse() || hovered || this.list.getSelected() == this && this.list.isFocused();
                if (showHoverOverlay) {
                    guiGraphics.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
                    int localX = mouseX - this.getContentX();
                    int localY = mouseY - this.getContentY();

                    if (!this.list.playlistSide) {
                        if (mouseOverIcon(localX, localY, 32)) {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SELECT_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                        } else {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SELECT_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                        }
                    } else {
                        if (mouseOverLeftHalf(localX, localY, 32)) {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, UNSELECT_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                        } else {
                            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, UNSELECT_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                        }

                        if (this.list.screen.canMoveUp(this.id)) {
                            if (mouseOverTopRightQuarter(localX, localY, 32)) {
                                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_UP_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                            } else {
                                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_UP_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                            }
                        }

                        if (this.list.screen.canMoveDown(this.id)) {
                            if (mouseOverBottomRightQuarter(localX, localY, 32)) {
                                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                            } else {
                                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                            }
                        }
                    }
                }
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.id.toString());
            }

            private static Component buildDescription(Identifier id) {
                return Component.translatable("screen.custommusicclientsideplayer.common.music_files_from", Component.literal(id.getNamespace()).withStyle(style -> style.withBold(true)));
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
                int localX = (int)mouseButtonEvent.x() - this.getContentX();
                int localY = (int)mouseButtonEvent.y() - this.getContentY();

                if (!this.list.playlistSide) {
                    if (mouseOverIcon(localX, localY, 32)) {
                        this.list.screen.playUiClickSound();
                        this.list.screen.addToPlaylist(this.id);
                        return true;
                    }
                } else {
                    if (mouseOverLeftHalf(localX, localY, 32)) {
                        this.list.screen.playUiClickSound();
                        this.list.screen.removeFromPlaylist(this.id);
                        return true;
                    }

                    if (this.list.screen.canMoveUp(this.id) && mouseOverTopRightQuarter(localX, localY, 32)) {
                        this.list.screen.playUiClickSound();
                        this.list.screen.moveUp(this.id);
                        return true;
                    }

                    if (this.list.screen.canMoveDown(this.id) && mouseOverBottomRightQuarter(localX, localY, 32)) {
                        this.list.screen.playUiClickSound();
                        this.list.screen.moveDown(this.id);
                        return true;
                    }
                }

                return super.mouseClicked(mouseButtonEvent, bl);
            }

            private static boolean mouseOverIcon(int x, int y, int size) {
                return x < size;
            }

            private static boolean mouseOverLeftHalf(int x, int y, int size) {
                return x < size / 2;
            }

            private static boolean mouseOverTopRightQuarter(int x, int y, int size) {
                return x > size / 2 && y < size / 2;
            }

            private static boolean mouseOverBottomRightQuarter(int x, int y, int size) {
                return x > size / 2 && y > size / 2;
            }
        }
    }
}
