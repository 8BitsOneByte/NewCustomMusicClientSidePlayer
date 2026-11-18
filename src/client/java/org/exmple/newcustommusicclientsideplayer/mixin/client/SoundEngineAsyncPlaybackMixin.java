package org.exmple.newcustommusicclientsideplayer.mixin.client;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.audio.SoundBuffer;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.exmple.newcustommusicclientsideplayer.client.playback.CManagedSoundInstance;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelAccess;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelHandle;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CLevelChangeSoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineAsyncPlaybackMixin implements CLevelChangeSoundEngine {
    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Shadow
    private boolean loaded;
    @Shadow
    @Final
    private ChannelAccess channelAccess;
    @Shadow
    @Final
    private Multimap<SoundSource, SoundInstance> instanceBySource;
    @Shadow
    @Final
    private Object2FloatMap<SoundSource> gainBySource;
    @Shadow
    @Final
    private List<TickableSoundInstance> tickingSounds;
    @Shadow
    @Final
    private Map<SoundInstance, Integer> queuedSounds;
    @Shadow
    @Final
    private Map<SoundInstance, Integer> soundDeleteTime;
    @Shadow
    @Final
    private List<TickableSoundInstance> queuedTickableSounds;

    @Override
    public boolean newcustommusicclientsideplayer$stopAllExcept(CManagedSoundInstance retainedSound) {
        if (!loaded || retainedSound == null) {
            return false;
        }
        ChannelAccess.ChannelHandle retainedHandle = instanceToChannel.get(retainedSound);
        if (retainedHandle == null || retainedHandle.isStopped()
                || ((CCancellableChannelHandle) retainedHandle).newcustommusicclientsideplayer$isCancelled()) {
            return false;
        }

        // Publish cancellation before removing indexes or queuing native releases.
        instanceToChannel.forEach((sound, handle) -> {
            if (sound != retainedSound) {
                ((CCancellableChannelHandle) handle).newcustommusicclientsideplayer$cancel();
            }
        });
        instanceToChannel.keySet().removeIf(sound -> sound != retainedSound);
        instanceBySource.values().removeIf(sound -> sound != retainedSound);
        // Managed music is a final SimpleSoundInstance, so both tickable lists belong to other sounds.
        tickingSounds.clear();
        queuedSounds.keySet().removeIf(sound -> sound != retainedSound);
        soundDeleteTime.keySet().removeIf(sound -> sound != retainedSound);
        queuedTickableSounds.clear();
        gainBySource.clear();
        // Sweep the actual handle set as well, including handles absent from the indexes.
        ((CCancellableChannelAccess) channelAccess)
                .newcustommusicclientsideplayer$releaseAllExcept(retainedHandle);
        return true;
    }

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
            ChannelAccess.ChannelHandle handle, SoundBuffer buffer, CallbackInfo callbackInfo
    ) {
        ((CCancellableChannelHandle) handle).newcustommusicclientsideplayer$prepareStaticPlayback(buffer);
        callbackInfo.cancel();
    }

    @Inject(method = "lambda$play$3", at = @At("HEAD"), cancellable = true)
    private static void newcustommusicclientsideplayer$blockCancelledStreamPlayback(
            ChannelAccess.ChannelHandle handle, AudioStream stream, CallbackInfo callbackInfo
    ) {
        ((CCancellableChannelHandle) handle).newcustommusicclientsideplayer$prepareStreamPlayback(stream);
        callbackInfo.cancel();
    }
}
