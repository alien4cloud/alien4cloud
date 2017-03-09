package alien4cloud.dao;

import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

/**
 * Utility to process elasticsearch response.
 */
public class ResponseUtil {

    /**
     * Get the search response hits as a json string array (uses source as string and does not go through multiple jackson serialize/deserialize nor through
     * alien's object model.
     * 
     * @param response The elasticsearch search response.
     * @return Jsong array string.
     */
    public static String rawMultipleData(SearchResponse response) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (SearchHit hit : response.getHits()) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(hit.getSourceAsString());
        }
        sb.append("]");
        return sb.toString();
    }

    public static String rawMultipleData(MultiGetResponse response) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (MultiGetItemResponse getItemResponse : response.getResponses()) {
            if (getItemResponse.getResponse().isExists()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(getItemResponse.getResponse().getSourceAsString());
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
