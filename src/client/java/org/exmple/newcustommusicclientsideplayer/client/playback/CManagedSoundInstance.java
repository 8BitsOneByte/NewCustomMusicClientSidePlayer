package org.exmple.newcustommusicclientsideplayer.client.playback;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class CManagedSoundInstance extends SimpleSoundInstance {
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
}
