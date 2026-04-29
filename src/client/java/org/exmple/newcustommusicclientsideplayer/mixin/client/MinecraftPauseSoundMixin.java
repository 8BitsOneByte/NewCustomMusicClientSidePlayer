package org.exmple.newcustommusicclientsideplayer.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftPauseSoundMixin {
    @Redirect(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundManager;pauseAllExcept([Lnet/minecraft/sounds/SoundSource;)V"
            )
    )
    private void custommusicclientsideplayer$pauseAllExceptWithMaster(SoundManager soundManager, SoundSource... soundSources) {
        if (CPlaySoundController.shouldKeepMasterDuringPause()) {
            soundManager.pauseAllExcept(SoundSource.MUSIC, SoundSource.UI, SoundSource.MASTER);
            return;
        }

        soundManager.pauseAllExcept(soundSources);
    }
}
