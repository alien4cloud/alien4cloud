package alien4cloud.utils;

import alien4cloud.utils.version.InvalidVersionException;
import alien4cloud.utils.version.Version;

import java.util.regex.Pattern;

public final class VersionUtil {

    /**
     * Default version
     */
    public static final String DEFAULT_VERSION_NAME = "0.1.0-SNAPSHOT";

    /** Utility class should not have public constructor. */
    private VersionUtil() {
    }

    /**
     * The version must begin with a bloc of numbers, and then it can have one or more bloc of numbers separated by '.'
     * and then it can have alpha numeric bloc separated by '.' or '-'
     */
    public static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*(?:[\\.-]\\p{Alnum}+)*");
    private static final String SNAPSHOT_IDENTIFIER = "SNAPSHOT";

    /**
     * Check if a version is a SNAPSHOT (development) version.
     * 
     * @param version The actual version string.
     * @return True if the version is a SNAPSHOT version, false if not (RELEASE version).
     */
    public static boolean isSnapshot(String version) {
        return version.toUpperCase().contains(SNAPSHOT_IDENTIFIER);
    }

    /**
     * Check if a version is valid
     * 
     * @param version version string to parse
     * @return true if it's following the defined version pattern
     */
    public static boolean isValid(String version) {
        return version != null && VERSION_PATTERN.matcher(version).matches();
    }

    /**
     * Parse the version's text to produce a comparable version object
     * 
     * @param version version text to parse
     * @return a comparable version object
     * @throws alien4cloud.utils.version.InvalidVersionException if the version text is not following the defined version pattern
     */
    public static Version parseVersion(String version) {
        if (!isValid(version)) {
            throw new InvalidVersionException("This version is not valid [" + version + "] as it does not match [" + VERSION_PATTERN + "]");
        } else {
            return new Version(version);
        }
    }

    /**
     * Compare 2 versions
     * 
     * @param versionLeft
     * @param versionRight
     * @return
     */
    public static int compare(String versionLeft, String versionRight) {
        return parseVersion(versionLeft).compareTo(parseVersion(versionRight));
    }
}