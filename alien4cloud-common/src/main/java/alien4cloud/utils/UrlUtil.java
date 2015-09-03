package alien4cloud.utils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility to work with url.
 */
public final class UrlUtil {
    /**
     * Utility class should have private constructor.
     */
    private UrlUtil() {
    }

    /**
     * Check if a given string is a valid url format.
     * 
     * @param candidateUrl The candidate string to validate.
     * @return true if the candidateUrl string is a valid url string.
     */
    public static boolean isValid(String candidateUrl) {
        try {
            URI uri = new URI(candidateUrl);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
