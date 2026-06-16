package org.exmple.newcustommusicclientsideplayer.client.update;

import java.nio.file.Path;
import java.util.Objects;

public final class CUpdateEnvironment {
    private final String modId;
    private final String projectSlug;
    private final String projectUrl;
    private final String loader;
    private final String minecraftVersion;
    private final String currentModVersion;
    private final String userAgent;
    private final Path cachePath;

    public CUpdateEnvironment(
        String modId,
        String projectSlug,
        String projectUrl,
        String loader,
        String minecraftVersion,
        String currentModVersion,
        String userAgent,
        Path cachePath
    ) {
        this.modId = requireText(modId, "modId");
        this.projectSlug = requireText(projectSlug, "projectSlug");
        this.projectUrl = requireText(projectUrl, "projectUrl");
        this.loader = requireText(loader, "loader");
        this.minecraftVersion = requireText(minecraftVersion, "minecraftVersion");
        this.currentModVersion = requireText(currentModVersion, "currentModVersion");
        this.userAgent = requireText(userAgent, "userAgent");
        this.cachePath = Objects.requireNonNull(cachePath, "cachePath");
    }

    public String modId() {
        return this.modId;
    }

    public String projectSlug() {
        return this.projectSlug;
    }

    public String projectUrl() {
        return this.projectUrl;
    }

    public String loader() {
        return this.loader;
    }

    public String minecraftVersion() {
        return this.minecraftVersion;
    }

    public String currentModVersion() {
        return this.currentModVersion;
    }

    public String userAgent() {
        return this.userAgent;
    }

    public Path cachePath() {
        return this.cachePath;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return trimmed;
    }
}
