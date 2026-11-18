package org.exmple.newcustommusicclientsideplayer.client.audio.catalog;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record CAudioResourceInfo(Identifier resourceId, String sourcePackId,
                                 MetadataStatus status, Optional<Metadata> metadata) {
    public CAudioResourceInfo {
        Objects.requireNonNull(resourceId);
        Objects.requireNonNull(sourcePackId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(metadata);
        if ((status == MetadataStatus.READY) != metadata.isPresent()) {
            throw new IllegalArgumentException("Metadata status");
        }
    }

    public String namespace() {
        return resourceId.getNamespace();
    }

    static CAudioResourceInfo pending(Identifier id, String packId) {
        return new CAudioResourceInfo(id, packId, MetadataStatus.PENDING, Optional.empty());
    }

    CAudioResourceInfo withResult(MetadataStatus result, Metadata value) {
        return new CAudioResourceInfo(resourceId, sourcePackId, result, Optional.ofNullable(value));
    }

    public enum MetadataStatus {
        PENDING, READY, INVALID, UNSUPPORTED, IO_ERROR
    }

    public record Metadata(long sampleCount, int sampleRate, int channels) {
        public Metadata {
            if (sampleCount < 0 || sampleRate <= 0 || channels <= 0 || channels > 255) {
                throw new IllegalArgumentException("Audio metadata");
            }
        }

        public double durationSeconds() {
            return (double) sampleCount / sampleRate;
        }
    }
}
