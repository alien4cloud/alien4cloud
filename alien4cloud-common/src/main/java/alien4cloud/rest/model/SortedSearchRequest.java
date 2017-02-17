package alien4cloud.rest.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Search request that also allow definition of the field to use for sorting data.
 */
@Getter
@Setter
public class SortedSearchRequest extends FilteredSearchRequest {
    private String sortField;
    private boolean desc;
}
