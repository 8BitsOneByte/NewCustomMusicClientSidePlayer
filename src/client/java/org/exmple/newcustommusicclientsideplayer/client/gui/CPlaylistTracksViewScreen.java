package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameRepository;

public final class CPlaylistTracksViewScreen extends Screen {
    private static final Identifier MC_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/mc_mus_icon.png");
    private static final Identifier OTHER_MUS_ICON = Identifier.fromNamespaceAndPath(NewcustommusicclientsideplayerClient.MOD_ID, "textures/gui/other_mus_icon.png");
    private static final int COLOR_INCOMPATIBLE_ROW = -8978432;
    private static final Component UNKNOWN_NAMESPACE_TOOLTIP = Component.translatable("screen.custommusicclientsideplayer.common.unknown_namespace_track");
    private static final Component UNPLAYABLE_TRACK_TOOLTIP = Component.translatable("screen.custommusicclientsideplayer.common.unplayable_track");

    private final Screen parent;
    private final String playlistName;
    private final List<Identifier> tracks;
    private Set<String> availableNamespaces = Set.of();
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 8 + 9 + 8 + 20 + 4, 36);

    private TrackSelectionList trackSelectionList;
    private Button renameTrackButton;

    public CPlaylistTracksViewScreen(Screen parent, String playlistName, List<Identifier> tracks) {
        super(Component.translatable("screen.custommusicclientsideplayer.view_tracks.title"));
        this.parent = parent;
        this.playlistName = playlistName;
        this.tracks = new ArrayList<>(tracks);
    }

    @Override
    protected void init() {
        this.availableNamespaces = this.minecraft
            .getSoundManager()
            .getAvailableSounds()
            .stream()
            .map(Identifier::getNamespace)
            .collect(java.util.stream.Collectors.toSet());

        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title, this.font));
        header.addChild(
            new StringWidget(
                Component.translatable("screen.custommusicclientsideplayer.view_tracks.playlist", Component.literal(this.playlistName).withStyle(ChatFormatting.AQUA)),
                this.font
            )
        );

        this.trackSelectionList = this.layout.addToContents(new TrackSelectionList(this.minecraft, this, this.width, this.layout.getContentHeight()));

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.defaultCellSetting().alignHorizontallyCenter();
        this.renameTrackButton = footer.addChild(
            Button.builder(Component.translatable("screen.custommusicclientsideplayer.rename_track.button"), button -> this.onRenameSelectedTrack())
                .width(150)
                .build()
        );
        footer.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).width(150).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
        this.trackSelectionList.setTracks(this.tracks);
        this.updateButtonStates();
    }

    @Override
    protected void repositionElements() {
        if (this.trackSelectionList != null) {
            this.trackSelectionList.updateSize(this.width, this.layout);
        }

        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private static String getDisplayName(Identifier id) {
        return CTrackNameRepository.getDisplayName(id);
    }

    private void updateButtonStates() {
        if (this.renameTrackButton != null) {
            this.renameTrackButton.active = this.getSelectedTrack() != null;
        }
    }

    private Identifier getSelectedTrack() {
        if (this.trackSelectionList == null) {
            return null;
        }

        TrackSelectionList.Entry selected = this.trackSelectionList.getSelected();
        if (selected instanceof TrackSelectionList.TrackEntry trackEntry) {
            return trackEntry.track;
        }

        return null;
    }

    private void onRenameSelectedTrack() {
        Identifier selectedTrack = this.getSelectedTrack();
        if (selectedTrack == null || this.minecraft == null) {
            return;
        }

        LinkedHashSet<Identifier> sameNamespaceTracks = new LinkedHashSet<>();
        for (Identifier id : this.tracks) {
            if (id.getNamespace().equals(selectedTrack.getNamespace())) {
                sameNamespaceTracks.add(id);
            }
        }

        for (Identifier id : this.minecraft.getSoundManager().getAvailableSounds()) {
            if (id.getNamespace().equals(selectedTrack.getNamespace())) {
                sameNamespaceTracks.add(id);
            }
        }

        this.minecraft.gui.setScreen(
            new CRenameTrackScreen(this, selectedTrack, List.copyOf(sameNamespaceTracks), () -> this.trackSelectionList.setTracks(this.tracks))
        );
    }

    private static final class TrackSelectionList extends ObjectSelectionList<TrackSelectionList.Entry> {
        private final CPlaylistTracksViewScreen screen;

        private TrackSelectionList(Minecraft minecraft, CPlaylistTracksViewScreen screen, int width, int height) {
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

        private void setTracks(List<Identifier> tracks) {
            this.clearEntries();
            if (tracks.isEmpty()) {
                this.addEntry(new EmptyEntry(this.screen));
            } else {
                int index = 1;
                for (Identifier track : tracks) {
                    this.addEntry(new TrackEntry(this.screen, track, index++));
                }
            }

            this.setSelected(null);
            this.screen.updateButtonStates();
        }

        @Override
        public void setSelected(Entry entry) {
            super.setSelected(entry);
            this.screen.updateButtonStates();
        }

        private abstract static class Entry extends ObjectSelectionList.Entry<Entry> {
        }

        private static final class EmptyEntry extends Entry {
            private final CPlaylistTracksViewScreen screen;

            private EmptyEntry(CPlaylistTracksViewScreen screen) {
                this.screen = screen;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
                guiGraphics.centeredText(this.screen.font, Component.translatable("screen.custommusicclientsideplayer.view_tracks.empty"), this.getX() + this.getWidth() / 2, this.getContentYMiddle() - 4, 0xFF808080);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("screen.custommusicclientsideplayer.view_tracks.empty_narration");
            }
        }

        private static final class TrackEntry extends Entry {
            private final CPlaylistTracksViewScreen screen;
            private final Identifier track;
            private final int order;

            private TrackEntry(CPlaylistTracksViewScreen screen, Identifier track, int order) {
                this.screen = screen;
                this.track = track;
                this.order = order;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
                Font font = this.screen.font;
                boolean missingNamespace = !this.screen.availableNamespaces.contains(this.track.getNamespace());
                boolean unplayableTrack = !CPlaySoundController.isTrackPlayable(this.screen.minecraft, this.track);
                if (missingNamespace || unplayableTrack) {
                    guiGraphics.fill(this.getContentX() - 1, this.getContentY() - 1, this.getContentRight() + 1, this.getContentBottom() + 1, COLOR_INCOMPATIBLE_ROW);
                }

                Identifier icon = this.track.getNamespace().equals("minecraft") ? MC_MUS_ICON : OTHER_MUS_ICON;
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, this.getContentX(), this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);

                int textX = this.getContentX() + 34;
                guiGraphics.text(font, Component.translatable("screen.custommusicclientsideplayer.view_tracks.track_order", this.order, CPlaylistTracksViewScreen.getDisplayName(this.track)), textX, this.getContentY() + 1, 0xFFFFFFFF, false);
                guiGraphics.text(font, buildDescription(this.track), textX, this.getContentY() + 12, 0xFF808080, false);

                if (hovered) {
                    if (missingNamespace) {
                        guiGraphics.setTooltipForNextFrame(UNKNOWN_NAMESPACE_TOOLTIP, mouseX, mouseY);
                    } else if (unplayableTrack) {
                        guiGraphics.setTooltipForNextFrame(UNPLAYABLE_TRACK_TOOLTIP, mouseX, mouseY);
                    }
                }
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.track.toString());
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
                this.screen.trackSelectionList.setSelected(this);
                return true;
            }

            private static Component buildDescription(Identifier id) {
                return Component.translatable("screen.custommusicclientsideplayer.view_tracks.description", Component.literal(id.getNamespace()).withStyle(style -> style.withBold(true)));
            }
        }
    }
}

