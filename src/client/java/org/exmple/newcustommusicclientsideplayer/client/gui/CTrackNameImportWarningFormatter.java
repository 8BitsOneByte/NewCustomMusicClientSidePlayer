package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameImportPreview;

public final class CTrackNameImportWarningFormatter {
    private static final String KEY_PREFIX =
        "screen.custommusicclientsideplayer.track_name_transfer.warning.";

    private CTrackNameImportWarningFormatter() {
    }

    public static List<Component> format(CTrackNameImportPreview preview) {
        List<Component> items = new ArrayList<>();
        addInvalidTrackIds(items, preview);
        addDuplicateTrackIds(items, preview);
        addNonStringNames(items, preview);
        addEmptyNames(items, preview);
        addTooLongNames(items, preview);
        addDefaultNames(items, preview);
        addNameConflicts(items, preview);
        addReplacements(items, preview);
        return items;
    }

    private static void addInvalidTrackIds(List<Component> items, CTrackNameImportPreview preview) {
        if (preview.invalidTrackIds().isEmpty()) {
            return;
        }
        items.add(heading("invalid_track_ids", preview.invalidTrackIds().size()));
        for (String rawTrackId : preview.invalidTrackIds()) {
            items.add(Component.literal(rawTrackId).withStyle(ChatFormatting.RED));
        }
    }

    private static void addDuplicateTrackIds(List<Component> items, CTrackNameImportPreview preview) {
        if (preview.duplicateTrackIds().isEmpty()) {
            return;
        }
        items.add(heading("duplicate_track_ids", preview.duplicateTrackIds().size()));
        for (Identifier trackId : preview.duplicateTrackIds()) {
            items.add(trackId(trackId));
        }
    }

    private static void addNonStringNames(List<Component> items, CTrackNameImportPreview preview) {
        if (preview.nonStringNameTrackIds().isEmpty()) {
            return;
        }
        items.add(heading("non_string_names", preview.nonStringNameTrackIds().size()));
        for (Identifier trackId : preview.nonStringNameTrackIds()) {
            items.add(trackId(trackId));
        }
    }

    private static void addEmptyNames(List<Component> items, CTrackNameImportPreview preview) {
        if (preview.emptyNameTrackIds().isEmpty()) {
            return;
        }
        items.add(heading("empty_names", preview.emptyNameTrackIds().size()));
        for (Identifier trackId : preview.emptyNameTrackIds()) {
            items.add(trackId(trackId));
        }
    }

    private static void addTooLongNames(List<Component> items, CTrackNameImportPreview preview) {
        if (preview.tooLongNames().isEmpty()) {
            return;
        }
        items.add(heading("too_long_names", preview.tooLongNames().size()));
        for (CTrackNameImportPreview.TrackName trackName : preview.tooLongNames()) {
            items.add(trackName(trackName));
        }
    }

    private static void addDefaultNames(List<Component> items, CTrackNameImportPreview preview) {
        if (preview.defaultNames().isEmpty()) {
            return;
        }
        items.add(heading("default_names", preview.defaultNames().size()));
        for (CTrackNameImportPreview.TrackName trackName : preview.defaultNames()) {
            items.add(trackName(trackName));
        }
    }

    private static void addNameConflicts(List<Component> items, CTrackNameImportPreview preview) {
        if (preview.nameConflicts().isEmpty()) {
            return;
        }
        items.add(heading("name_conflicts", preview.nameConflicts().size()));
        for (CTrackNameImportPreview.NameConflict conflict : preview.nameConflicts()) {
            items.add(Component.empty()
                .append(trackName(conflict.importedTrack()))
                .append(Component.literal(" ↔ ").withStyle(ChatFormatting.RED))
                .append(trackName(conflict.conflictingTrack())));
        }
    }

    private static void addReplacements(List<Component> items, CTrackNameImportPreview preview) {
        if (preview.replacements().isEmpty()) {
            return;
        }
        items.add(heading("replaced_names", preview.replacements().size()));
        for (CTrackNameImportPreview.Replacement replacement : preview.replacements()) {
            items.add(Component.empty()
                .append(trackId(replacement.trackId()))
                .append(Component.literal("  \"").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(replacement.oldName()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("\" → \"").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(replacement.newName()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("\"").withStyle(ChatFormatting.GRAY)));
        }
    }

    private static Component heading(String suffix, int count) {
        return Component.translatable(KEY_PREFIX + suffix, count).withStyle(ChatFormatting.GRAY);
    }

    private static MutableComponent trackName(CTrackNameImportPreview.TrackName trackName) {
        return Component.empty()
            .append(trackId(trackName.trackId()))
            .append(Component.literal("  \"").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(trackName.name()).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("\"").withStyle(ChatFormatting.GRAY));
    }

    private static MutableComponent trackId(Identifier trackId) {
        return Component.empty()
            .append(Component.literal(trackId.getNamespace()).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(":").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(trackId.getPath()).withStyle(ChatFormatting.AQUA));
    }
}
