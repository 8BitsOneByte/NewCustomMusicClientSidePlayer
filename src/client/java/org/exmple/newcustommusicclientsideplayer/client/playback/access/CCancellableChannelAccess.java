package org.exmple.newcustommusicclientsideplayer.client.playback.access;

import net.minecraft.client.sounds.ChannelAccess;

public interface CCancellableChannelAccess {
    void newcustommusicclientsideplayer$executePlaybackTask(Runnable task);

    void newcustommusicclientsideplayer$scheduleCancelledHandleRelease(
            ChannelAccess.ChannelHandle handle
    );
}
