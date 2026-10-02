package org.exmple.newcustommusicclientsideplayer.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenCancellationMixin {
    @Inject(method = "lambda$init$0", at = @At("HEAD"))
    private void newcustommusicclientsideplayer$applyCancelledConnectionPolicy(Button button, CallbackInfo callbackInfo) {
        CPlaySoundController.onWorldDisconnect(Minecraft.getInstance(), false);
    }
}
