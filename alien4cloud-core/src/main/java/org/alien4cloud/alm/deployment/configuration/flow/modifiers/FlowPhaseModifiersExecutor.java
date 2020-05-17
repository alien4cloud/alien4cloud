package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.model.orchestrators.locations.Location;
import lombok.extern.slf4j.Slf4j;

/**
 * This topology modifiers execute topology modifiers injected in a given phase
 * of it's process.
 */
@Slf4j
public class FlowPhaseModifiersExecutor implements ITopologyModifier {
    private final String phase;

    public FlowPhaseModifiersExecutor(String phase) {
        this.phase = phase;
    }

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        // First process generic phase
        processPhase(topology, context, phase);
        if (!context.log().isValid()) {
            // In case of errors we don't process the flow further.
            return;
        }

        Map<String, Location> selectedLocations = safe((Map<String, Location>) context
                .getExecutionCache()
                    .get(FlowExecutionContext.DEPLOYMENT_LOCATIONS_MAP_CACHE_KEY));

        for (Location location: selectedLocations.values().stream().distinct().collect(Collectors.toSet())){
            try {
                context.getExecutionCache().put(FlowExecutionContext.ORIGIN_LOCATION_FOR_MODIFIER, location.getId());
                processPhase(topology, context, phase + "-" + location.getId());
                if (!context.log().isValid()) {
                    // In case of errors we don't process the flow further.
                    return;
                }
            } finally{
                context.getExecutionCache().remove(FlowExecutionContext.ORIGIN_LOCATION_FOR_MODIFIER);
            }
        }
    }

    private void processPhase(Topology topology, FlowExecutionContext context, String phaseName) {
        List<ITopologyModifier> phaseModifiers = (List<ITopologyModifier>) context.getExecutionCache().get(phaseName);
        if (phaseModifiers == null || phaseModifiers.isEmpty()) {
            log.debug("No topology modifiers found for phase {}", phaseName);
            return;
        }

        long start = System.currentTimeMillis();
        log.debug("Starting phase {} with {} modifiers to execute.", phaseName, phaseModifiers.size());
        for (ITopologyModifier modifier : phaseModifiers) {
            modifier.process(context.getTopology(), context);
            if (!context.log().isValid()) {
                // In case of errors we don't process the flow further.
                return;
            }
        }
        log.debug("Phase {} completed in {} ms.", phaseName, System.currentTimeMillis() - start);

    }
}