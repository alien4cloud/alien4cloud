package alien4cloud.rest.model;

import alien4cloud.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A basic search request object
 * 
 * @author 'Igor Ngouagna'
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class BasicSearchRequest {
    /* The component type to query */
    private String query;
    private Integer from;
    private Integer size;

    /**
     * Set the value for 'from': start element in the request.
     * 
     * @param from Initial element index for the request. If null the value will be set to 0.
     */
    public void setFrom(Integer from) {
        if (from == null) {
            this.from = 0;
        } else {
            this.from = from;
        }
    }

    /**
     * Set the value for the size (maximum number of elements to return in the request).
     * 
     * @param size Maximum number of elements to return in the request. If null will be set to 50. Cannot be more than 100.
     */
    public void setSize(Integer size) {
        if (size == null) {
            this.size = Constants.DEFAULT_ES_SEARCH_SIZE;
        } else if (size > Constants.MAX_ES_SEARCH_SIZE) {
            this.size = Constants.MAX_ES_SEARCH_SIZE;
        } else {
            this.size = size;
        }
    }
}