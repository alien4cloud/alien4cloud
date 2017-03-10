package alien4cloud.rest.deployment.model;

import alien4cloud.utils.AlienConstants;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class ScrollTimedRequest {
    /** number of results to get. */
    private int size;
    /** The beginning of the interval for which to get data. */
    @NonNull
    private Long intervalStart;
    /** Optional end interval. */
    private Long intervalEnd;

    /**
     * Set the value for the size (maximum number of elements to return in the request).
     *
     * @param size Maximum number of elements to return in the request. If null will be set to 50. Cannot be more than 100.
     */
    public void setSize(Integer size) {
        if (size == null) {
            this.size = AlienConstants.DEFAULT_ES_SEARCH_SIZE;
        } else if (size > AlienConstants.MAX_ES_SEARCH_SIZE) {
            this.size = AlienConstants.MAX_ES_SEARCH_SIZE;
        } else {
            this.size = size;
        }
    }
}
