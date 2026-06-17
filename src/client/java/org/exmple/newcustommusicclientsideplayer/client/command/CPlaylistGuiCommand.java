package org.exmple.newcustommusicclientsideplayer.client.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.exmple.newcustommusicclientsideplayer.client.gui.CPlaylistTestScreen;
//Legacy class only used for test purpose.
//It will never be used again, how sad.
@Deprecated
public final class CPlaylistGuiCommand {
    private CPlaylistGuiCommand() {
    }

    @SuppressWarnings("all")
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ignoredContext) -> dispatcher.register(
                ClientCommands.literal("cplaylistgui")
                        .executes(commandContext -> openGui(commandContext.getSource()))
        ));
    }

    private static int openGui(FabricClientCommandSource source) {
        Minecraft client = source.getClient();
        client.execute(() -> client.gui.setScreen(new CPlaylistTestScreen(client.gui.screen())));
        source.sendFeedback(Component.translatable("command.custommusicclientsideplayer.cplaylistgui.opened"));
        return 1;
    }
}
