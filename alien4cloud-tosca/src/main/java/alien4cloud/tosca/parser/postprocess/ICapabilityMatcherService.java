package alien4cloud.tosca.parser.postprocess;

import java.util.Map;

import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;

public interface ICapabilityMatcherService {
    Map<String, Capability> getCompatibleCapabilityByType(NodeTemplate nodeTemplate, String type);
}
