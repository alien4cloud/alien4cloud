package alien4cloud.tosca.container.model.type;

import java.util.List;
import java.util.Map;

import alien4cloud.tosca.container.model.ToscaInheritableElement;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * A Policy Type is a reusable entity that describes a kind of non-functional behavior or a kind of quality-of-service (QoS) that a Node Type can declare to
 * expose. For example, a Policy Type can be defined to express high availability for specific Node Types (e.g. a Node Type for an application server).
 * </p>
 * <p>
 * A Policy Type defines the structure of observable properties via a Properties Definition, i.e. the names, data types and allowed values the properties
 * defined in a corresponding Policy Template can have.
 * </p>
 * <p>
 * A Policy Type can inherit properties from another Policy Type by means of the DerivedFrom element.
 * </p>
 * <p>
 * A Policy Type declares the set of Node Types it specifies non-functional behavior for via the AppliesTo element. Note that being “applicable to�? does not
 * enforce implementation: i.e. in case a Policy Type expressing high availability is associated with a “Webserver�? Node Type, an instance of the Webserver is
 * not necessarily highly available. Whether or not an instance of a Node Type to which a Policy Type is applicable will show the specified non-functional
 * behavior, is determined by a Node Template of the corresponding Node Type.
 * </p>
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class PolicyType extends ToscaInheritableElement {
    /**
     * This OPTIONAL attribute specifies the language used to specify the details of the Policy Type. These details can be defined as policy type specific
     * content of the PolicyType element.
     */
    private String policyLanguage;
    /**
     * This OPTIONAL element specifies the set of Node Types the Policy Type is applicable to.
     */
    private List<String> appliesTo;
    /**
     * Any policy specific content.
     */
    private Map<String, Object> specificContent;
}