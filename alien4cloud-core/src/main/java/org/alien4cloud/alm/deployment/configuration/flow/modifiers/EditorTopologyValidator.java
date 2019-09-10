package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import static alien4cloud.utils.AlienUtils.safe;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.topology.TopologyValidationService;
import alien4cloud.topology.task.AbstractTask;

/**
 * This topology modifier doesn't actually impact the topology but performs a validation of a topology as out of the editor (abstract nodes are authorized etc.,
 * inputs does not have to be provided etc.).
 */
@Component
public class EditorTopologyValidator implements ITopologyModifier {
    @Inject
    private TopologyValidationService validationService;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        TopologyValidationResult validationResult = validationService.validateTopology(topology);
        for (AbstractTask warning : safe(validationResult.getWarningList())) {
            context.log().warn(warning);
        }
        for (AbstractTask error : safe(validationResult.getTaskList())) {
            context.log().error(error);
        }
        for (AbstractTask info : safe(validationResult.getInfoList())) {
            context.log().info(info);
        }
    }
}