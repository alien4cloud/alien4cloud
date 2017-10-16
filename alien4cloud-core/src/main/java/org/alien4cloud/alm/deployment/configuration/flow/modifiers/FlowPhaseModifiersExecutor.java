package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import java.util.List;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.tosca.model.templates.Topology;

import lombok.extern.slf4j.Slf4j;

/**
 * This topology modifiers execute topology modifiers injected in a given phase of it's process.
 */
@Slf4j
public class FlowPhaseModifiersExecutor implements ITopologyModifier {
    private final String phase;

    public FlowPhaseModifiersExecutor(String phase) {
        this.phase = phase;
    }

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        List<ITopologyModifier> phaseModifiers = (List<ITopologyModifier>) context.getExecutionCache().get(phase);
        if (phaseModifiers == null || phaseModifiers.isEmpty()) {
            log.debug("No topology modifiers found for phase {}", phase);
            return;
        }

        log.debug("Starting phase {} with {} modifiers to execute.", phase, phaseModifiers.size());
        for (ITopologyModifier modifier : phaseModifiers) {
            modifier.process(context.getTopology(), context);
            if (!context.log().isValid()) {
                // In case of errors we don't process the flow further.
                return;
            }
        }
        log.debug("Phase {} completed.", phase);
    }
}