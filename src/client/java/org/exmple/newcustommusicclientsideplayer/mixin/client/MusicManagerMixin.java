package org.exmple.newcustommusicclientsideplayer.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.Music;
import org.exmple.newcustommusicclientsideplayer.client.playback.CPlaySoundController;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelHandle;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CVanillaMusicControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MusicManagerMixin implements CVanillaMusicControl {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private SoundInstance currentMusic;

    @Shadow
    public abstract void stopPlaying();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void newcustommusicclientsideplayer$suppressScheduledMusic(CallbackInfo callbackInfo) {
        if (CPlaySoundController.shouldSuppressVanillaMusic()) {
            newcustommusicclientsideplayer$stopForManagedPlayback();
            callbackInfo.cancel();
        }
    }

    @Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
    private void newcustommusicclientsideplayer$suppressDirectMusicStart(
            Music music, CallbackInfo callbackInfo
    ) {
        if (CPlaySoundController.shouldSuppressVanillaMusic()) {
            newcustommusicclientsideplayer$stopForManagedPlayback();
            callbackInfo.cancel();
        }
    }

    @Override
    public void newcustommusicclientsideplayer$stopForManagedPlayback() {
        if (currentMusic == null) {
            return;
        }

        SoundEngine soundEngine = ((SoundManagerAccessor) minecraft.getSoundManager())
                .newcustommusicclientsideplayer$getSoundEngine();
        ChannelAccess.ChannelHandle handle = ((SoundEngineAccessor) soundEngine)
                .newcustommusicclientsideplayer$getInstanceToChannel().get(currentMusic);
        CCancellableChannelHandle cancellable = (CCancellableChannelHandle) handle;
        if (cancellable != null) {
            // Cancel before stopping so late static buffers and streams cannot restart this music.
            cancellable.newcustommusicclientsideplayer$cancel();
        }
        // Clear the tracked music, hide its toast and reset its normal scheduling delay once.
        stopPlaying();
        if (cancellable != null) {
            cancellable.newcustommusicclientsideplayer$requestCancelledHandleRelease();
        }
    }
}
