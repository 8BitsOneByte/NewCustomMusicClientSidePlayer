package org.exmple.newcustommusicclientsideplayer.mixin.client;

import com.mojang.blaze3d.audio.SoundBuffer;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.exmple.newcustommusicclientsideplayer.client.playback.CManagedSoundInstance;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelHandle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineAsyncPlaybackMixin {
    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Redirect(
            method = "play",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V",
                    ordinal = 0)
    )
    private void newcustommusicclientsideplayer$bindBeforeLoading(
            ChannelAccess.ChannelHandle handle, Consumer<com.mojang.blaze3d.audio.Channel> action,
            SoundInstance sound
    ) {
        if (sound instanceof CManagedSoundInstance managed) {
            ((CCancellableChannelHandle) handle).newcustommusicclientsideplayer$bindManagedSound(managed);
        }
        handle.execute(action);
    }

    @Redirect(
            method = "resume",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/ChannelAccess;executeOnChannels(Ljava/util/function/Consumer;)V")
    )
    private void newcustommusicclientsideplayer$preserveManualPause(
            ChannelAccess access, Consumer<Stream<com.mojang.blaze3d.audio.Channel>> action
    ) {
        // Snapshot handles on the client thread; inspect their live state on the sound executor.
        CCancellableChannelHandle[] handles = instanceToChannel.values().stream()
                .map(handle -> (CCancellableChannelHandle) handle)
                .toArray(CCancellableChannelHandle[]::new);
        access.executeOnChannels(channels -> action.accept(channels.filter(channel -> {
            for (CCancellableChannelHandle handle : handles) {
                if (handle.newcustommusicclientsideplayer$blocksGlobalResume(channel)) {
                    return false;
                }
            }
            return true;
        })));
    }

    @Inject(method = "lambda$play$1", at = @At("HEAD"), cancellable = true)
    private static void newcustommusicclientsideplayer$blockCancelledStaticPlayback(
            ChannelAccess.ChannelHandle handle,
            SoundBuffer buffer,
            CallbackInfo callbackInfo
    ) {
        CCancellableChannelHandle controlled = (CCancellableChannelHandle) handle;
        if (controlled.newcustommusicclientsideplayer$isManagedPlayback()) {
            controlled.newcustommusicclientsideplayer$prepareStaticPlayback(buffer);
            callbackInfo.cancel();
            return;
        }
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
        CCancellableChannelHandle controlled = (CCancellableChannelHandle) handle;
        if (controlled.newcustommusicclientsideplayer$isManagedPlayback()) {
            controlled.newcustommusicclientsideplayer$prepareStreamPlayback(stream);
            callbackInfo.cancel();
            return;
        }
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
