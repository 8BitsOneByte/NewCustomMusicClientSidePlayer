package org.exmple.newcustommusicclientsideplayer.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftLevelChangeSoundMixin {
    @Redirect(
            method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundManager;stop()V"
            )
    )
    private void custommusicclientsideplayer$keepLocalPlaybackAcrossLevelChange(SoundManager soundManager) {
        if (CPlaySoundController.shouldKeepPlayingDuringLevelChange()) {
            return;
        }

        soundManager.stop();
    }
}
