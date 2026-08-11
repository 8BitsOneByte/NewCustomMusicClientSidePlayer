package org.exmple.newcustommusicclientsideplayer.client.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.exmple.newcustommusicclientsideplayer.client.bootstrap.NewcustommusicclientsideplayerClient;

public final class CTrackNameTransferService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FALLBACK_MOD_NAME = "NewCustomMusicClientSidePlayer";
    private static final String UNKNOWN_VERSION = "unknown";
    private static final String MINECRAFT_MOD_ID = "minecraft";
    private static final String MOD_ID_KEY = "modId";
    private static final String TRACK_NAMES_KEY = "trackNames";

    public String defaultFileName() {
        return sanitizeFileName(modName()) + "-" + sanitizeFileName(modVersion()) + "-track-names.txt";
    }

    public void exportTrackNames(Path path) throws IOException {
        Objects.requireNonNull(path, "path");

        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        CTrackNameTransferData data = new CTrackNameTransferData(
            modVersion(),
            minecraftVersion(),
            System.currentTimeMillis(),
            CTrackNameRepository.exportCustomNames()
        );
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    public CTrackNameImportPreview previewImport(
        Path path,
        Collection<Identifier> availableTracks
    ) throws CTrackNameImportException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(availableTracks, "availableTracks");

        ParsedTransferFile transferFile = readTransferFile(path);
        if (!NewcustommusicclientsideplayerClient.MOD_ID.equals(transferFile.modId())) {
            throw CTrackNameImportException.invalidTrackNameFile();
        }

        Map<String, String> currentNames;
        try {
            currentNames = CTrackNameRepository.exportCustomNames();
        } catch (IOException exception) {
            throw CTrackNameImportException.ioError(exception);
        }

        return scanTrackNames(transferFile.entries(), availableTracks, currentNames);
    }

    public CTrackNameRepository.ImportResult importPreview(CTrackNameImportPreview preview) throws IOException {
        Objects.requireNonNull(preview, "preview");
        return CTrackNameRepository.importCustomNames(preview.compatibleNames());
    }

    private static ParsedTransferFile readTransferFile(Path path) throws CTrackNameImportException {
        if (!Files.isRegularFile(path)) {
            throw CTrackNameImportException.ioError(null);
        }

        try {
            if (Files.size(path) == 0L) {
                throw CTrackNameImportException.ioError(null);
            }
        } catch (IOException exception) {
            throw CTrackNameImportException.ioError(exception);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonReader jsonReader = new JsonReader(reader);
            if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                throw CTrackNameImportException.invalidTrackNameFile();
            }

            String modId = null;
            boolean sawModId = false;
            boolean modIdReadable = false;
            List<RawTrackNameEntry> entries = null;
            boolean sawTrackNames = false;

            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if (MOD_ID_KEY.equals(name)) {
                    if (sawModId) {
                        throw CTrackNameImportException.invalidTrackNameFile();
                    }
                    sawModId = true;
                    if (jsonReader.peek() == JsonToken.STRING) {
                        modId = jsonReader.nextString();
                        modIdReadable = true;
                    } else {
                        jsonReader.skipValue();
                    }
                } else if (TRACK_NAMES_KEY.equals(name)) {
                    if (sawTrackNames) {
                        throw CTrackNameImportException.invalidTrackNameFile();
                    }
                    sawTrackNames = true;
                    if (jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
                        entries = readTrackNameEntries(jsonReader);
                    } else {
                        jsonReader.skipValue();
                    }
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();

            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw CTrackNameImportException.invalidTrackNameFile();
            }
            if (!modIdReadable || entries == null) {
                throw CTrackNameImportException.invalidTrackNameFile();
            }

            return new ParsedTransferFile(modId, entries);
        } catch (CTrackNameImportException exception) {
            throw exception;
        } catch (JsonParseException | IllegalStateException | MalformedJsonException exception) {
            throw CTrackNameImportException.invalidTrackNameFile(exception);
        } catch (IOException exception) {
            throw CTrackNameImportException.ioError(exception);
        }
    }

    private static List<RawTrackNameEntry> readTrackNameEntries(JsonReader jsonReader) throws IOException {
        List<RawTrackNameEntry> entries = new ArrayList<>();
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String rawTrackId = jsonReader.nextName();
            JsonElement value = JsonParser.parseReader(jsonReader);
            entries.add(new RawTrackNameEntry(rawTrackId, value));
        }
        jsonReader.endObject();
        return entries;
    }

    private static CTrackNameImportPreview scanTrackNames(
        List<RawTrackNameEntry> entries,
        Collection<Identifier> availableTracks,
        Map<String, String> currentNames
    ) {
        List<String> invalidTrackIds = new ArrayList<>();
        LinkedHashSet<Identifier> duplicateTrackIds = new LinkedHashSet<>();
        List<Identifier> nonStringNameTrackIds = new ArrayList<>();
        List<Identifier> emptyNameTrackIds = new ArrayList<>();
        List<CTrackNameImportPreview.TrackName> tooLongNames = new ArrayList<>();
        List<CTrackNameImportPreview.TrackName> defaultNames = new ArrayList<>();
        List<CTrackNameImportPreview.NameConflict> nameConflicts = new ArrayList<>();
        List<CTrackNameImportPreview.Replacement> replacements = new ArrayList<>();

        List<ParsedRawEntry> parsedEntries = new ArrayList<>();
        Map<Identifier, Integer> occurrenceCounts = new HashMap<>();
        for (RawTrackNameEntry entry : entries) {
            Identifier trackId = Identifier.tryParse(entry.rawTrackId());
            if (trackId == null || trackId.getPath().isEmpty()) {
                invalidTrackIds.add(entry.rawTrackId());
            } else {
                occurrenceCounts.merge(trackId, 1, Integer::sum);
            }
            parsedEntries.add(new ParsedRawEntry(trackId, entry.value()));
        }

        for (ParsedRawEntry entry : parsedEntries) {
            if (entry.trackId() != null && occurrenceCounts.get(entry.trackId()) > 1) {
                duplicateTrackIds.add(entry.trackId());
            }
        }

        LinkedHashMap<Identifier, String> candidates = new LinkedHashMap<>();
        for (ParsedRawEntry entry : parsedEntries) {
            Identifier trackId = entry.trackId();
            if (trackId == null || duplicateTrackIds.contains(trackId)) {
                continue;
            }

            if (!(entry.value() instanceof JsonPrimitive primitive) || !primitive.isString()) {
                nonStringNameTrackIds.add(trackId);
                continue;
            }

            String name = primitive.getAsString().trim();
            if (name.isEmpty()) {
                emptyNameTrackIds.add(trackId);
                continue;
            }
            if (name.length() > CTrackNameRepository.MAX_TRACK_NAME_LENGTH) {
                tooLongNames.add(new CTrackNameImportPreview.TrackName(trackId, name));
                continue;
            }
            if (name.equals(CTrackNameRepository.getDefaultDisplayName(trackId))) {
                defaultNames.add(new CTrackNameImportPreview.TrackName(trackId, name));
                continue;
            }

            candidates.put(trackId, name);
        }

        LinkedHashSet<Identifier> knownTracks = new LinkedHashSet<>(availableTracks);
        for (String rawTrackId : currentNames.keySet()) {
            Identifier trackId = Identifier.tryParse(rawTrackId);
            if (trackId != null && !trackId.getPath().isEmpty()) {
                knownTracks.add(trackId);
            }
        }
        knownTracks.addAll(candidates.keySet());

        LinkedHashMap<Identifier, String> activeCandidates = new LinkedHashMap<>();
        LinkedHashSet<Identifier> unchangedCandidates = new LinkedHashSet<>();
        Map<Identifier, Integer> candidateOrder = new HashMap<>();
        int candidateIndex = 0;
        for (Map.Entry<Identifier, String> candidate : candidates.entrySet()) {
            candidateOrder.put(candidate.getKey(), candidateIndex++);
            if (candidate.getValue().equals(currentDisplayName(candidate.getKey(), currentNames))) {
                unchangedCandidates.add(candidate.getKey());
            } else {
                activeCandidates.put(candidate.getKey(), candidate.getValue());
            }
        }

        Map<Identifier, OccupiedName> conflictByTrack = resolveNameConflicts(
            knownTracks,
            activeCandidates,
            candidateOrder,
            currentNames
        );
        LinkedHashMap<Identifier, String> compatibleNames = new LinkedHashMap<>();
        for (Map.Entry<Identifier, String> candidate : candidates.entrySet()) {
            Identifier trackId = candidate.getKey();
            String importedName = candidate.getValue();
            OccupiedName conflicting = conflictByTrack.get(trackId);
            if (conflicting != null) {
                nameConflicts.add(new CTrackNameImportPreview.NameConflict(
                    new CTrackNameImportPreview.TrackName(trackId, importedName),
                    new CTrackNameImportPreview.TrackName(conflicting.trackId(), conflicting.displayName())
                ));
                continue;
            }
            if (!unchangedCandidates.contains(trackId) && !activeCandidates.containsKey(trackId)) {
                continue;
            }

            compatibleNames.put(trackId, importedName);
            String currentCustomName = currentNames.get(trackId.toString());
            if (currentCustomName != null && !currentCustomName.equals(importedName)) {
                replacements.add(new CTrackNameImportPreview.Replacement(
                    trackId,
                    currentCustomName,
                    importedName
                ));
            }
        }

        return new CTrackNameImportPreview(
            compatibleNames,
            invalidTrackIds,
            List.copyOf(duplicateTrackIds),
            nonStringNameTrackIds,
            emptyNameTrackIds,
            tooLongNames,
            defaultNames,
            nameConflicts,
            replacements
        );
    }

    private static Map<Identifier, OccupiedName> resolveNameConflicts(
        Collection<Identifier> knownTracks,
        LinkedHashMap<Identifier, String> activeCandidates,
        Map<Identifier, Integer> candidateOrder,
        Map<String, String> currentNames
    ) {
        LinkedHashMap<Identifier, OccupiedName> conflicts = new LinkedHashMap<>();
        while (true) {
            Map<NameKey, List<OccupiedName>> finalNames = new LinkedHashMap<>();
            for (Identifier trackId : knownTracks) {
                String displayName = activeCandidates.getOrDefault(
                    trackId,
                    currentDisplayName(trackId, currentNames)
                );
                finalNames.computeIfAbsent(
                    new NameKey(trackId.getNamespace(), displayName.toLowerCase(Locale.ROOT)),
                    ignored -> new ArrayList<>()
                ).add(new OccupiedName(trackId, displayName));
            }

            LinkedHashMap<Identifier, OccupiedName> rejectedThisPass = new LinkedHashMap<>();
            for (List<OccupiedName> collidingTracks : finalNames.values()) {
                if (collidingTracks.size() < 2) {
                    continue;
                }

                List<OccupiedName> activeCollisions = collidingTracks.stream()
                    .filter(track -> activeCandidates.containsKey(track.trackId()))
                    .toList();
                if (activeCollisions.isEmpty()) {
                    continue;
                }

                OccupiedName unchangedCollision = collidingTracks.stream()
                    .filter(track -> !activeCandidates.containsKey(track.trackId()))
                    .findFirst()
                    .orElse(null);
                if (unchangedCollision != null) {
                    for (OccupiedName activeCollision : activeCollisions) {
                        rejectedThisPass.putIfAbsent(activeCollision.trackId(), unchangedCollision);
                    }
                    continue;
                }

                OccupiedName winner = activeCollisions.stream()
                    .min((left, right) -> Integer.compare(
                        candidateOrder.get(left.trackId()),
                        candidateOrder.get(right.trackId())
                    ))
                    .orElseThrow();
                for (OccupiedName activeCollision : activeCollisions) {
                    if (!activeCollision.trackId().equals(winner.trackId())) {
                        rejectedThisPass.putIfAbsent(activeCollision.trackId(), winner);
                    }
                }
            }

            if (rejectedThisPass.isEmpty()) {
                return conflicts;
            }
            for (Map.Entry<Identifier, OccupiedName> rejection : rejectedThisPass.entrySet()) {
                activeCandidates.remove(rejection.getKey());
                conflicts.putIfAbsent(rejection.getKey(), rejection.getValue());
            }
        }
    }

    private static String currentDisplayName(Identifier trackId, Map<String, String> currentNames) {
        String customName = currentNames.get(trackId.toString());
        return customName == null ? CTrackNameRepository.getDefaultDisplayName(trackId) : customName;
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
        return sanitized.toLowerCase(Locale.ROOT).equals(UNKNOWN_VERSION) ? UNKNOWN_VERSION : sanitized;
    }

    private record ParsedTransferFile(String modId, List<RawTrackNameEntry> entries) {
    }

    private record RawTrackNameEntry(String rawTrackId, JsonElement value) {
    }

    private record ParsedRawEntry(Identifier trackId, JsonElement value) {
    }

    private record OccupiedName(Identifier trackId, String displayName) {
    }

    private record NameKey(String namespace, String displayNameLower) {
    }
}
