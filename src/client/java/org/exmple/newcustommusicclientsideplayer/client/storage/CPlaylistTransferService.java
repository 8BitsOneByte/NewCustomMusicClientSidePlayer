package org.exmple.newcustommusicclientsideplayer.client.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CPlaylistTransferService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FALLBACK_MOD_NAME = "NewCustomMusicClientSidePlayer";
    private static final String UNKNOWN_VERSION = "unknown";
    private static final String MINECRAFT_MOD_ID = "minecraft";
    private static final String MOD_ID_KEY = "modId";
    private static final String PLAYLISTS_KEY = "playlists";
    private static final int MAX_PLAYLIST_NAME_LENGTH = 50;
    private static final long FALLBACK_MODIFIED_AT = 0L;

    public String defaultFileName() {
        return sanitizeFileName(modName()) + "-" + sanitizeFileName(modVersion()) + "-playlists.txt";
    }

    public void exportPlaylists(Path path) throws IOException {
        Objects.requireNonNull(path, "path");

        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        LinkedHashMap<String, CPlaylistTransferData.Playlist> playlists = new LinkedHashMap<>();
        for (CPlaylistRepository.ExportPlaylist playlist : CPlaylistRepository.exportPlaylists()) {
            playlists.put(
                playlist.name(),
                new CPlaylistTransferData.Playlist(playlist.tracks(), playlist.modifiedAt())
            );
        }

        CPlaylistTransferData data = new CPlaylistTransferData(
            modVersion(),
            minecraftVersion(),
            System.currentTimeMillis(),
            playlists
        );
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    public CPlaylistImportPreview previewImport(Path path) throws CPlaylistImportException {
        Objects.requireNonNull(path, "path");

        ParsedTransferFile transferFile = readTransferFile(path);
        validateModId(transferFile.modId());

        return scanPlaylists(transferFile.playlists());
    }

    public CPlaylistRepository.ImportResult importPreview(CPlaylistImportPreview preview) throws IOException {
        Objects.requireNonNull(preview, "preview");

        List<CPlaylistRepository.ImportPlaylist> playlists = new ArrayList<>();
        for (CPlaylistImportPreview.PlaylistToImport playlist : preview.playlists()) {
            playlists.add(new CPlaylistRepository.ImportPlaylist(
                playlist.name(),
                playlist.tracks(),
                playlist.modifiedAt()
            ));
        }

        return CPlaylistRepository.importPlaylists(playlists);
    }

    private static ParsedTransferFile readTransferFile(Path path) throws CPlaylistImportException {
        if (!Files.isRegularFile(path)) {
            throw CPlaylistImportException.ioError(null);
        }

        try {
            if (Files.size(path) == 0L) {
                throw CPlaylistImportException.ioError(null);
            }
        } catch (IOException exception) {
            throw CPlaylistImportException.ioError(exception);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonReader jsonReader = new JsonReader(reader);
            if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                throw CPlaylistImportException.invalidPlaylistFile();
            }

            String modId = null;
            boolean modIdReadable = false;
            List<RawPlaylistEntry> playlists = null;
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if (MOD_ID_KEY.equals(name)) {
                    if (jsonReader.peek() == JsonToken.STRING) {
                        modId = jsonReader.nextString();
                        modIdReadable = true;
                    } else {
                        jsonReader.skipValue();
                    }
                } else if (PLAYLISTS_KEY.equals(name)) {
                    if (jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
                        playlists = readPlaylistEntries(jsonReader);
                    } else {
                        jsonReader.skipValue();
                    }
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw CPlaylistImportException.invalidPlaylistFile();
            }

            if (!modIdReadable || playlists == null) {
                throw CPlaylistImportException.invalidPlaylistFile();
            }

            return new ParsedTransferFile(modId, playlists);
        } catch (JsonParseException | IllegalStateException | MalformedJsonException exception) {
            throw CPlaylistImportException.invalidPlaylistFile(exception);
        } catch (IOException exception) {
            throw CPlaylistImportException.ioError(exception);
        }
    }

    private static List<RawPlaylistEntry> readPlaylistEntries(JsonReader jsonReader) throws IOException {
        List<RawPlaylistEntry> entries = new ArrayList<>();
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String playlistName = jsonReader.nextName();
            JsonElement playlistElement = JsonParser.parseReader(jsonReader);
            entries.add(new RawPlaylistEntry(playlistName, playlistElement));
        }
        jsonReader.endObject();

        return entries;
    }

    private static void validateModId(String modId) throws CPlaylistImportException {
        if (!NewcustommusicclientsideplayerClient.MOD_ID.equals(modId)) {
            throw CPlaylistImportException.invalidPlaylistFile();
        }
    }

    private static CPlaylistImportPreview scanPlaylists(List<RawPlaylistEntry> playlistEntries) throws CPlaylistImportException {
        List<String> existingNames = readExistingPlaylistNames();
        Set<String> occupiedNamesLower = new HashSet<>();
        for (String existingName : existingNames) {
            occupiedNamesLower.add(existingName.toLowerCase(Locale.ROOT));
        }

        List<CPlaylistImportPreview.PlaylistToImport> playlists = new ArrayList<>();
        List<String> skippedPlaylistEntries = new ArrayList<>();
        List<String> unreadableTrackListPlaylists = new ArrayList<>();
        LinkedHashMap<String, List<String>> invalidTracksByPlaylist = new LinkedHashMap<>();
        List<String> invalidModifiedAtPlaylists = new ArrayList<>();
        List<CPlaylistImportPreview.Rename> renamedPlaylists = new ArrayList<>();

        int entryIndex = 0;
        for (RawPlaylistEntry entry : playlistEntries) {
            entryIndex++;
            String originalName = normalizePlaylistName(entry.name());
            if (originalName == null) {
                skippedPlaylistEntries.add(Integer.toString(entryIndex));
                continue;
            }

            String importName = uniquePlaylistName(originalName, occupiedNamesLower);
            occupiedNamesLower.add(importName.toLowerCase(Locale.ROOT));
            if (!importName.equals(originalName)) {
                renamedPlaylists.add(new CPlaylistImportPreview.Rename(originalName, importName));
            }

            ParsedPlaylist parsedPlaylist = parsePlaylist(entry.value());
            if (parsedPlaylist.unreadableTrackList()) {
                unreadableTrackListPlaylists.add(importName);
            }
            if (!parsedPlaylist.invalidTracks().isEmpty()) {
                invalidTracksByPlaylist.put(importName, parsedPlaylist.invalidTracks());
            }
            if (parsedPlaylist.invalidModifiedAt()) {
                invalidModifiedAtPlaylists.add(importName);
            }

            playlists.add(new CPlaylistImportPreview.PlaylistToImport(
                importName,
                parsedPlaylist.tracks(),
                parsedPlaylist.modifiedAt()
            ));
        }

        if (playlists.isEmpty()) {
            throw CPlaylistImportException.noCompatiblePlaylists();
        }

        return new CPlaylistImportPreview(
            playlists,
            skippedPlaylistEntries,
            unreadableTrackListPlaylists,
            invalidTracksByPlaylist,
            invalidModifiedAtPlaylists,
            renamedPlaylists
        );
    }

    private static List<String> readExistingPlaylistNames() throws CPlaylistImportException {
        try {
            return CPlaylistRepository.listPlaylistNames();
        } catch (IOException exception) {
            throw CPlaylistImportException.ioError(exception);
        }
    }

    private static ParsedPlaylist parsePlaylist(JsonElement element) {
        if (element instanceof JsonArray tracksArray) {
            ParsedTracks parsedTracks = parseTracks(tracksArray);
            return new ParsedPlaylist(parsedTracks.tracks(), FALLBACK_MODIFIED_AT, false, parsedTracks.invalidTracks(), true);
        }

        if (!(element instanceof JsonObject object)) {
            return new ParsedPlaylist(List.of(), FALLBACK_MODIFIED_AT, true, List.of(), true);
        }

        boolean unreadableTrackList = false;
        List<Identifier> tracks = List.of();
        List<String> invalidTracks = List.of();
        JsonElement tracksElement = object.get("tracks");
        if (tracksElement == null) {
            tracks = List.of();
        } else if (tracksElement instanceof JsonArray tracksArray) {
            ParsedTracks parsedTracks = parseTracks(tracksArray);
            tracks = parsedTracks.tracks();
            invalidTracks = parsedTracks.invalidTracks();
        } else {
            unreadableTrackList = true;
        }

        long modifiedAt = FALLBACK_MODIFIED_AT;
        boolean invalidModifiedAt = true;
        JsonElement modifiedAtElement = object.get("modifiedAt");
        if (modifiedAtElement instanceof JsonPrimitive primitive && primitive.isNumber()) {
            try {
                long parsedModifiedAt = primitive.getAsLong();
                if (parsedModifiedAt >= 0L) {
                    modifiedAt = parsedModifiedAt;
                    invalidModifiedAt = false;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return new ParsedPlaylist(tracks, modifiedAt, unreadableTrackList, invalidTracks, invalidModifiedAt);
    }

    private static ParsedTracks parseTracks(JsonArray tracksArray) {
        List<Identifier> tracks = new ArrayList<>();
        List<String> invalidTracks = new ArrayList<>();
        for (JsonElement trackElement : tracksArray) {
            String rawTrack = null;
            if (trackElement instanceof JsonPrimitive primitive && primitive.isString()) {
                rawTrack = primitive.getAsString();
                Identifier id = parseImportableTrackId(rawTrack);
                if (id != null) {
                    tracks.add(id);
                    continue;
                }
            }

            invalidTracks.add(rawTrack == null ? GSON.toJson(trackElement) : rawTrack);
        }

        return new ParsedTracks(tracks, invalidTracks);
    }

    private static Identifier parseImportableTrackId(String rawTrack) {
        Identifier id = Identifier.tryParse(rawTrack);
        if (id == null || id.getPath().isEmpty()) {
            return null;
        }

        return id;
    }

    private static String normalizePlaylistName(String rawName) {
        String name = rawName.trim();
        if (name.isEmpty()) {
            return null;
        }

        return name.length() > MAX_PLAYLIST_NAME_LENGTH ? null : name;
    }

    private static String uniquePlaylistName(String originalName, Set<String> occupiedNamesLower) {
        if (!occupiedNamesLower.contains(originalName.toLowerCase(Locale.ROOT))) {
            return originalName;
        }

        for (int suffix = 2; suffix < Integer.MAX_VALUE; suffix++) {
            String suffixText = "(" + suffix + ")";
            int baseLength = Math.min(originalName.length(), MAX_PLAYLIST_NAME_LENGTH - suffixText.length());
            String candidate = originalName.substring(0, baseLength) + suffixText;
            if (!occupiedNamesLower.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }

        throw new IllegalStateException("Could not allocate a unique playlist name.");
    }

    private static String modName() {
        return FabricLoader.getInstance()
            .getModContainer(NewcustommusicclientsideplayerClient.MOD_ID)
            .map(container -> container.getMetadata().getName())
            .filter(name -> !name.isBlank())
            .orElse(FALLBACK_MOD_NAME);
    }

    private static String modVersion() {
        return modVersion(NewcustommusicclientsideplayerClient.MOD_ID);
    }

    private static String minecraftVersion() {
        return modVersion(MINECRAFT_MOD_ID);
    }

    private static String modVersion(String modId) {
        return FabricLoader.getInstance()
            .getModContainer(modId)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .filter(version -> !version.isBlank())
            .orElse(UNKNOWN_VERSION);
    }

    private static String sanitizeFileName(String value) {
        String sanitized = value.trim().replaceAll("[\\\\/:*?\"<>|]+", "_");
        if (sanitized.isBlank()) {
            return UNKNOWN_VERSION;
        }

        return sanitized.toLowerCase(Locale.ROOT).equals("unknown") ? UNKNOWN_VERSION : sanitized;
    }

    private record ParsedPlaylist(
        List<Identifier> tracks,
        long modifiedAt,
        boolean unreadableTrackList,
        List<String> invalidTracks,
        boolean invalidModifiedAt
    ) {
    }

    private record ParsedTracks(List<Identifier> tracks, List<String> invalidTracks) {
    }

    private record ParsedTransferFile(String modId, List<RawPlaylistEntry> playlists) {
    }

    private record RawPlaylistEntry(String name, JsonElement value) {
    }
}
