package org.exmple.newcustommusicclientsideplayer.client.playback.access;

import net.minecraft.client.sounds.ChannelAccess;

public interface CCancellableChannelAccess {
    void newcustommusicclientsideplayer$executePlaybackTask(Runnable task);

    void newcustommusicclientsideplayer$releaseAllExcept(ChannelAccess.ChannelHandle retainedHandle);

    void newcustommusicclientsideplayer$scheduleCancelledHandleRelease(
            ChannelAccess.ChannelHandle handle
    );
}
