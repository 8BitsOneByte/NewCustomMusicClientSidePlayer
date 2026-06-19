package org.exmple.newcustommusicclientsideplayer.client.update;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import net.fabricmc.loader.api.FabricLoader;

public final class CUpdateChecker {
    private static final String MOD_ID = "newcustommusicclientsideplayer";
    private static final String PROJECT_SLUG = "new-custom-music-client-side-player";
    private static final String PROJECT_URL = "https://modrinth.com/mod/new-custom-music-client-side-player";
    private static final String LOADER = "fabric";
    private static final String CACHE_FILE_NAME = "update_cache.json";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new UpdateThreadFactory());
    private static final Object STATE_LOCK = new Object();

    private static volatile CUpdateEnvironment environment;
    private static volatile CUpdateStatus status;
    private static boolean checkForUpdatesEnabled = true;
    private static long checkGeneration;

    private CUpdateChecker() {
    }

    public static void initialize() {
        initialize(true);
    }

    public static void initialize(boolean enabled) {
        applyCheckForUpdates(enabled, true);
    }

    /**
     * Applies a persisted update-check preference to the live checker. Callers should invoke this only
     * after an Apply operation has saved the selected value, not while a configuration draft is changing.
     *
     * <p>Every application advances the request generation. An asynchronous request may publish its result
     * only while update checks remain enabled and its captured generation is still current. Cache writes and
     * in-memory status updates share the same lock as preference changes, so a request that predates OFF, or
     * predates a later OFF-to-ON cycle, cannot overwrite the state or cache selected by the newer generation.</p>
     */
    public static void applyCheckForUpdates(boolean enabled) {
        applyCheckForUpdates(enabled, false);
    }

    private static void applyCheckForUpdates(boolean enabled, boolean honorRetrySchedule) {
        synchronized (STATE_LOCK) {
            CUpdateEnvironment currentEnvironment = environment;
            if (currentEnvironment == null) {
                currentEnvironment = createEnvironment();
                environment = currentEnvironment;
            }

            checkForUpdatesEnabled = enabled;
            long generation = ++checkGeneration;
            CUpdateCache cache = CUpdateCache.forEnvironment(currentEnvironment);
            if (!enabled) {
                status = CUpdateStatus.disabled(currentEnvironment);
                cache.saveDisabled(currentEnvironment);
                return;
            }

            CUpdateCache.Snapshot snapshot = enabledSnapshot(cache.load(currentEnvironment), currentEnvironment);
            status = snapshot.status();

            long now = System.currentTimeMillis();
            if (snapshot.hasFreshSuccessfulStatus(currentEnvironment, now)
                || (honorRetrySchedule && !snapshot.canAttempt(now))) {
                return;
            }

            startAsyncCheck(currentEnvironment, cache, snapshot, generation);
        }
    }

    public static CUpdateStatus getStatus() {
        CUpdateStatus current = status;
        if (current != null) {
            return current;
        }

        synchronized (STATE_LOCK) {
            if (status != null) {
                return status;
            }

            CUpdateEnvironment currentEnvironment = environment;
            if (currentEnvironment == null) {
                currentEnvironment = createEnvironment();
                environment = currentEnvironment;
            }

            status = checkForUpdatesEnabled
                ? CUpdateStatus.unknown(currentEnvironment)
                : CUpdateStatus.disabled(currentEnvironment);
            return status;
        }
    }

    private static void startAsyncCheck(
        CUpdateEnvironment currentEnvironment,
        CUpdateCache cache,
        CUpdateCache.Snapshot snapshot,
        long generation
    ) {
        CompletableFuture.runAsync(
            () -> checkNow(currentEnvironment, cache, snapshot, generation),
            EXECUTOR
        );
    }

    private static void checkNow(
        CUpdateEnvironment currentEnvironment,
        CUpdateCache cache,
        CUpdateCache.Snapshot snapshot,
        long generation
    ) {
        if (!canRunCheck(currentEnvironment, generation)) {
            return;
        }

        long now = System.currentTimeMillis();
        Optional<List<CModrinthVersion>> fetchedVersions = new CModrinthUpdateClient().fetchProjectVersions(currentEnvironment);
        if (fetchedVersions.isEmpty()) {
            commitFailure(currentEnvironment, cache, snapshot, now, generation);
            return;
        }

        CUpdateStatus checkedStatus = selectLatestUpdate(currentEnvironment, fetchedVersions.get(), now)
            .orElseGet(() -> CUpdateStatus.upToDate(currentEnvironment, now));
        commitSuccess(currentEnvironment, cache, checkedStatus, now, generation);
    }

    private static boolean canRunCheck(CUpdateEnvironment currentEnvironment, long generation) {
        synchronized (STATE_LOCK) {
            return isCurrentGeneration(currentEnvironment, generation);
        }
    }

    private static void commitFailure(
        CUpdateEnvironment currentEnvironment,
        CUpdateCache cache,
        CUpdateCache.Snapshot snapshot,
        long now,
        long generation
    ) {
        synchronized (STATE_LOCK) {
            if (!isCurrentGeneration(currentEnvironment, generation)) {
                return;
            }

            cache.saveFailure(currentEnvironment, snapshot, now);
            status = enabledSnapshot(cache.load(currentEnvironment), currentEnvironment).status();
        }
    }

    private static void commitSuccess(
        CUpdateEnvironment currentEnvironment,
        CUpdateCache cache,
        CUpdateStatus checkedStatus,
        long now,
        long generation
    ) {
        synchronized (STATE_LOCK) {
            if (!isCurrentGeneration(currentEnvironment, generation)) {
                return;
            }

            cache.saveSuccess(currentEnvironment, checkedStatus, now);
            status = checkedStatus;
        }
    }

    private static boolean isCurrentGeneration(CUpdateEnvironment currentEnvironment, long generation) {
        return checkForUpdatesEnabled
            && checkGeneration == generation
            && environment == currentEnvironment;
    }

    private static CUpdateCache.Snapshot enabledSnapshot(
        CUpdateCache.Snapshot snapshot,
        CUpdateEnvironment currentEnvironment
    ) {
        if (snapshot.status().state() == CUpdateState.DISABLED) {
            return CUpdateCache.Snapshot.empty(currentEnvironment);
        }
        return snapshot;
    }

    private static Optional<CUpdateStatus> selectLatestUpdate(
        CUpdateEnvironment currentEnvironment,
        List<CModrinthVersion> versions,
        long checkedAtEpochMillis
    ) {
        Optional<CSemanticVersion> currentVersion = CSemanticVersion.parse(currentEnvironment.currentModVersion());
        if (currentVersion.isEmpty()) {
            return Optional.empty();
        }

        return versions.stream()
            .filter(version -> isEligibleVersion(currentEnvironment, version))
            .flatMap(version -> CSemanticVersion.parse(version.versionNumber())
                .filter(parsedVersion -> parsedVersion.isNewerThan(currentVersion.get()))
                .map(parsedVersion -> new VersionCandidate(version, parsedVersion))
                .stream())
            .max(Comparator.comparing(VersionCandidate::parsedVersion))
            .map(candidate -> CUpdateStatus.updateAvailable(
                currentEnvironment,
                candidate.version().versionNumber(),
                candidate.version().datePublished(),
                CUpdatePolicy.truncateChangelog(candidate.version().changelog()),
                candidate.version().versionPageUrl(),
                checkedAtEpochMillis
            ));
    }

    private static boolean isEligibleVersion(CUpdateEnvironment currentEnvironment, CModrinthVersion version) {
        if (!"release".equalsIgnoreCase(version.versionType())) {
            return false;
        }

        if (!version.status().isBlank() && !"listed".equalsIgnoreCase(version.status())) {
            return false;
        }

        return version.hasFiles()
            && containsIgnoreCase(version.loaders(), currentEnvironment.loader())
            && containsIgnoreCase(version.gameVersions(), currentEnvironment.minecraftVersion())
            && !version.versionNumber().equals(currentEnvironment.currentModVersion());
    }

    private static boolean containsIgnoreCase(List<String> values, String expectedValue) {
        for (String value : values) {
            if (value.equalsIgnoreCase(expectedValue)) {
                return true;
            }
        }

        return false;
    }

    private static CUpdateEnvironment createEnvironment() {
        String currentModVersion = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("0.0.0");
        String minecraftVersion = FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
        Path cachePath = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(MOD_ID)
            .resolve(CACHE_FILE_NAME);
        String userAgent = "8BitsOneByte/NewCustomMusicClientSidePlayer/"
            + currentModVersion
            + " ("
            + MOD_ID
            + "; "
            + LOADER.toLowerCase(Locale.ROOT)
            + ")";

        return new CUpdateEnvironment(
            MOD_ID,
            PROJECT_SLUG,
            PROJECT_URL,
            LOADER,
            minecraftVersion,
            currentModVersion,
            userAgent,
            cachePath
        );
    }

    private record VersionCandidate(CModrinthVersion version, CSemanticVersion parsedVersion) {
    }

    private static final class UpdateThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "NewCustomMusicClientSidePlayer Update Checker");
            thread.setDaemon(true);
            return thread;
        }
    }
}
