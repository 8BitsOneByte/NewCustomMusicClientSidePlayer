package org.exmple.newcustommusicclientsideplayer.mixin.client;

import com.mojang.blaze3d.audio.Channel;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.sounds.ChannelAccess;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelAccess;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelHandle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChannelAccess.ChannelHandle.class)
public abstract class ChannelHandleCancellationMixin implements CCancellableChannelHandle {
    @Unique
    private ChannelAccess newcustommusicclientsideplayer$owner;

    @Unique
    private volatile boolean newcustommusicclientsideplayer$cancelled;

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
