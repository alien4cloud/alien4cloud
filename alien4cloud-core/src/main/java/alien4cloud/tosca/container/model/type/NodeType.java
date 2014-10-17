package alien4cloud.tosca.container.model.type;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.container.model.ToscaInheritableElement;
import alien4cloud.tosca.container.model.template.DeploymentArtifact;
import alien4cloud.ui.form.annotation.FormProperties;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * <p>
 * A Node Type is a reusable entity that defines the type of one or more Node Templates. As such, a Node Type defines the structure of observable properties via
 * a Properties Definition, i.e. the names, data types and allowed values the properties defined in Node Templates using a Node Type or instances of such Node
 * Templates can have.
 * </p>
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@FormProperties({ "id", "final", "derivedFrom", "description", "properties", "requirements", "capabilities", "instanceStates", "interfaces" })
public class NodeType extends ToscaInheritableElement {
    /**
     * This OPTIONAL element specifies the requirements that the Node Type exposes.
     */
    private Map<String, RequirementDefinition> requirements = Maps.newHashMap();
    /**
     * This OPTIONAL element specifies the capabilities that the Node Type exposes.
     */
    private Map<String, CapabilityDefinition> capabilities = Maps.newHashMap();
    /**
     * This OPTIONAL element lists the set of states an instance of this Node Type can occupy.
     */
    private List<String> instanceStates = Lists.newArrayList();
    /**
     * This element contains the definitions of the operations that can be performed on (instances of) this Node Type.
     */
    private Map<String, Interface> interfaces = Maps.newHashMap();

    /**
     * List of artifacts that are required for the node implementation.
     */
    private Map<String, DeploymentArtifact> artifacts;
}