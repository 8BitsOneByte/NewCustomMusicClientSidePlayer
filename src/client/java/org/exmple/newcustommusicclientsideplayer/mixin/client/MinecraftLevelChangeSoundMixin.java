package org.exmple.newcustommusicclientsideplayer.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CLevelChangeSoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftLevelChangeSoundMixin {
    @Shadow
    @Final
    private SoundManager soundManager;

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    private void newcustommusicclientsideplayer$applyDisconnectPolicy(
            Screen screen, boolean transferring, boolean stopSounds, CallbackInfo callbackInfo
    ) {
        Minecraft client = (Minecraft) (Object) this;
        // Connecting to a destination also calls disconnect with an already empty world.
        if (client.level != null || screen instanceof DisconnectedScreen) {
            CPlaySoundController.onWorldDisconnect(client, transferring);
        }
    }

    @Redirect(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;setScreenAndShow(Lnet/minecraft/client/gui/screens/Screen;)V")
    )
    private void newcustommusicclientsideplayer$showDisconnectScreen(
            Minecraft client, Screen target, Screen requestedScreen, boolean transferring, boolean stopSounds
    ) {
        if (transferring) {
            CPlaySoundController.beginServerTransferTeardown();
        }
        try {
            client.setScreenAndShow(target);
        } finally {
            if (transferring) {
                CPlaySoundController.endServerTransferTeardown();
            }
        }
    }

    @Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
    private void newcustommusicclientsideplayer$cleanWhenVanillaSkipsStop(
            ClientLevel level, boolean stopSounds, CallbackInfo callbackInfo
    ) {
        if (!stopSounds) {
            newcustommusicclientsideplayer$cleanWorldSounds(soundManager);
        }
    }

    @Redirect(
            method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop()V")
    )
    private void custommusicclientsideplayer$keepLocalPlaybackAcrossLevelChange(SoundManager soundManager) {
        // Reconfiguration reaches this boundary without an ordinary disconnect.
        newcustommusicclientsideplayer$cleanWorldSounds(soundManager);
    }

    @Unique
    private static void newcustommusicclientsideplayer$cleanWorldSounds(SoundManager soundManager) {
        if (((CLevelChangeSoundEngine) ((SoundManagerAccessor) soundManager)
                .newcustommusicclientsideplayer$getSoundEngine())
                .newcustommusicclientsideplayer$stopAllExcept(CPlaySoundController.currentSoundForLevelChange())) {
            return;
        }
        soundManager.stop();
    }
}
