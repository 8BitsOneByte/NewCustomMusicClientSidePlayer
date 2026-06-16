package org.exmple.newcustommusicclientsideplayer.client.update;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class CUpdateCache {
    private static final Gson GSON = new Gson();

    private final Path cachePath;

    public CUpdateCache(Path cachePath) {
        this.cachePath = cachePath;
    }

    public static CUpdateCache forEnvironment(CUpdateEnvironment environment) {
        return new CUpdateCache(environment.cachePath());
    }

    public Snapshot load(CUpdateEnvironment environment) {
        if (!Files.isRegularFile(this.cachePath)) {
            return Snapshot.empty(environment);
        }

        try (Reader reader = Files.newBufferedReader(this.cachePath)) {
            CacheData data = GSON.fromJson(reader, CacheData.class);
            if (data == null || !data.matches(environment)) {
                return Snapshot.empty(environment);
            }

            return data.toSnapshot(environment);
        } catch (IOException | RuntimeException exception) {
            return Snapshot.empty(environment);
        }
    }

    public void saveSuccess(CUpdateEnvironment environment, CUpdateStatus status, long nowEpochMillis) {
        CacheData data = CacheData.fromSnapshot(environment, new Snapshot(
            status,
            nowEpochMillis,
            nowEpochMillis,
            nowEpochMillis + CUpdatePolicy.SUCCESS_TTL_MILLIS,
            0
        ));
        write(data);
    }

    public void saveFailure(CUpdateEnvironment environment, Snapshot previousSnapshot, long nowEpochMillis) {
        Snapshot previous = previousSnapshot == null ? Snapshot.empty(environment) : previousSnapshot;
        int consecutiveFailures = previous.consecutiveFailures() + 1;
        long nextAttemptAfter = CUpdatePolicy.nextFailureAttemptAfter(nowEpochMillis, consecutiveFailures);
        CUpdateStatus preservedStatus = previous.hasReusableSuccessfulStatus(environment)
            ? previous.status()
            : CUpdateStatus.checkFailed(environment, nowEpochMillis);

        CacheData data = CacheData.fromSnapshot(environment, new Snapshot(
            preservedStatus,
            previous.lastSuccessfulCheckAt(),
            nowEpochMillis,
            nextAttemptAfter,
            consecutiveFailures
        ));
        write(data);
    }

    private void write(CacheData data) {
        try {
            Path parent = this.cachePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(this.cachePath)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }

    public static final class Snapshot {
        private final CUpdateStatus status;
        private final long lastSuccessfulCheckAt;
        private final long lastAttemptAt;
        private final long nextAttemptAfter;
        private final int consecutiveFailures;

        private Snapshot(
            CUpdateStatus status,
            long lastSuccessfulCheckAt,
            long lastAttemptAt,
            long nextAttemptAfter,
            int consecutiveFailures
        ) {
            this.status = status;
            this.lastSuccessfulCheckAt = Math.max(0L, lastSuccessfulCheckAt);
            this.lastAttemptAt = Math.max(0L, lastAttemptAt);
            this.nextAttemptAfter = Math.max(0L, nextAttemptAfter);
            this.consecutiveFailures = Math.max(0, consecutiveFailures);
        }

        public static Snapshot empty(CUpdateEnvironment environment) {
            return new Snapshot(CUpdateStatus.unknown(environment), 0L, 0L, 0L, 0);
        }

        public CUpdateStatus status() {
            return this.status;
        }

        public long lastSuccessfulCheckAt() {
            return this.lastSuccessfulCheckAt;
        }

        public long lastAttemptAt() {
            return this.lastAttemptAt;
        }

        public long nextAttemptAfter() {
            return this.nextAttemptAfter;
        }

        public int consecutiveFailures() {
            return this.consecutiveFailures;
        }

        public boolean canAttempt(long nowEpochMillis) {
            return this.nextAttemptAfter <= 0L || nowEpochMillis >= this.nextAttemptAfter;
        }

        public boolean hasFreshSuccessfulStatus(CUpdateEnvironment environment, long nowEpochMillis) {
            return CUpdatePolicy.isSuccessfulStatusFresh(this.status, environment, nowEpochMillis);
        }

        private boolean hasReusableSuccessfulStatus(CUpdateEnvironment environment) {
            return CUpdatePolicy.isEnvironmentMatching(this.status, environment)
                && (this.status.state() == CUpdateState.UP_TO_DATE || this.status.state() == CUpdateState.UPDATE_AVAILABLE);
        }
    }

    private static final class CacheData {
        private String projectSlug = "";
        private String loader = "";
        private String minecraftVersion = "";
        private String currentModVersion = "";
        private String state = CUpdateState.UNKNOWN.name();
        private String latestVersion = "";
        private String publishedAt = "";
        private String changelogPreview = "";
        private String versionPageUrl = "";
        private long checkedAtEpochMillis;
        private long lastSuccessfulCheckAt;
        private long lastAttemptAt;
        private long nextAttemptAfter;
        private int consecutiveFailures;

        private boolean matches(CUpdateEnvironment environment) {
            return this.projectSlug.equals(environment.projectSlug())
                && this.loader.equals(environment.loader())
                && this.minecraftVersion.equals(environment.minecraftVersion())
                && this.currentModVersion.equals(environment.currentModVersion());
        }

        private Snapshot toSnapshot(CUpdateEnvironment environment) {
            CUpdateState parsedState = parseState(this.state);
            CUpdateStatus status = switch (parsedState) {
                case UP_TO_DATE -> CUpdateStatus.upToDate(environment, this.checkedAtEpochMillis);
                case UPDATE_AVAILABLE -> CUpdateStatus.updateAvailable(
                    environment,
                    this.latestVersion,
                    this.publishedAt,
                    this.changelogPreview,
                    this.versionPageUrl,
                    this.checkedAtEpochMillis
                );
                case CHECK_FAILED -> CUpdateStatus.checkFailed(environment, this.checkedAtEpochMillis);
                case UNKNOWN -> CUpdateStatus.unknown(environment);
            };

            return new Snapshot(
                status,
                this.lastSuccessfulCheckAt,
                this.lastAttemptAt,
                this.nextAttemptAfter,
                this.consecutiveFailures
            );
        }

        private static CacheData fromSnapshot(CUpdateEnvironment environment, Snapshot snapshot) {
            CUpdateStatus status = snapshot.status();
            CacheData data = new CacheData();
            data.projectSlug = environment.projectSlug();
            data.loader = environment.loader();
            data.minecraftVersion = environment.minecraftVersion();
            data.currentModVersion = environment.currentModVersion();
            data.state = status.state().name();
            data.latestVersion = status.latestVersion();
            data.publishedAt = status.publishedAt();
            data.changelogPreview = status.changelogPreview();
            data.versionPageUrl = status.versionPageUrl();
            data.checkedAtEpochMillis = status.checkedAtEpochMillis();
            data.lastSuccessfulCheckAt = snapshot.lastSuccessfulCheckAt();
            data.lastAttemptAt = snapshot.lastAttemptAt();
            data.nextAttemptAfter = snapshot.nextAttemptAfter();
            data.consecutiveFailures = snapshot.consecutiveFailures();
            return data;
        }

        private static CUpdateState parseState(String state) {
            return Optional.ofNullable(state)
                .map(value -> {
                    try {
                        return CUpdateState.valueOf(value);
                    } catch (IllegalArgumentException exception) {
                        return CUpdateState.UNKNOWN;
                    }
                })
                .orElse(CUpdateState.UNKNOWN);
        }
    }
}
