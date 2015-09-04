package alien4cloud.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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
            URL url = new URL(candidateUrl);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
