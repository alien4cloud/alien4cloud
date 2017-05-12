package alien4cloud.utils;

import java.util.regex.Pattern;

import alien4cloud.utils.version.InvalidVersionException;
import alien4cloud.utils.version.Version;

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
    public static final String SNAPSHOT_IDENTIFIER = "-SNAPSHOT";
    public static final Pattern QUALIFIER_PATTERN = Pattern.compile("^((?!.*snapshot)[a-zA-Z0-9\\-_]+)*$", Pattern.CASE_INSENSITIVE);

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
     * Check if a version is valid or throw a alien4cloud.utils.version.InvalidVersionException
     *
     * @param version version text to check
     * @throws alien4cloud.utils.version.InvalidVersionException if the version text is not following the defined version pattern
     */
    public static void isValidOrFail(String version) {
        if (!isValid(version)) {
            throw new InvalidVersionException("This version is not valid [" + version + "] as it does not match [" + VERSION_PATTERN + "]");
        }
    }

    /**
     * Parse the version's text to produce a comparable version object
     * 
     * @param version version text to parse
     * @return a comparable version object
     * @throws alien4cloud.utils.version.InvalidVersionException if the version text is not following the defined version pattern
     */
    public static Version parseVersion(String version) {
        isValidOrFail(version);
        return new Version(version);
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

    /**
     * Check if a qualifier is valid
     *
     * @param qualifier qualifier string to parse
     * @return true if it's following the defined pattern {@link VersionUtil#QUALIFIER_PATTERN}
     */
    public static boolean isQualifierValid(String qualifier) {
        return qualifier != null && QUALIFIER_PATTERN.matcher(qualifier).matches();
    }

    /**
     * Check if a qualifier is valid or throw a alien4cloud.utils.version.InvalidVersionException
     *
     * @param qualifier qualifier text to check
     * @throws alien4cloud.utils.version.InvalidVersionException if the qualifier text is not following the defined pattern
     */
    public static void isQualifierValidOrFail(String qualifier) {
        if (!isQualifierValid(qualifier)) {
            throw new InvalidVersionException("This qualifier [" + qualifier + "] is not valid as it does not match [" + QUALIFIER_PATTERN + "]");
        }
    }
}