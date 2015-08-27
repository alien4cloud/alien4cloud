package alien4cloud.model.orchestrators.locations;

import alien4cloud.model.topology.NodeTemplate;
import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;
import org.hibernate.validator.constraints.NotBlank;

import alien4cloud.model.components.IndexedNodeType;

/**
 * A Location resource template is a location resource that has been defined and can be matched against nodes in a topology.
 *
 * A location resource template is defined with a node template that specifies values
 */
@Getter
@Setter
@ESObject
public class LocationResourceTemplate {
    @Id
    private String id;
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String locationId;
    /** Flag to know if the administrator allow this resource to be matched. */
    private boolean enabled;
    /** Flag to know if the node has been automatically generated. */
    private boolean generated;
    /** Flag to know if the resource template defines a service or a resource. In case of a resource it's type must be one of the orchestrator resource types. */
    private boolean isService;
    /** Node template that describe the location resource (it's type must be a type derived from one of the orchestrator LocationResourceDefinition types). */
    private NodeTemplate template;
}