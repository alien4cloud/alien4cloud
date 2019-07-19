package alien4cloud.rest.wizard.model;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.types.NodeType;

import java.util.List;

/**
 * A topology is composed of nodes. A node has a given type.
 * A module is a node of a topology that has some meanings for the user.
 */
@Getter
@Setter
public class ApplicationModule {

    /**
     * A short readable name for the module (without package).
     */
    private NodeType nodeType;

    /**
     * The meta properties related to the application. Key is the human readable name of the meta-property.
     */
    private List<MetaProperty> namedMetaProperties;

}
