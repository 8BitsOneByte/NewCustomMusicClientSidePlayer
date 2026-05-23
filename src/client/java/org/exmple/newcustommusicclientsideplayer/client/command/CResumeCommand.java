package org.exmple.newcustommusicclientsideplayer.client.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;

public final class CResumeCommand {
    private CResumeCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal("resume")
                        .executes(commandContext -> executeResume(commandContext.getSource()))
        ));
    }

    private static int executeResume(FabricClientCommandSource source) {
        CPlaySoundController.PauseResult result = CPlaySoundController.resumePaused();
        return switch (result) {
            case RESUMED -> {
                source.sendFeedback(Component.translatable("message.newcustommusicclientsideplayer.pause.resumed").withStyle(ChatFormatting.GREEN));
                yield 1;
            }
            case PAUSED, NO_PLAYBACK, NOT_PAUSED -> {
                source.sendError(Component.translatable("command.newcustommusicclientsideplayer.resume.no_paused_playback").withStyle(ChatFormatting.RED));
                yield 0;
            }
        };
    }
}
