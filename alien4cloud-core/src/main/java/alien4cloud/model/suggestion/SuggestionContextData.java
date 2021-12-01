package alien4cloud.model.suggestion;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.annotation.ESObject;

/**
 * These data can be set or not (depending of the context).
 */
@ESObject
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class SuggestionContextData {

    /**
     * Set when the user is editing an application topology, setting a deployment input, setting matching resources.
     */
    private String applicationId;

    /**
     * Set when the user is editing a topology.
     */
    private String topologyId;

    /**
     * Set when the user is working on an environment (deployment matching, editing environment topology, setting inputs).
     */
    private String environmentId;

    /**
     * Set when a property being edited is related to a given node in a topology.
     */
    private String nodeId;

    /**
     * Set when a property being edited is related to a policy.
     */
    private String policyId;

    /**
     * Set when a property being edited is related to a resource (service configuration, resource configuration, matching).
     */
    private String resourceId;

    /**
     * Set when a property being edited is related to a capability of a node.
     */
    private String capabilityId;

    /**
     * Set when a property being edited is related to a relationship of a node.
     */
    private String relationshipId;

    /**
     * The property name or the full property path of the property that is being edited (in case of nested).
     */
    private String propertyName;

}
