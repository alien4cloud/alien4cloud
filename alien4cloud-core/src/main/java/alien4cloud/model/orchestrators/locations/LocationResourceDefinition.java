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
@ApiModel(value = "LocationResource", description = "A LocationResource ")
public class LocationResourceDefinition {
    /** The type of resources that is defined. */
    private LocationResourceType locationResourceType;
    /** The node type for this resource. */
    private String nodeType;
}