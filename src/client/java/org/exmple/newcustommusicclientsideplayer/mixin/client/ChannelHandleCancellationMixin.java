package org.exmple.newcustommusicclientsideplayer.mixin.client;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.SoundBuffer;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.ChannelAccess;
import org.exmple.newcustommusicclientsideplayer.client.playback.CManagedSoundInstance;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelAccess;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelHandle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChannelAccess.ChannelHandle.class)
public abstract class ChannelHandleCancellationMixin implements CCancellableChannelHandle {
    @Shadow
    private Channel channel;

    @Unique
    private ChannelAccess newcustommusicclientsideplayer$owner;

    @Unique
    private volatile boolean newcustommusicclientsideplayer$cancelled;

    @Unique
    private volatile CManagedSoundInstance newcustommusicclientsideplayer$managedSound;

    // These fields are read and written only on the sound executor.
    @Unique
    private boolean newcustommusicclientsideplayer$prepared;

    @Unique
    private boolean newcustommusicclientsideplayer$started;

    @Unique
    private final AtomicBoolean newcustommusicclientsideplayer$releaseRequested = new AtomicBoolean();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void newcustommusicclientsideplayer$captureOwner(
            ChannelAccess owner,
            Channel channel,
            CallbackInfo callbackInfo
    ) {
        newcustommusicclientsideplayer$owner = owner;
    }

    @Override
    public void newcustommusicclientsideplayer$bindManagedSound(CManagedSoundInstance sound) {
        newcustommusicclientsideplayer$managedSound = sound;
    }

    @Override
    public boolean newcustommusicclientsideplayer$isManagedPlayback() {
        return newcustommusicclientsideplayer$managedSound != null;
    }

    @Override
    public boolean newcustommusicclientsideplayer$blocksGlobalResume(Channel target) {
        CManagedSoundInstance sound = newcustommusicclientsideplayer$managedSound;
        return channel == target && (newcustommusicclientsideplayer$cancelled
                || (sound != null && sound.isPlaybackPaused()));
    }

    @Override
    public void newcustommusicclientsideplayer$prepareStaticPlayback(SoundBuffer buffer) {
        newcustommusicclientsideplayer$execute(() -> {
            if (newcustommusicclientsideplayer$cancelled || channel == null) {
                newcustommusicclientsideplayer$requestCancelledHandleRelease();
                return;
            }
            channel.attachStaticBuffer(buffer);
            newcustommusicclientsideplayer$prepared = true;
            newcustommusicclientsideplayer$startOrResume();
        });
    }

    @Override
    public void newcustommusicclientsideplayer$prepareStreamPlayback(AudioStream stream) {
        newcustommusicclientsideplayer$execute(() -> {
            if (newcustommusicclientsideplayer$cancelled || channel == null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
                newcustommusicclientsideplayer$requestCancelledHandleRelease();
                return;
            }
            channel.attachBufferStream(stream);
            newcustommusicclientsideplayer$prepared = true;
            newcustommusicclientsideplayer$startOrResume();
        });
    }

    @Override
    public void newcustommusicclientsideplayer$pauseManagedPlayback() {
        newcustommusicclientsideplayer$execute(() -> {
            CManagedSoundInstance sound = newcustommusicclientsideplayer$managedSound;
            if (!newcustommusicclientsideplayer$cancelled && channel != null
                    && sound != null && sound.isPlaybackPaused()) {
                channel.pause();
            }
        });
    }

    @Override
    public void newcustommusicclientsideplayer$resumeManagedPlayback() {
        newcustommusicclientsideplayer$execute(this::newcustommusicclientsideplayer$startOrResume);
    }

    @Unique
    private void newcustommusicclientsideplayer$execute(Runnable task) {
        ((CCancellableChannelAccess) newcustommusicclientsideplayer$owner)
                .newcustommusicclientsideplayer$executePlaybackTask(task);
    }

    @Unique
    private void newcustommusicclientsideplayer$startOrResume() {
        CManagedSoundInstance sound = newcustommusicclientsideplayer$managedSound;
        if (newcustommusicclientsideplayer$cancelled || channel == null
                || sound == null || sound.isPlaybackPaused()
                || !newcustommusicclientsideplayer$prepared) {
            return;
        }
        if (!newcustommusicclientsideplayer$started) {
            channel.play();
            newcustommusicclientsideplayer$started = true;
        } else {
            channel.unpause();
        }
    }

    @Override
    public void newcustommusicclientsideplayer$cancel() {
        newcustommusicclientsideplayer$cancelled = true;
    }

    @Override
    public boolean newcustommusicclientsideplayer$isCancelled() {
        return newcustommusicclientsideplayer$cancelled;
    }

    @Override
    public void newcustommusicclientsideplayer$requestCancelledHandleRelease() {
        if (!newcustommusicclientsideplayer$cancelled
                || !newcustommusicclientsideplayer$releaseRequested.compareAndSet(false, true)) {
            return;
        }

        ((CCancellableChannelAccess) newcustommusicclientsideplayer$owner)
                .newcustommusicclientsideplayer$scheduleCancelledHandleRelease(
                        (ChannelAccess.ChannelHandle) (Object) this
                );
    }
}
