package org.exmple.newcustommusicclientsideplayer.client.playback.access;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.sounds.AudioStream;
import org.exmple.newcustommusicclientsideplayer.client.playback.CManagedSoundInstance;

public interface CCancellableChannelHandle {
    void newcustommusicclientsideplayer$bindManagedSound(CManagedSoundInstance sound);

    boolean newcustommusicclientsideplayer$isManagedPlayback();

    boolean newcustommusicclientsideplayer$blocksGlobalResume(Channel channel);

    void newcustommusicclientsideplayer$prepareStaticPlayback(SoundBuffer buffer);

    void newcustommusicclientsideplayer$prepareStreamPlayback(AudioStream stream);

    void newcustommusicclientsideplayer$pauseManagedPlayback();

    void newcustommusicclientsideplayer$resumeManagedPlayback();

    void newcustommusicclientsideplayer$cancel();

    boolean newcustommusicclientsideplayer$isCancelled();

    void newcustommusicclientsideplayer$requestCancelledHandleRelease();
}
