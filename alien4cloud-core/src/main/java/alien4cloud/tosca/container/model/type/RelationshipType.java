package alien4cloud.tosca.container.model.type;

import java.util.List;
import java.util.Map;

import alien4cloud.tosca.container.model.ToscaInheritableElement;
import alien4cloud.tosca.container.model.template.DeploymentArtifact;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * A Relationship Type is a reusable entity that defines the type of one or more Relationship Templates between Node Templates. As such, a Relationship Type can
 * define the structure of observable properties via a Properties Definition, i.e. the names, data types and allowed values the properties defined in
 * Relationship Templates using a Relationship Type or instances of such Relationship Templates can have.
 * </p>
 * <p>
 * The operations that can be performed on (an instance of) a corresponding Relationship Template are defined by the Interfaces of the Relationship Type.
 * Furthermore, a Relationship Type defines the potential states an instance of it might reveal at runtime.
 * </p>
 * <p>
 * A Relationship Type can inherit the definitions defined in another Relationship Type by means of the DerivedFrom element. Relationship Types might be
 * declared as abstract, meaning that they cannot be instantiated. The purpose of such abstract Relationship Types is to provide common properties and behavior
 * for re-use in specialized, derived Relationship Types. Relationship Types might also be declared as final, meaning that they cannot be derived by other
 * Relationship Types
 * </p>
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class RelationshipType extends ToscaInheritableElement {
    /**
     * This OPTIONAL element lists the set of states an instance of this Relationship Type can occupy at runtime.
     */
    private List<String> instanceStates;
    /**
     * This OPTIONAL element contains definitions of manageability interfaces that can be performed on the source of a relationship of this Relationship Type to
     * actually establish the relationship between the source and the target in the deployed service.
     */
    private Map<String, Interface> interfaces;

    private String[] validSources;

    /**
     * <p>
     * This OPTIONAL element specifies the type of object that is allowed as a valid target for relationships defined using the Relationship Type under
     * definition. If not specified, any Node Type is allowed to be the origin of the relationship
     * </p>
     * <p>
     * If specified, the value must be the reference to a valid Node Type or Relationship Type:
     * <ul>
     * <li>If ValidTarget specifies a Node Type, the ValidSource element (if present) of the Relationship Type under definition MUST also specify a Node Type.</li>
     * <li>If ValidTarget specifies a Capability Type, the ValidSource element (if present) of the Relationship Type under definition MUST specify a Requirement
     * Type. This Requirement Type MUST declare it requires the capability defined in ValidTarget, i.e. it MUST declare the type (or a super-type of) the
     * capability in the requiredCapabilityType attribute of the respective RequirementType definition.</li>
     * </ul>
     * </p>
     */
    private String[] validTargets;

    /**
     * List of artifacts that are required for the relationship implementation.
     */
    private Map<String, DeploymentArtifact> artifacts;
}