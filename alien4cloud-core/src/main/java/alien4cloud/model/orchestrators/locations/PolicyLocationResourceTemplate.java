package alien4cloud.model.orchestrators.locations;

import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.ObjectField;

import lombok.Getter;
import lombok.Setter;

/**
 * A Location policy template is a location policy that has been defined and can be matched against policies added in a topology.
 */
@ESObject
@Getter
@Setter
public class PolicyLocationResourceTemplate extends AbstractLocationResourceTemplate<PolicyTemplate> {

    /**
     * If true, just consider this as a template, properties defined for location resource don't have the priority.
     */
    private boolean onlyTemplate;

    /** Policy template that describe the location policy (it's type must be a type derived from one of the orchestrator LocationPolicyDefinition types). */
    @ObjectField(enabled = false)
    private PolicyTemplate template;
}
