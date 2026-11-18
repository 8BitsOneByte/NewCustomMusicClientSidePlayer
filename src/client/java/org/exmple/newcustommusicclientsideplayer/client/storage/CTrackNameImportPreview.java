package org.exmple.newcustommusicclientsideplayer.client.storage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public final class CTrackNameImportPreview {
    private Map<CTrackSourceType, CTrackNameImportPreview> groups = Map.of();
    private List<String> unknownGroups = List.of();

    public static CTrackNameImportPreview grouped(Map<CTrackSourceType, CTrackNameImportPreview> groups, List<String> unknown) {
        CTrackNameImportPreview result = new CTrackNameImportPreview(Map.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        result.groups = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(groups));
        result.unknownGroups = List.copyOf(unknown);
        return result;
    }

    public Map<CTrackSourceType, CTrackNameImportPreview> groups() { return groups; }
    public List<String> unknownGroups() { return unknownGroups; }

    private final LinkedHashMap<Identifier, String> compatibleNames;
    private final List<String> invalidTrackIds;
    private final List<Identifier> duplicateTrackIds;
    private final List<Identifier> nonStringNameTrackIds;
    private final List<Identifier> emptyNameTrackIds;
    private final List<TrackName> tooLongNames;
    private final List<TrackName> defaultNames;
    private final List<NameConflict> nameConflicts;
    private final List<Replacement> replacements;

    public CTrackNameImportPreview(
        Map<Identifier, String> compatibleNames,
        List<String> invalidTrackIds,
        List<Identifier> duplicateTrackIds,
        List<Identifier> nonStringNameTrackIds,
        List<Identifier> emptyNameTrackIds,
        List<TrackName> tooLongNames,
        List<TrackName> defaultNames,
        List<NameConflict> nameConflicts,
        List<Replacement> replacements
    ) {
        this.compatibleNames = new LinkedHashMap<>(Objects.requireNonNull(compatibleNames, "compatibleNames"));
        this.invalidTrackIds = List.copyOf(Objects.requireNonNull(invalidTrackIds, "invalidTrackIds"));
        this.duplicateTrackIds = List.copyOf(Objects.requireNonNull(duplicateTrackIds, "duplicateTrackIds"));
        this.nonStringNameTrackIds = List.copyOf(Objects.requireNonNull(nonStringNameTrackIds, "nonStringNameTrackIds"));
        this.emptyNameTrackIds = List.copyOf(Objects.requireNonNull(emptyNameTrackIds, "emptyNameTrackIds"));
        this.tooLongNames = List.copyOf(Objects.requireNonNull(tooLongNames, "tooLongNames"));
        this.defaultNames = List.copyOf(Objects.requireNonNull(defaultNames, "defaultNames"));
        this.nameConflicts = List.copyOf(Objects.requireNonNull(nameConflicts, "nameConflicts"));
        this.replacements = List.copyOf(Objects.requireNonNull(replacements, "replacements"));
    }

    public Map<Identifier, String> compatibleNames() {
        return new LinkedHashMap<>(this.compatibleNames);
    }

    public boolean hasCompatibleNames() {
        return !this.compatibleNames.isEmpty() || groups.values().stream().anyMatch(CTrackNameImportPreview::hasCompatibleNames);
    }

    public List<String> invalidTrackIds() {
        return this.invalidTrackIds;
    }

    public List<Identifier> duplicateTrackIds() {
        return this.duplicateTrackIds;
    }

    public List<Identifier> nonStringNameTrackIds() {
        return this.nonStringNameTrackIds;
    }

    public List<Identifier> emptyNameTrackIds() {
        return this.emptyNameTrackIds;
    }

    public List<TrackName> tooLongNames() {
        return this.tooLongNames;
    }

    public List<TrackName> defaultNames() {
        return this.defaultNames;
    }

    public List<NameConflict> nameConflicts() {
        return this.nameConflicts;
    }

    public List<Replacement> replacements() {
        return this.replacements;
    }

    public boolean hasWarnings() {
        return !unknownGroups.isEmpty() || groups.values().stream().anyMatch(CTrackNameImportPreview::hasWarnings) || !this.invalidTrackIds.isEmpty()
            || !this.duplicateTrackIds.isEmpty()
            || !this.nonStringNameTrackIds.isEmpty()
            || !this.emptyNameTrackIds.isEmpty()
            || !this.tooLongNames.isEmpty()
            || !this.defaultNames.isEmpty()
            || !this.nameConflicts.isEmpty()
            || !this.replacements.isEmpty();
    }

    public record TrackName(Identifier trackId, String name) {
        public TrackName {
            Objects.requireNonNull(trackId, "trackId");
            Objects.requireNonNull(name, "name");
        }
    }

    public record NameConflict(TrackName importedTrack, TrackName conflictingTrack) {
        public NameConflict {
            Objects.requireNonNull(importedTrack, "importedTrack");
            Objects.requireNonNull(conflictingTrack, "conflictingTrack");
        }
    }

    public record Replacement(Identifier trackId, String oldName, String newName) {
        public Replacement {
            Objects.requireNonNull(trackId, "trackId");
            Objects.requireNonNull(oldName, "oldName");
            Objects.requireNonNull(newName, "newName");
        }
    }
}
