package alien4cloud.utils;

import java.util.regex.Pattern;

import alien4cloud.utils.version.InvalidVersionException;
import alien4cloud.utils.version.Version;

public final class VersionUtil {

    /** Utility class should have default constructor. */
    private VersionUtil() {
    }

    /**
     * The version must begin with a bloc of numbers, and then it can have one or more bloc of numbers separated by '.'
     * and then it can have alpha numeric bloc separated by '.' or '-'
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*(?:[\\.-]\\p{Alnum}+)*");

    /**
     * Check if a version is valid
     * 
     * @param version version string to parse
     * @return true if it's following the defined version pattern
     */
    public static boolean isValid(String version) {
        return VERSION_PATTERN.matcher(version).matches();
    }

    /**
     * Parse the version's text to produce a comparable version object
     * 
     * @param version version text to parse
     * @return a comparable version object
     * @throws InvalidVersionException if the version text is not following the defined version pattern
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