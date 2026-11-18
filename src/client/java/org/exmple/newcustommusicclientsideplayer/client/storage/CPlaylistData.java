package org.exmple.newcustommusicclientsideplayer.client.storage;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public record CPlaylistData(CTrackSourceType sourceType, List<Identifier> tracks, long modifiedAt) {
    public CPlaylistData {
        Objects.requireNonNull(sourceType, "sourceType");
        tracks = List.copyOf(tracks);
        if (modifiedAt < 0 || tracks.stream().anyMatch(id -> !sourceType.accepts(id))) {
            throw new IllegalArgumentException("Invalid playlist");
        }
    }
}
