package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.exmple.newcustommusicclientsideplayer.client.storage.CPlaylistImportPreview;

public final class CPlaylistImportWarningFormatter {
    private static final String WARNING_SKIPPED_PLAYLISTS_KEY =
        "screen.custommusicclientsideplayer.playlist_transfer.warning.skipped_playlists";
    private static final Component WARNING_UNREADABLE_TRACK_LISTS =
        Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.warning.unreadable_track_lists")
            .withStyle(ChatFormatting.GRAY);
    private static final Component WARNING_INVALID_TRACKS =
        Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.warning.invalid_tracks")
            .withStyle(ChatFormatting.GRAY);
    private static final String WARNING_EMPTY_TRACK_IDS_KEY =
        "screen.custommusicclientsideplayer.playlist_transfer.warning.empty_track_ids";
    private static final Component WARNING_INVALID_MODIFIED_AT =
        Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.warning.invalid_modified_at")
            .withStyle(ChatFormatting.GRAY);
    private static final Component WARNING_RENAMED_PLAYLISTS =
        Component.translatable("screen.custommusicclientsideplayer.playlist_transfer.warning.renamed_playlists")
            .withStyle(ChatFormatting.GRAY);

    private CPlaylistImportWarningFormatter() {
    }

    public static List<Component> format(CPlaylistImportPreview preview) {
        List<Component> items = new ArrayList<>();

        addSkippedPlaylists(items, preview);
        addUnreadableTrackLists(items, preview);
        addInvalidTracks(items, preview);
        addInvalidModifiedAt(items, preview);
        addRenamedPlaylists(items, preview);

        return items;
    }

    private static void addSkippedPlaylists(List<Component> items, CPlaylistImportPreview preview) {
        if (preview.skippedPlaylistCount() > 0) {
            items.add(Component.translatable(WARNING_SKIPPED_PLAYLISTS_KEY, preview.skippedPlaylistCount())
                .withStyle(ChatFormatting.GRAY));
        }
    }

    private static void addUnreadableTrackLists(List<Component> items, CPlaylistImportPreview preview) {
        if (preview.unreadableTrackListPlaylists().isEmpty()) {
            return;
        }

        items.add(WARNING_UNREADABLE_TRACK_LISTS);
        for (String playlistName : preview.unreadableTrackListPlaylists()) {
            items.add(playlistName(playlistName));
        }
    }

    private static void addInvalidTracks(List<Component> items, CPlaylistImportPreview preview) {
        if (preview.invalidTracksByPlaylist().isEmpty()) {
            return;
        }

        items.add(WARNING_INVALID_TRACKS);
        int emptyTrackIdCount = 0;
        for (Map.Entry<String, List<String>> entry : preview.invalidTracksByPlaylist().entrySet()) {
            items.add(playlistName(entry.getKey() + ":"));
            for (String track : entry.getValue()) {
                if (track.isEmpty()) {
                    emptyTrackIdCount++;
                    continue;
                }

                items.add(Component.literal(track).withStyle(ChatFormatting.RED));
            }
        }

        if (emptyTrackIdCount > 0) {
            items.add(Component.translatable(WARNING_EMPTY_TRACK_IDS_KEY, emptyTrackIdCount)
                .withStyle(ChatFormatting.RED));
        }
    }

    private static void addInvalidModifiedAt(List<Component> items, CPlaylistImportPreview preview) {
        if (preview.invalidModifiedAtPlaylists().isEmpty()) {
            return;
        }

        items.add(WARNING_INVALID_MODIFIED_AT);
        for (String playlistName : preview.invalidModifiedAtPlaylists()) {
            items.add(playlistName(playlistName));
        }
    }

    private static void addRenamedPlaylists(List<Component> items, CPlaylistImportPreview preview) {
        if (preview.renamedPlaylists().isEmpty()) {
            return;
        }

        items.add(WARNING_RENAMED_PLAYLISTS);
        for (CPlaylistImportPreview.Rename rename : preview.renamedPlaylists()) {
            MutableComponent item = Component.empty()
                .append(playlistName(rename.originalName()))
                .append(Component.literal(" -> ").withStyle(ChatFormatting.GRAY))
                .append(playlistName(rename.importedName()));
            items.add(item);
        }
    }

    private static Component playlistName(String name) {
        return Component.literal(name).withStyle(ChatFormatting.AQUA);
    }
}
