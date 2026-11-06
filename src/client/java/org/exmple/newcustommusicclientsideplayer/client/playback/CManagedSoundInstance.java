package org.exmple.newcustommusicclientsideplayer.client.playback;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import org.exmple.newcustommusicclientsideplayer.client.storage.CTrackSourceType;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.util.valueproviders.ConstantFloat;

public final class CManagedSoundInstance extends SimpleSoundInstance {
    private CTrackSourceType trackSource =
        CTrackSourceType.SOUND_EVENT;
    private volatile boolean playbackPaused;

    void setTrackSource(CTrackSourceType type) {
        trackSource = type;
    }

    @Override
    public WeighedSoundEvents getOrResolve(SoundManager manager) {
        if (trackSource == CTrackSourceType.SOUND_EVENT)
            return super.getOrResolve(manager);
        String path = identifier.getPath();
        Identifier location = Identifier.fromNamespaceAndPath(identifier.getNamespace(), path.substring(7, path.length() - 4));
        sound = new Sound(location,
            ConstantFloat.of(1.0F), ConstantFloat.of(1.0F),
            1, Sound.Type.FILE, true, false, 16);
        soundEvent = new WeighedSoundEvents(identifier, null);
        soundEvent.addSound(sound);
        return soundEvent;
    }

    CManagedSoundInstance(
            Identifier identifier,
            SoundSource soundSource,
            float volume,
            float pitch,
            RandomSource randomSource,
            boolean looping,
            int delay,
            SoundInstance.Attenuation attenuation,
            double x,
            double y,
            double z,
            boolean relative
    ) {
        super(identifier, soundSource, volume, pitch, randomSource, looping, delay, attenuation, x, y, z, relative);
    }

    void setBaseVolume(float volume) {
        this.volume = volume;
    }

    public boolean isPlaybackPaused() {
        return playbackPaused;
    }

    void setPlaybackPaused(boolean paused) {
        playbackPaused = paused;
    }
}
