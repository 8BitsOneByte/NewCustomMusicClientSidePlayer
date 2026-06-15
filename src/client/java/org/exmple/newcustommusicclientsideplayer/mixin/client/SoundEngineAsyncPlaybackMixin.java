package org.exmple.newcustommusicclientsideplayer.mixin.client;

import com.mojang.blaze3d.audio.SoundBuffer;
import java.io.IOException;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelHandle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineAsyncPlaybackMixin {
    @Inject(method = "lambda$play$1", at = @At("HEAD"), cancellable = true)
    private static void newcustommusicclientsideplayer$blockCancelledStaticPlayback(
            ChannelAccess.ChannelHandle handle,
            SoundBuffer buffer,
            CallbackInfo callbackInfo
    ) {
        if (!blockCancelledPlayback(handle)) {
            return;
        }

        requestCancelledHandleRelease(handle);
        callbackInfo.cancel();
    }

    @Inject(method = "lambda$play$3", at = @At("HEAD"), cancellable = true)
    private static void newcustommusicclientsideplayer$blockCancelledStreamPlayback(
            ChannelAccess.ChannelHandle handle,
            AudioStream stream,
            CallbackInfo callbackInfo
    ) {
        if (!blockCancelledPlayback(handle)) {
            return;
        }

        closeQuietly(stream);
        requestCancelledHandleRelease(handle);
        callbackInfo.cancel();
    }

    private static boolean blockCancelledPlayback(ChannelAccess.ChannelHandle handle) {
        CCancellableChannelHandle cancellable = (CCancellableChannelHandle) handle;
        if (!cancellable.newcustommusicclientsideplayer$isCancelled()) {
            return false;
        }

        return true;
    }

    private static void requestCancelledHandleRelease(ChannelAccess.ChannelHandle handle) {
        ((CCancellableChannelHandle) handle)
                .newcustommusicclientsideplayer$requestCancelledHandleRelease();
    }

    private static void closeQuietly(AudioStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }
}
