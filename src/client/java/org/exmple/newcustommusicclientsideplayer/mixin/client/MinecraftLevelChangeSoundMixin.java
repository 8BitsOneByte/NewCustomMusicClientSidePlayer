package org.exmple.newcustommusicclientsideplayer.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CLevelChangeSoundEngine;
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
    private void custommusicclientsideplayer$keepLocalPlaybackAcrossLevelChange(
            SoundManager soundManager, ClientLevel level, boolean stopSounds
    ) {
        // Menu playback policy is handled separately from entering a replacement world.
        if (level == null && CPlaySoundController.shouldKeepPlayingDuringLevelChange()) {
            return;
        }
        if (level != null && ((CLevelChangeSoundEngine) ((SoundManagerAccessor) soundManager)
                .newcustommusicclientsideplayer$getSoundEngine())
                .newcustommusicclientsideplayer$stopAllExcept(CPlaySoundController.currentSoundForLevelChange())) {
            return;
        }

        soundManager.stop();
    }
}
