package org.exmple.newcustommusicclientsideplayer.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiDisconnectPolicyMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void newcustommusicclientsideplayer$finishFailedConnection(Screen screen, CallbackInfo callbackInfo) {
        // Login and address-resolution failures can show this screen without world teardown.
        if (screen instanceof DisconnectedScreen && minecraft.level == null
                && !CPlaySoundController.isServerTransferTeardown()) {
            CPlaySoundController.onWorldDisconnect(minecraft, false);
        }
    }
}
