package alien4cloud.model.deployment.matching;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.PropertyConstraint;

/**
 * Matching configuration is associated to types provided by a plugin so alien knows how things should be matched.
 *
 * Alien ships with default implementations of matcher configurations for TOSCA normative types but this can be extended by the locations providers.
 */
@Getter
@Setter
public class MatchingConfiguration {
    /** Type to be matched */
    private String nodeType;
    /** Version of the node type's archive. */
    private String archiveVersion;
    /** Name of the node type's archive. */
    private String archiveName;
    /** Matching ordering is used to sort templates in order to find best matches. */
    private List<String> matchingOrdering;
    /** Key is the path of the element to check. Value is the constraint to apply. */
    private Map<String, List<PropertyConstraint>> propsConstraints;
    /** Constraints to be applied to properties capabilities to match the type. */
    private Map<String, Map<String, List<PropertyConstraint>>> capabilitiesPropsConstraints;
}