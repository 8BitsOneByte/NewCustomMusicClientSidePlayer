package org.exmple.newcustommusicclientsideplayer.client.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigInteger;

final class CPlaylistFormat {
    static final int CURRENT_VERSION = 2;

    private CPlaylistFormat() {
    }

    static int version(JsonElement element) throws CPlaylistStorageException {
        if (element == null) {
            return 1;
        }
        if (!(element instanceof JsonPrimitive value) || !value.isNumber()
            || !value.getAsString().matches("[1-9][0-9]*")) {
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.INVALID_DATA);
        }
        BigInteger version = new BigInteger(value.getAsString());
        if (version.compareTo(BigInteger.valueOf(CURRENT_VERSION)) > 0) {
            throw new CPlaylistStorageException(CPlaylistStorageException.Reason.UNSUPPORTED_VERSION);
        }
        return version.intValue();
    }

    static CTrackSourceType sourceType(JsonElement element, int version) {
        if (version == 1) {
            return CTrackSourceType.SOUND_EVENT;
        }
        if (element instanceof JsonObject object
            && object.get("sourceType") instanceof JsonPrimitive value && value.isString()) {
            try {
                return CTrackSourceType.valueOf(value.getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
