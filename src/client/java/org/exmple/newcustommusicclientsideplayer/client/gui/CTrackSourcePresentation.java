package org.exmple.newcustommusicclientsideplayer.client.gui;

import java.util.Collection;
import java.util.Comparator;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.audio.catalog.CAudioCatalog;
import org.exmple.newcustommusicclientsideplayer.client.audio.catalog.COggTrackNames;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackNameRepository;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackSourceType;

final class CTrackSourcePresentation {
    private CTrackSourcePresentation() { }

    static Collection<Identifier> resources(Minecraft client, CTrackSourceType type) {
        return type == CTrackSourceType.OGG_RESOURCE ? CAudioCatalog.getInstance().resources()
            : client.getSoundManager().getAvailableSounds();
    }

    static Comparator<Identifier> order(CTrackSourceType type) {
        return type == CTrackSourceType.OGG_RESOURCE ? COggTrackNames::compare : Comparator.comparing(Identifier::toString);
    }

    static int nameColor(CTrackSourceType type, Identifier id) {
        return CTrackNameRepository.getCustomName(type, id) == null ? 0xFFFFFFFF : 0xFFFFFF55;
    }

    static Pattern searchPattern(String query) {
        return query.isBlank() ? null : Pattern.compile("(?=(" + Pattern.quote(query) + "))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    static boolean matches(Pattern pattern, String text) {
        return pattern == null || pattern.matcher(text).find();
    }

    static Component highlight(String text, Pattern pattern) {
        if (pattern == null) return Component.literal(text);
        boolean[] highlighted = new boolean[text.length()];
        var matches = pattern.matcher(text);
        while (matches.find()) {
            java.util.Arrays.fill(highlighted, matches.start(1), matches.end(1), true);
        }
        MutableComponent result = Component.empty();
        int start = 0;
        while (start < text.length()) {
            int end = start + 1;
            while (end < text.length() && highlighted[end] == highlighted[start]) end++;
            MutableComponent part = Component.literal(text.substring(start, end));
            if (highlighted[start]) part.withStyle(ChatFormatting.GREEN);
            result.append(part);
            start = end;
        }
        return result;
    }

    static Component description(CTrackSourceType type, Identifier id) {
        return Component.translatable("screen.custommusicclientsideplayer.common."
            + (type == CTrackSourceType.OGG_RESOURCE ? "music_file_from" : "sound_event_from"),
            Component.literal(id.getNamespace()).withStyle(ChatFormatting.BOLD));
    }

    static void header(GuiGraphicsExtractor graphics, Minecraft client, CTrackSourceType type) {
        graphics.text(client.font, Component.translatable("screen.custommusicclientsideplayer.singletrack.track_source"),
            8, 8, -1, false);
        boolean ogg = type == CTrackSourceType.OGG_RESOURCE;
        graphics.text(client.font, Component.translatable("screen.custommusicclientsideplayer.mod_config.track_source."
            + (ogg ? "ogg_files" : "sound_events")).withStyle(ogg ? ChatFormatting.GOLD : ChatFormatting.GREEN),
            8, 8 + client.font.lineHeight + 2, -1, false);
    }
}
