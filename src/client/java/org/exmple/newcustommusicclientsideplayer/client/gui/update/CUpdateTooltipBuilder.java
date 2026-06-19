package org.exmple.newcustommusicclientsideplayer.client.gui.update;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.exmple.newcustommusicclientsideplayer.client.update.CUpdatePolicy;
import org.exmple.newcustommusicclientsideplayer.client.update.CUpdateStatus;

public final class CUpdateTooltipBuilder {
    private static final String KEY_PREFIX = "screen.custommusicclientsideplayer.update.";

    private CUpdateTooltipBuilder() {
    }

    public static Tooltip build(CUpdateStatus status) {
        return Tooltip.create(toComponent(status));
    }

    private static Component toComponent(CUpdateStatus status) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY_PREFIX + "available").withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable(KEY_PREFIX + "latest_version", status.latestVersion()).withStyle(ChatFormatting.AQUA));
        if (!status.publishedAt().isBlank()) {
            lines.add(Component.translatable(KEY_PREFIX + "updated_at", formatPublishedAt(status.publishedAt())).withStyle(ChatFormatting.GOLD));
        }
        if (!status.changelogPreview().isBlank()) {
            lines.add(Component.translatable(KEY_PREFIX + "changelog").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal(changelogLine(status)).withStyle(ChatFormatting.GRAY));
        }

        MutableComponent tooltip = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                tooltip.append("\n");
            }
            tooltip.append(lines.get(i));
        }

        return tooltip;
    }

    private static String changelogLine(CUpdateStatus status) {
        return status.changelogPreview();
    }

    private static String formatPublishedAt(String publishedAt) {
        try {
            return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(Instant.parse(publishedAt));
        } catch (DateTimeParseException exception) {
            return publishedAt;
        }
    }
}
