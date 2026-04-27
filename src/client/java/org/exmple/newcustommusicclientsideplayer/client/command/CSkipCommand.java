package org.exmple.newcustommusicclientsideplayer.client.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;

public final class CSkipCommand {
    private CSkipCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal("skip")
                        .executes(commandContext -> executeSkip(commandContext.getSource(), 1))
                        .then(
                                ClientCommands.argument("offset", IntegerArgumentType.integer())
                                        .executes(commandContext -> executeSkip(commandContext.getSource(), IntegerArgumentType.getInteger(commandContext, "offset")))
                        )
        ));
    }

    private static int executeSkip(FabricClientCommandSource source, int offset) {
        if (!CPlaySoundController.isPlaylistModeActive()) {
            source.sendError(Component.translatable("command.custommusicclientsideplayer.skip.not_in_playlist_mode").withStyle(ChatFormatting.RED));
            return 0;
        }

        CPlaySoundController.SkipResult result = CPlaySoundController.skipPlaylistBy(offset);
        return switch (result) {
            case SUCCESS -> 1;
            case OUT_OF_BOUNDARY -> {
                source.sendError(Component.translatable("command.custommusicclientsideplayer.skip.out_of_boundary").withStyle(ChatFormatting.RED));
                yield 0;
            }
            case TARGET_NOT_PLAYABLE -> {
                source.sendError(Component.translatable("command.custommusicclientsideplayer.skip.target_not_playable").withStyle(ChatFormatting.RED));
                yield 0;
            }
            case SWITCH_LOCKED -> {
                source.sendError(Component.translatable("command.custommusicclientsideplayer.skip.switch_locked").withStyle(ChatFormatting.RED));
                yield 0;
            }
            case NOT_IN_PLAYLIST_MODE -> {
                source.sendError(Component.translatable("command.custommusicclientsideplayer.skip.not_in_playlist_mode").withStyle(ChatFormatting.RED));
                yield 0;
            }
        };
    }
}
