package org.exmple.newcustommusicclientsideplayer.mixin.client;

import java.util.Set;
import java.util.concurrent.Executor;
import net.minecraft.client.sounds.ChannelAccess;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelAccess;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelHandle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChannelAccess.class)
public abstract class ChannelAccessCancellationMixin implements CCancellableChannelAccess {
    @Shadow
    @Final
    private Set<ChannelAccess.ChannelHandle> channels;

    @Shadow
    @Final
    private Executor executor;

    @Override
    public void newcustommusicclientsideplayer$scheduleCancelledHandleRelease(
            ChannelAccess.ChannelHandle handle
    ) {
        CCancellableChannelHandle cancellable = (CCancellableChannelHandle) handle;
        if (!cancellable.newcustommusicclientsideplayer$isCancelled()) {
            return;
        }

        executor.execute(() -> {
            if (cancellable.newcustommusicclientsideplayer$isCancelled() && channels.remove(handle)) {
                handle.release();
            }
        });
    }
}
