package alien4cloud.model.orchestrators.locations;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.ObjectField;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alien4cloud.json.deserializer.NodeTemplateDeserializer;
import lombok.Getter;
import lombok.Setter;

/**
 * A Location resource template is a location resource that has been defined and can be matched against nodes templates in a topology.
 */
@Getter
@Setter
@ESObject
public class LocationResourceTemplate extends AbstractLocationResourceTemplate<NodeTemplate> {
    /** Node template that describe the location resource (it's type must be a type derived from one of the orchestrator LocationResourceDefinition types). */
    @ObjectField(enabled = false)
    @JsonDeserialize(using = NodeTemplateDeserializer.class)
    private NodeTemplate template;

    /** Flag to know if the location resource template has been automatically generated. */
    private boolean generated;
}