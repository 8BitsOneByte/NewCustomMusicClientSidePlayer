package org.exmple.newcustommusicclientsideplayer.client.command;

import java.io.IOException;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;
import org.exmple.newcustommusicclientsideplayer.client.config.CModConfigRepository;
import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackSourceType;

public final class CTrackSourceCommand {
    private CTrackSourceCommand() { }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
            ClientCommands.literal("tracksource")
                .executes(command -> report(command.getSource(), "current", current()))
                .then(ClientCommands.literal("sound_events").executes(command -> change(command.getSource(), CTrackSourceType.SOUND_EVENT)))
                .then(ClientCommands.literal("ogg_files").executes(command -> change(command.getSource(), CTrackSourceType.OGG_RESOURCE)))
        ));
    }

    private static CTrackSourceType current() {
        return NewcustommusicclientsideplayerClient.getModConfig().defaultTrackSource();
    }

    private static int change(FabricClientCommandSource source, CTrackSourceType type) {
        if (current() == type) return report(source, "already", type);
        var config = NewcustommusicclientsideplayerClient.getModConfig().withDefaultTrackSource(type);
        try {
            new CModConfigRepository().save(config);
        } catch (IOException | RuntimeException exception) {
            source.sendError(Component.translatable("screen.custommusicclientsideplayer.mod_config.save_failed"));
            return 0;
        }
        NewcustommusicclientsideplayerClient.applyModConfig(config);
        return report(source, "switched", type);
    }

    private static int report(FabricClientCommandSource source, String suffix, CTrackSourceType type) {
        boolean ogg = type == CTrackSourceType.OGG_RESOURCE;
        Component label = Component.translatable("screen.custommusicclientsideplayer.mod_config.track_source."
            + (ogg ? "ogg_files" : "sound_events")).withStyle(ogg ? ChatFormatting.GOLD : ChatFormatting.GREEN);
        source.sendFeedback(Component.translatable("command.custommusicclientsideplayer.track_source." + suffix, label)
            .withStyle(ChatFormatting.WHITE));
        return 1;
    }
}
