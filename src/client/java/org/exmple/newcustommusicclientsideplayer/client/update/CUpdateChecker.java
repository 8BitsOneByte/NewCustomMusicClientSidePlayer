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
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.loader.api.FabricLoader;

public final class CUpdateChecker {
    private static final String MOD_ID = "newcustommusicclientsideplayer";
    private static final String PROJECT_SLUG = "new-custom-music-client-side-player";
    private static final String PROJECT_URL = "https://modrinth.com/mod/new-custom-music-client-side-player";
    private static final String LOADER = "fabric";
    private static final String CACHE_FILE_NAME = "update_cache.json";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new UpdateThreadFactory());
    private static final AtomicBoolean CHECK_RUNNING = new AtomicBoolean(false);

    private static volatile CUpdateEnvironment environment;
    private static volatile CUpdateStatus status;

    private CUpdateChecker() {
    }

    public static void initialize() {
        CUpdateEnvironment createdEnvironment = createEnvironment();
        environment = createdEnvironment;

        CUpdateCache cache = CUpdateCache.forEnvironment(createdEnvironment);
        CUpdateCache.Snapshot snapshot = cache.load(createdEnvironment);
        status = snapshot.status();

        long now = System.currentTimeMillis();
        if (snapshot.hasFreshSuccessfulStatus(createdEnvironment, now) || !snapshot.canAttempt(now)) {
            return;
        }

        startAsyncCheck(createdEnvironment, cache, snapshot);
    }

    public static CUpdateStatus getStatus() {
        CUpdateStatus current = status;
        CUpdateEnvironment currentEnvironment = environment;
        if (current != null) {
            return current;
        }
        if (currentEnvironment != null) {
            return CUpdateStatus.unknown(currentEnvironment);
        }

        CUpdateEnvironment createdEnvironment = createEnvironment();
        environment = createdEnvironment;
        CUpdateStatus unknown = CUpdateStatus.unknown(createdEnvironment);
        status = unknown;
        return unknown;
    }

    private static void startAsyncCheck(
        CUpdateEnvironment currentEnvironment,
        CUpdateCache cache,
        CUpdateCache.Snapshot snapshot
    ) {
        if (!CHECK_RUNNING.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                checkNow(currentEnvironment, cache, snapshot);
            } finally {
                CHECK_RUNNING.set(false);
            }
        }, EXECUTOR);
    }

    private static void checkNow(CUpdateEnvironment currentEnvironment, CUpdateCache cache, CUpdateCache.Snapshot snapshot) {
        long now = System.currentTimeMillis();
        Optional<List<CModrinthVersion>> fetchedVersions = new CModrinthUpdateClient().fetchProjectVersions(currentEnvironment);
        if (fetchedVersions.isEmpty()) {
            cache.saveFailure(currentEnvironment, snapshot, now);
            CUpdateCache.Snapshot updatedSnapshot = cache.load(currentEnvironment);
            status = updatedSnapshot.status();
            return;
        }

        CUpdateStatus checkedStatus = selectLatestUpdate(currentEnvironment, fetchedVersions.get(), now)
            .orElseGet(() -> CUpdateStatus.upToDate(currentEnvironment, now));
        status = checkedStatus;
        cache.saveSuccess(currentEnvironment, checkedStatus, now);
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
