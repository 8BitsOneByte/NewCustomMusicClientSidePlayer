package org.exmple.newcustommusicclientsideplayer.client.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;

public final class CPauseCommand {
    private CPauseCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal("pause")
                        .executes(commandContext -> executePause(commandContext.getSource()))
        ));
    }

    private static int executePause(FabricClientCommandSource source) {
        CPlaySoundController.PauseResult result = CPlaySoundController.togglePause();
        return switch (result) {
            case PAUSED -> {
                source.sendFeedback(Component.translatable("message.newcustommusicclientsideplayer.pause.paused").withStyle(ChatFormatting.RED));
                yield 1;
            }
            case RESUMED -> {
                source.sendFeedback(Component.translatable("message.newcustommusicclientsideplayer.pause.resumed").withStyle(ChatFormatting.GREEN));
                yield 1;
            }
            case NO_PLAYBACK, NOT_PAUSED -> {
                source.sendError(Component.translatable("command.newcustommusicclientsideplayer.pause.no_playback").withStyle(ChatFormatting.RED));
                yield 0;
            }
        };
    }
}
