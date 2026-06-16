package org.exmple.newcustommusicclientsideplayer.client.update;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CSemanticVersion implements Comparable<CSemanticVersion> {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

    private final int major;
    private final int minor;
    private final int patch;

    private CSemanticVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static Optional<CSemanticVersion> parse(String version) {
        if (version == null) {
            return Optional.empty();
        }

        Matcher matcher = VERSION_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new CSemanticVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
            ));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public int major() {
        return this.major;
    }

    public int minor() {
        return this.minor;
    }

    public int patch() {
        return this.patch;
    }

    public boolean isNewerThan(CSemanticVersion other) {
        return this.compareTo(other) > 0;
    }

    @Override
    public int compareTo(CSemanticVersion other) {
        int majorComparison = Integer.compare(this.major, other.major);
        if (majorComparison != 0) {
            return majorComparison;
        }

        int minorComparison = Integer.compare(this.minor, other.minor);
        if (minorComparison != 0) {
            return minorComparison;
        }

        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CSemanticVersion other)) {
            return false;
        }

        return this.major == other.major
            && this.minor == other.minor
            && this.patch == other.patch;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(this.major);
        result = 31 * result + Integer.hashCode(this.minor);
        result = 31 * result + Integer.hashCode(this.patch);
        return result;
    }

    @Override
    public String toString() {
        return this.major + "." + this.minor + "." + this.patch;
    }
}
