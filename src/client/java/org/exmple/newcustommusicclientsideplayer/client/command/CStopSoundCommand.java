package org.exmple.newcustommusicclientsideplayer.client.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;

public final class CStopSoundCommand {
    private CStopSoundCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            dispatcher.register(
                    ClientCommands.literal("cstopsound")
                            .executes(commandContext -> executeStop(commandContext.getSource()))
            );

            dispatcher.register(
                    ClientCommands.literal("stopsong")
                            .executes(commandContext -> executeStop(commandContext.getSource()))
            );
        });
    }

    private static int executeStop(FabricClientCommandSource source) {
        if (!CPlaySoundController.hasActivePlayback()) {
            source.sendError(Component.translatable("command.custommusicclientsideplayer.cstopsound.not_found").withStyle(ChatFormatting.RED));
            return 0;
        }

        CPlaySoundController.SessionMode mode = CPlaySoundController.getLastSessionMode();
        CPlaySoundController.stop();
        source.getPlayer().sendSystemMessage(buildStopMessage(mode));
        return 1;
    }

    private static MutableComponent buildStopMessage(CPlaySoundController.SessionMode mode) {
        return Component.empty()
                .append(getModeHeader(mode))
                .append(Component.literal("\n"))
                .append(Component.translatable("command.custommusicclientsideplayer.cstopsound.stopped").withStyle(ChatFormatting.RED));
    }

    private static MutableComponent getModeHeader(CPlaySoundController.SessionMode mode) {
        String text = switch (mode) {
            case PLAYLIST -> "mode.custommusicclientsideplayer.playlist";
            case SINGLE_TRACK -> "mode.custommusicclientsideplayer.singletrack";
            case NONE -> "mode.custommusicclientsideplayer.local";
        };
        return Component.translatable(text).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
    }
}
