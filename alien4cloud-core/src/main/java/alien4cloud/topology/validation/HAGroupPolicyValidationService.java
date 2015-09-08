package alien4cloud.topology.validation;

import alien4cloud.model.deployment.DeploymentSetup;
import alien4cloud.model.cloud.AvailabilityZone;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.exception.AvailabilityZoneConfigurationException;
import alien4cloud.paas.ha.AllocationError;
import alien4cloud.paas.ha.AllocationErrorCode;
import alien4cloud.paas.ha.AvailabilityZoneAllocator;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.paas.plan.TopologyTreeBuilderService;
import alien4cloud.topology.task.HAGroupTask;
import alien4cloud.topology.task.TopologyTask;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Performs validation that Groups for HA Policy can be full filled based on zone configurations.
 */
@Slf4j
@Component
public class HAGroupPolicyValidationService {
    @Resource
    private TopologyTreeBuilderService topologyTreeBuilderService;

    /**
     * Performs validation that Groups for HA Policy can be full filled based on zone configurations.
     * 
     * @param topology The topology to validate.
     * @param deploymentSetup The deployment setup that contains the parameters.
     * @param matcherConfig The matching that has associated node templates to zones based on the H.A. policy management.
     * @return A list of tasks to be performed to make this topology valid for deployment.
     */
    public List<TopologyTask> validateHAGroup(Topology topology, DeploymentSetup deploymentSetup, CloudResourceMatcherConfig matcherConfig) {
        AvailabilityZoneAllocator allocator = new AvailabilityZoneAllocator();
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        List<TopologyTask> tasks = Lists.newArrayList();
        List<AllocationError> allocationErrors;
        try {
            Map<String, AvailabilityZone> allocatedZones = allocator.processAllocation(paaSTopology, deploymentSetup, matcherConfig);
            allocationErrors = allocator.validateAllocation(allocatedZones, paaSTopology, deploymentSetup, matcherConfig);
        } catch (AvailabilityZoneConfigurationException e) {
            log.warn("Unable to validate zones allocation due to bad configuration", e);
            tasks.add(new HAGroupTask(null, e.getGroupId(), AllocationErrorCode.CONFIGURATION_ERROR));
            return tasks;
        } catch (Exception e) {
            log.error("Unable to validate zones allocation due to unknown error", e);
            tasks.add(new HAGroupTask(null, null, AllocationErrorCode.UNKNOWN_ERROR));
            return tasks;
        }
        for (AllocationError error : allocationErrors) {
            String nodeId = error.getNodeId();
            tasks.add(new HAGroupTask(nodeId, error.getGroupId(), error.getCode()));
        }
        return tasks;
    }
}
