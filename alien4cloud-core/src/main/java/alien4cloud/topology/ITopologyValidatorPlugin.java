package alien4cloud.topology;

import org.alien4cloud.tosca.model.templates.Topology;

import java.util.List;

public interface ITopologyValidatorPlugin {

    public void validate(Topology topology,ITopologyValidatorPluginLogger logger);
}
