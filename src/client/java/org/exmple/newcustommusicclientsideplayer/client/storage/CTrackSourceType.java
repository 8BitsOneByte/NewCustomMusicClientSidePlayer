package org.exmple.newcustommusicclientsideplayer.client.storage;

import net.minecraft.resources.Identifier;

public enum CTrackSourceType {
    SOUND_EVENT,
    OGG_RESOURCE;

    public boolean accepts(Identifier id) {
        return id != null && !id.getPath().isEmpty()
            && (this == SOUND_EVENT || (id.getPath().startsWith("sounds/") && id.getPath().endsWith(".ogg")));
    }
}
