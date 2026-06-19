package org.exmple.newcustommusicclientsideplayer.client.update;

import java.util.Objects;

public final class CUpdateStatus {
    private final CUpdateState state;
    private final String latestVersion;
    private final String publishedAt;
    private final String changelogPreview;
    private final String versionPageUrl;
    private final long checkedAtEpochMillis;
    private final String minecraftVersion;
    private final String currentModVersion;
    private final String loader;

    private CUpdateStatus(
        CUpdateState state,
        String latestVersion,
        String publishedAt,
        String changelogPreview,
        String versionPageUrl,
        long checkedAtEpochMillis,
        String minecraftVersion,
        String currentModVersion,
        String loader
    ) {
        this.state = Objects.requireNonNull(state, "state");
        this.latestVersion = normalizeNullableText(latestVersion);
        this.publishedAt = normalizeNullableText(publishedAt);
        this.changelogPreview = normalizeNullableText(changelogPreview);
        this.versionPageUrl = normalizeNullableText(versionPageUrl);
        this.checkedAtEpochMillis = checkedAtEpochMillis;
        this.minecraftVersion = normalizeNullableText(minecraftVersion);
        this.currentModVersion = normalizeNullableText(currentModVersion);
        this.loader = normalizeNullableText(loader);
    }

    public static CUpdateStatus unknown(CUpdateEnvironment environment) {
        return new CUpdateStatus(
            CUpdateState.UNKNOWN,
            "",
            "",
            "",
            "",
            0L,
            environment.minecraftVersion(),
            environment.currentModVersion(),
            environment.loader()
        );
    }

    public static CUpdateStatus disabled(CUpdateEnvironment environment) {
        return new CUpdateStatus(
            CUpdateState.DISABLED,
            "",
            "",
            "",
            "",
            0L,
            environment.minecraftVersion(),
            environment.currentModVersion(),
            environment.loader()
        );
    }

    public static CUpdateStatus upToDate(CUpdateEnvironment environment, long checkedAtEpochMillis) {
        return new CUpdateStatus(
            CUpdateState.UP_TO_DATE,
            "",
            "",
            "",
            "",
            checkedAtEpochMillis,
            environment.minecraftVersion(),
            environment.currentModVersion(),
            environment.loader()
        );
    }

    public static CUpdateStatus updateAvailable(
        CUpdateEnvironment environment,
        String latestVersion,
        String publishedAt,
        String changelogPreview,
        String versionPageUrl,
        long checkedAtEpochMillis
    ) {
        return new CUpdateStatus(
            CUpdateState.UPDATE_AVAILABLE,
            latestVersion,
            publishedAt,
            changelogPreview,
            versionPageUrl,
            checkedAtEpochMillis,
            environment.minecraftVersion(),
            environment.currentModVersion(),
            environment.loader()
        );
    }

    public static CUpdateStatus checkFailed(CUpdateEnvironment environment, long checkedAtEpochMillis) {
        return new CUpdateStatus(
            CUpdateState.CHECK_FAILED,
            "",
            "",
            "",
            "",
            checkedAtEpochMillis,
            environment.minecraftVersion(),
            environment.currentModVersion(),
            environment.loader()
        );
    }

    public CUpdateState state() {
        return this.state;
    }

    public boolean updateAvailable() {
        return this.state == CUpdateState.UPDATE_AVAILABLE;
    }

    public String latestVersion() {
        return this.latestVersion;
    }

    public String publishedAt() {
        return this.publishedAt;
    }

    public String changelogPreview() {
        return this.changelogPreview;
    }

    public String versionPageUrl() {
        return this.versionPageUrl;
    }

    public long checkedAtEpochMillis() {
        return this.checkedAtEpochMillis;
    }

    public String minecraftVersion() {
        return this.minecraftVersion;
    }

    public String currentModVersion() {
        return this.currentModVersion;
    }

    public String loader() {
        return this.loader;
    }

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
