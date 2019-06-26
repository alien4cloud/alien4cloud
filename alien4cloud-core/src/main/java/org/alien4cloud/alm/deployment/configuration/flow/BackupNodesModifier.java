package org.alien4cloud.alm.deployment.configuration.flow;

import alien4cloud.utils.CloneUtil;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BackupNodesModifier implements ITopologyModifier {

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
      // Backup the nodes before processing them
      topology.setUnprocessedNodeTemplates(CloneUtil.clone(topology.getNodeTemplates()));
    }
}
