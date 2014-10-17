package alien4cloud.utils;

import org.elasticsearch.action.search.SearchResponse;

/**
 * Utility class to work with elastic search responses.
 * 
 */
public final class ElasticSearchUtil {
    private ElasticSearchUtil() {
    }

    /**
     * Checks if a search response from elastic search contains results or not.
     * 
     * @param searchResponse The ES search response object.
     * @return True if the response does not contain any result, false if the response does contains results.
     */
    public static boolean isResponseEmpty(SearchResponse searchResponse) {
        if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().getHits() == null
                || searchResponse.getHits().getHits().length == 0) {
            return true;
        }
        return false;
    }
}
