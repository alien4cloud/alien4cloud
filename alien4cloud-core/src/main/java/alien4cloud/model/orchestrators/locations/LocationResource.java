package alien4cloud.model.orchestrators.locations;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;

import alien4cloud.model.components.IndexedNodeType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModel;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
@ESObject
@ApiModel(value = "LocationResource", description = "A LocationResource ")
public class LocationResource {
    private String id;
    private String locationId;
    private LocationResourceType locationResourceType;
    /** The node type that represents the resource. */
    private IndexedNodeType type;
}