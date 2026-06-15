package org.exmple.newcustommusicclientsideplayer.client.playback;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import org.exmple.newcustommusicclientsideplayer.client.playback.access.CCancellableChannelHandle;
import org.exmple.newcustommusicclientsideplayer.mixin.client.SoundEngineAccessor;
import org.exmple.newcustommusicclientsideplayer.mixin.client.SoundManagerAccessor;

final class COwnedSoundRegistry {
    private CManagedSoundInstance current;
    private final Set<CManagedSoundInstance> retired =
            java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    CManagedSoundInstance current() {
        return current;
    }

    boolean hasCurrent() {
        return current != null;
    }

    boolean hasOwnedSounds() {
        return current != null || !retired.isEmpty();
    }

    void activate(CManagedSoundInstance sound) {
        if (current != null) {
            throw new IllegalStateException("Current sound must be retired before activating another sound");
        }
        current = sound;
    }

    void retireCurrent(Minecraft client) {
        if (current == null) {
            return;
        }

        CManagedSoundInstance retiring = current;
        current = null;
        retired.add(retiring);
        markCancelled(client, retiring);
        stopOwnedSound(client.getSoundManager(), retiring);
    }

    void releaseCurrent(Minecraft client) {
        if (current == null) {
            return;
        }

        CManagedSoundInstance retiring = current;
        retired.add(retiring);
        current = null;
        markCancelled(client, retiring);
    }

    void releaseFinishedCurrent(Minecraft client) {
        if (current == null) {
            return;
        }

        SoundEngine soundEngine = ((SoundManagerAccessor) client.getSoundManager())
                .newcustommusicclientsideplayer$getSoundEngine();
        Map<SoundInstance, ChannelAccess.ChannelHandle> channels =
                ((SoundEngineAccessor) soundEngine).newcustommusicclientsideplayer$getInstanceToChannel();
        ChannelAccess.ChannelHandle handle = channels.get(current);
        if (handle == null || handle.isStopped()) {
            releaseCurrent(client);
        }
    }

    void stopAll(Minecraft client) {
        SoundManager soundManager = client.getSoundManager();
        SoundEngine soundEngine = ((SoundManagerAccessor) soundManager)
                .newcustommusicclientsideplayer$getSoundEngine();
        Map<SoundInstance, ChannelAccess.ChannelHandle> channels =
                ((SoundEngineAccessor) soundEngine).newcustommusicclientsideplayer$getInstanceToChannel();
        Set<CManagedSoundInstance> owned = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

        if (current != null) {
            owned.add(current);
        }
        owned.addAll(retired);

        for (CManagedSoundInstance sound : owned) {
            ChannelAccess.ChannelHandle handle = channels.get(sound);
            if (handle != null) {
                markCancelled(handle);
            }
            stopOwnedSound(soundManager, sound);
            if (handle != null && !handle.isStopped()) {
                ((CCancellableChannelHandle) handle)
                        .newcustommusicclientsideplayer$requestCancelledHandleRelease();
            }
        }

        current = null;
        retired.clear();
    }

    void recoverRetiredSounds(Minecraft client) {
        SoundEngine soundEngine = ((SoundManagerAccessor) client.getSoundManager())
                .newcustommusicclientsideplayer$getSoundEngine();
        Map<SoundInstance, ChannelAccess.ChannelHandle> channels =
                ((SoundEngineAccessor) soundEngine).newcustommusicclientsideplayer$getInstanceToChannel();
        SoundManager soundManager = client.getSoundManager();

        Iterator<CManagedSoundInstance> iterator = retired.iterator();
        while (iterator.hasNext()) {
            CManagedSoundInstance sound = iterator.next();
            ChannelAccess.ChannelHandle handle = channels.get(sound);
            if (handle == null) {
                iterator.remove();
                continue;
            }

            markCancelled(handle);

            if (handle.isStopped()) {
                continue;
            }

            soundManager.stop(sound);
        }
    }

    private static void stopOwnedSound(SoundManager soundManager, CManagedSoundInstance sound) {
        soundManager.stop(sound);
        soundManager.stop(sound.getIdentifier(), SoundSource.MASTER);
    }

    private static void markCancelled(Minecraft client, CManagedSoundInstance sound) {
        SoundEngine soundEngine = ((SoundManagerAccessor) client.getSoundManager())
                .newcustommusicclientsideplayer$getSoundEngine();
        Map<SoundInstance, ChannelAccess.ChannelHandle> channels =
                ((SoundEngineAccessor) soundEngine).newcustommusicclientsideplayer$getInstanceToChannel();
        ChannelAccess.ChannelHandle handle = channels.get(sound);
        if (handle != null) {
            markCancelled(handle);
        }
    }

    private static void markCancelled(ChannelAccess.ChannelHandle handle) {
        CCancellableChannelHandle cancellable = (CCancellableChannelHandle) handle;
        if (cancellable.newcustommusicclientsideplayer$isCancelled()) {
            return;
        }

        cancellable.newcustommusicclientsideplayer$cancel();
    }
}
