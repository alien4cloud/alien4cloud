package alien4cloud.paas.wf;

import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.utils.IToscaTypeFinder;

public interface TopologyContext extends IToscaTypeFinder {

    String getDSLVersion();

    Topology getTopology();
}
