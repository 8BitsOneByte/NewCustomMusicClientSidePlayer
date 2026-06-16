package org.exmple.newcustommusicclientsideplayer.client.update;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class CUpdatePolicy {
    public static final long SUCCESS_TTL_MILLIS = Duration.ofHours(24L).toMillis();
    public static final long FAILURE_RETRY_MIN_MILLIS = Duration.ofMinutes(5L).toMillis();
    public static final long FAILURE_RETRY_JITTER_MILLIS = Duration.ofMinutes(5L).toMillis();
    public static final int MAX_CONSECUTIVE_FAILURES = 3;
    public static final long FAILURE_COOLDOWN_MILLIS = Duration.ofHours(24L).toMillis();
    public static final int REQUEST_TIMEOUT_SECONDS = 4;
    public static final int CHANGELOG_PREVIEW_LIMIT = 100;

    private CUpdatePolicy() {
    }

    public static boolean isSuccessfulStatusFresh(CUpdateStatus status, CUpdateEnvironment environment, long nowEpochMillis) {
        if (status == null || !isEnvironmentMatching(status, environment)) {
            return false;
        }

        if (status.state() != CUpdateState.UP_TO_DATE && status.state() != CUpdateState.UPDATE_AVAILABLE) {
            return false;
        }

        long checkedAt = status.checkedAtEpochMillis();
        return checkedAt > 0L && nowEpochMillis >= checkedAt && nowEpochMillis - checkedAt < SUCCESS_TTL_MILLIS;
    }

    public static boolean isEnvironmentMatching(CUpdateStatus status, CUpdateEnvironment environment) {
        if (status == null || environment == null) {
            return false;
        }

        return status.loader().equals(environment.loader())
            && status.minecraftVersion().equals(environment.minecraftVersion())
            && status.currentModVersion().equals(environment.currentModVersion());
    }

    public static long nextFailureAttemptAfter(long nowEpochMillis, int consecutiveFailures) {
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            return nowEpochMillis + FAILURE_COOLDOWN_MILLIS;
        }

        long jitter = ThreadLocalRandom.current().nextLong(FAILURE_RETRY_JITTER_MILLIS + 1L);
        return nowEpochMillis + FAILURE_RETRY_MIN_MILLIS + jitter;
    }

    public static String truncateChangelog(String changelog) {
        if (changelog == null) {
            return "";
        }

        String trimmed = changelog.trim();
        if (trimmed.length() <= CHANGELOG_PREVIEW_LIMIT) {
            return trimmed;
        }

        return trimmed.substring(0, CHANGELOG_PREVIEW_LIMIT).trim();
    }
}
