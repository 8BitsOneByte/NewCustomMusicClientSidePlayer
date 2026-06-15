package org.exmple.newcustommusicclientsideplayer.client.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;

public final class CSkipRandomCommand {
    private CSkipRandomCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal("skiprandom")
                        .executes(commandContext -> executeSkipRandom(commandContext.getSource()))
        ));
    }

    private static int executeSkipRandom(FabricClientCommandSource source) {
        CPlaySoundController.SkipResult result = CPlaySoundController.skipPlaylistRandom();
        return switch (result) {
            case SUCCESS -> 1;
            case TARGET_NOT_PLAYABLE -> {
                source.sendError(Component.translatable("command.custommusicclientsideplayer.skip.target_not_playable").withStyle(ChatFormatting.RED));
                yield 0;
            }
            case NOT_IN_PLAYLIST_MODE, OUT_OF_BOUNDARY -> {
                source.sendError(Component.translatable("command.custommusicclientsideplayer.skip.not_in_playlist_mode").withStyle(ChatFormatting.RED));
                yield 0;
            }
        };
    }
}
