package alien4cloud.tosca.container.model.type;

import alien4cloud.tosca.container.model.ToscaInheritableElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * A Capability Type is a reusable entity that describes a kind of capability that a Node Type can declare to expose. For example, a Capability Type for a
 * database server endpoint can be defined and various Node Types (e.g. a Node Type for a database) can declare to expose (or to “provide�?) the capability of
 * serving as a database server endpoint.
 * </p>
 * <p>
 * A Capability Type defines the structure of observable properties via a Properties Definition, i.e. the names, data types and allowed values the properties
 * defined in Capabilities of Node Templates of a Node Type can have in cases where the Node Type defines a capability of the respective Capability Type.
 * </p>
 * 
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class CapabilityType extends ToscaInheritableElement {

    /**
     * <p>
     * Jackson DeSerialization workaround constructor to create an operation with no arguments.
     * </p>
     * 
     * @param emptyString
     *            The empty string provided by jackson.
     */
    public CapabilityType(String emptyString) {
        // do nothing as this is a Jackson workaround.
    }
}