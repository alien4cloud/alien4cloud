package alien4cloud.dao.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A facet informations.
 * 
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@ToString
public class FacetedSearchFacet implements Serializable {
    private static final long serialVersionUID = 1L;
    private String facetValue;
    private long count;
}