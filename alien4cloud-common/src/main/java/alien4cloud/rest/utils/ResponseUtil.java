package alien4cloud.rest.utils;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

/**
 * Utility class to convert Http response to string content.
 */
public final class ResponseUtil {
    private ResponseUtil() {
    }

    /**
     * Get the string content out of a {@link CloseableHttpResponse} and close it.
     * 
     * @param response The response from which to get the content as string.
     * @return The content of the response as a string.
     * @throws IOException In case we fail to read the content of the response in a string.
     */
    public static String toString(CloseableHttpResponse response) throws IOException {
        try {
            return EntityUtils.toString(response.getEntity());
        } finally {
            response.close();
        }
    }
}