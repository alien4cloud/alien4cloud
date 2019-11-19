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
      // The whole topology is saved in the execution cache for later use
      context.getExecutionCache().put(FlowExecutionContext.INITIAL_TOPOLOGY, CloneUtil.clone(topology));
    }
}
