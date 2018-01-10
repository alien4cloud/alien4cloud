package alien4cloud.tosca.parser.postprocess;

import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.NodeType;

public interface ICapabilityMatcherService {
    Map<String, Capability> getCompatibleCapabilityByType(NodeTemplate nodeTemplate, String type);

    List<CapabilityDefinition> getCompatibleCapabilityByType(NodeType nodeType, String type);
}
