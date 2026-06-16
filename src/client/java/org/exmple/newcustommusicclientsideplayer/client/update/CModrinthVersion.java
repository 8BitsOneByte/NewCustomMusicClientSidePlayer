package org.exmple.newcustommusicclientsideplayer.client.update;

import java.util.List;
import java.util.Objects;

public final class CModrinthVersion {
    private final String id;
    private final String versionNumber;
    private final String versionType;
    private final String status;
    private final List<String> loaders;
    private final List<String> gameVersions;
    private final String datePublished;
    private final String changelog;
    private final boolean hasFiles;
    private final String versionPageUrl;

    public CModrinthVersion(
        String id,
        String versionNumber,
        String versionType,
        String status,
        List<String> loaders,
        List<String> gameVersions,
        String datePublished,
        String changelog,
        boolean hasFiles,
        String versionPageUrl
    ) {
        this.id = normalizeNullableText(id);
        this.versionNumber = normalizeNullableText(versionNumber);
        this.versionType = normalizeNullableText(versionType);
        this.status = normalizeNullableText(status);
        this.loaders = List.copyOf(Objects.requireNonNull(loaders, "loaders"));
        this.gameVersions = List.copyOf(Objects.requireNonNull(gameVersions, "gameVersions"));
        this.datePublished = normalizeNullableText(datePublished);
        this.changelog = normalizeNullableText(changelog);
        this.hasFiles = hasFiles;
        this.versionPageUrl = normalizeNullableText(versionPageUrl);
    }

    public String id() {
        return this.id;
    }

    public String versionNumber() {
        return this.versionNumber;
    }

    public String versionType() {
        return this.versionType;
    }

    public String status() {
        return this.status;
    }

    public List<String> loaders() {
        return this.loaders;
    }

    public List<String> gameVersions() {
        return this.gameVersions;
    }

    public String datePublished() {
        return this.datePublished;
    }

    public String changelog() {
        return this.changelog;
    }

    public boolean hasFiles() {
        return this.hasFiles;
    }

    public String versionPageUrl() {
        return this.versionPageUrl;
    }

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
