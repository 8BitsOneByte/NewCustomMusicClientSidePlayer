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

    private final CPlaylistStore store;

    public CPlaylistTransferService() {
        this(CPlaylistRepository.store());
    }

    CPlaylistTransferService(CPlaylistStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public String defaultFileName() {
        return sanitizeFileName(modName()) + "-" + sanitizeFileName(modVersion()) + "-playlists.txt";
    }

    public void exportPlaylists(Path path) throws IOException {
        Objects.requireNonNull(path, "path");

        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        JsonObject playlists = this.store.exportPlaylists();

        CPlaylistTransferData data = new CPlaylistTransferData(
            modVersion(),
            minecraftVersion(),
            System.currentTimeMillis(),
            Map.of()
        );
        JsonObject document = GSON.toJsonTree(data).getAsJsonObject();
        document.add(PLAYLISTS_KEY, playlists);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(document, writer);
        }
    }

    public CPlaylistImportPreview previewImport(Path path) throws CPlaylistImportException {
        Objects.requireNonNull(path, "path");

        ParsedTransferFile transferFile = readTransferFile(path);
        validateModId(transferFile.modId());

        return scanPlaylists(transferFile.playlists(), transferFile.version());
    }

    public CPlaylistRepository.ImportResult importPreview(CPlaylistImportPreview preview) throws IOException {
        Objects.requireNonNull(preview, "preview");

        List<CPlaylistRepository.ImportPlaylist> playlists = new ArrayList<>();
        for (CPlaylistImportPreview.PlaylistToImport playlist : preview.playlists()) {
            playlists.add(new CPlaylistRepository.ImportPlaylist(
                playlist.name(),
                playlist.sourceType(),
                playlist.tracks(),
                playlist.modifiedAt()
            ));
        }

        this.store.importPlaylists(playlists);
        return new CPlaylistRepository.ImportResult(playlists.size());
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
            jsonReader.setStrictness(com.google.gson.Strictness.STRICT);
            if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                throw CPlaylistImportException.invalidPlaylistFile();
            }

            JsonElement formatVersion = null;
            boolean versionSeen = false;
            String modId = null;
            boolean modIdReadable = false;
            List<RawPlaylistEntry> playlists = null;
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if ("formatVersion".equals(name)) {
                    if (versionSeen) {
                        throw CPlaylistImportException.unsupportedVersion();
                    }
                    versionSeen = true;
                    formatVersion = JsonParser.parseReader(jsonReader);
                } else if (MOD_ID_KEY.equals(name)) {
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

            int version = CPlaylistFormat.version(formatVersion);
            if (version >= 3) {
                List<RawPlaylistEntry> flattened = new ArrayList<>();
                for (RawPlaylistEntry group : playlists) {
                    CTrackSourceType type;
                    try { type = CTrackSourceType.valueOf(group.name()); }
                    catch (IllegalArgumentException exception) {
                        flattened.add(new RawPlaylistEntry(group.name(), com.google.gson.JsonNull.INSTANCE));
                        continue;
                    }
                    if (!(group.value() instanceof JsonObject)) {
                        flattened.add(new RawPlaylistEntry(group.name(), com.google.gson.JsonNull.INSTANCE));
                        continue;
                    }
                    for (RawPlaylistEntry entry : group.children()) {
                        JsonElement value = entry.value().deepCopy();
                        if (value instanceof JsonObject object
                            && CPlaylistFormat.sourceType(object, version) == type) {
                            flattened.add(new RawPlaylistEntry(entry.name(), object));
                        } else flattened.add(new RawPlaylistEntry(entry.name(), com.google.gson.JsonNull.INSTANCE));
                    }
                }
                playlists = flattened;
            }
            return new ParsedTransferFile(modId, playlists, version);
        } catch (CPlaylistStorageException exception) {
            if (exception.reason() == CPlaylistStorageException.Reason.UNSUPPORTED_VERSION) {
                throw CPlaylistImportException.unsupportedVersion();
            }
            throw CPlaylistImportException.invalidPlaylistFile(exception);
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
            if (jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
                List<RawPlaylistEntry> children = readPlaylistEntries(jsonReader);
                JsonObject object = new JsonObject();
                children.forEach(child -> object.add(child.name(), child.value()));
                entries.add(new RawPlaylistEntry(playlistName, object, children));
            } else entries.add(new RawPlaylistEntry(playlistName, JsonParser.parseReader(jsonReader)));
        }
        jsonReader.endObject();

        return entries;
    }

    private static void validateModId(String modId) throws CPlaylistImportException {
        if (!NewcustommusicclientsideplayerClient.MOD_ID.equals(modId)) {
            throw CPlaylistImportException.invalidPlaylistFile();
        }
    }

    private CPlaylistImportPreview scanPlaylists(List<RawPlaylistEntry> playlistEntries, int version) throws CPlaylistImportException {
        List<CPlaylistImportPreview.PlaylistToImport> all = new ArrayList<>();
        Map<CTrackSourceType, CPlaylistImportPreview> groups = new java.util.EnumMap<>(CTrackSourceType.class);
        List<String> skipped = new ArrayList<>();
        for (int i = 0; i < playlistEntries.size(); i++) {
            RawPlaylistEntry entry = playlistEntries.get(i);
            if (normalizePlaylistName(entry.name()) == null || CPlaylistFormat.sourceType(entry.value(), version) == null)
                skipped.add(Integer.toString(i + 1));
        }
        for (CTrackSourceType type : CTrackSourceType.values()) {
            var entries = playlistEntries.stream().filter(entry -> normalizePlaylistName(entry.name()) != null
                && CPlaylistFormat.sourceType(entry.value(), version) == type).toList();
            CPlaylistImportPreview group = scanSource(entries, version, type);
            groups.put(type, group);
            all.addAll(group.playlists());
        }
        if (all.isEmpty()) throw CPlaylistImportException.noCompatiblePlaylists();
        return new CPlaylistImportPreview(all, skipped, List.of(), Map.of(), List.of(), List.of(), groups);
    }

    private CPlaylistImportPreview scanSource(List<RawPlaylistEntry> playlistEntries, int version,
        CTrackSourceType type) throws CPlaylistImportException {
        Set<String> occupiedNames = new HashSet<>(readExistingPlaylistNames(type));

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
            CTrackSourceType sourceType = CPlaylistFormat.sourceType(entry.value(), version);
            if (originalName == null || sourceType == null) {
                skippedPlaylistEntries.add(Integer.toString(entryIndex));
                continue;
            }

            String importName = uniquePlaylistName(originalName, occupiedNames);
            occupiedNames.add(importName);
            if (!importName.equals(originalName)) {
                renamedPlaylists.add(new CPlaylistImportPreview.Rename(originalName, importName));
            }

            ParsedPlaylist parsedPlaylist = parsePlaylist(entry.value(), sourceType);
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
                sourceType,
                parsedPlaylist.tracks(),
                parsedPlaylist.modifiedAt()
            ));
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

    private List<String> readExistingPlaylistNames(CTrackSourceType type) throws CPlaylistImportException {
        try {
            return this.store.names(type);
        } catch (IOException exception) {
            throw CPlaylistImportException.ioError(exception);
        }
    }

    private static ParsedPlaylist parsePlaylist(JsonElement element, CTrackSourceType sourceType) {
        if (element instanceof JsonArray tracksArray) {
            ParsedTracks parsedTracks = parseTracks(tracksArray, sourceType);
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
            ParsedTracks parsedTracks = parseTracks(tracksArray, sourceType);
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

    private static ParsedTracks parseTracks(JsonArray tracksArray, CTrackSourceType sourceType) {
        List<Identifier> tracks = new ArrayList<>();
        List<String> invalidTracks = new ArrayList<>();
        for (JsonElement trackElement : tracksArray) {
            String rawTrack = null;
            if (trackElement instanceof JsonPrimitive primitive && primitive.isString()) {
                rawTrack = primitive.getAsString();
                Identifier id = parseImportableTrackId(rawTrack);
                if (sourceType.accepts(id)) {
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

    private static String uniquePlaylistName(String originalName, Set<String> occupiedNames) {
        if (!occupiedNames.contains(originalName)) {
            return originalName;
        }

        for (int suffix = 2; suffix < Integer.MAX_VALUE; suffix++) {
            String suffixText = "(" + suffix + ")";
            int baseLength = Math.min(originalName.length(), MAX_PLAYLIST_NAME_LENGTH - suffixText.length());
            String candidate = originalName.substring(0, baseLength) + suffixText;
            if (!occupiedNames.contains(candidate)) {
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

    private record ParsedTransferFile(String modId, List<RawPlaylistEntry> playlists, int version) {
    }

    private record RawPlaylistEntry(String name, JsonElement value, List<RawPlaylistEntry> children) {
        RawPlaylistEntry(String name, JsonElement value) {
            this(name, value, List.of());
        }
    }
}
