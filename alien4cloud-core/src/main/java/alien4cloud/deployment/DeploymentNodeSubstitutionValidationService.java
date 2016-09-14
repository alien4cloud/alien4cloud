package alien4cloud.deployment;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import org.alien4cloud.tosca.model.types.NodeType;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TopologyTask;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Perform validation of a topology before deployment.
 */
@Service
public class DeploymentNodeSubstitutionValidationService {

    @Inject
    private WorkflowsBuilderService workflowsBuilderService;
    @Inject
    private IDeploymentNodeSubstitutionService deploymentNodeSubstitutionService;

    /**
     * Perform validation of a deployment topology against substitutions.
     *
     * @param deploymentTopology The topology to check.
     * @return A List of tasks: Nodes that can be substituted but are not.
     */
    public List<AbstractTask> validateNodeSubstitutions(DeploymentTopology deploymentTopology) {

        TopologyContext topologyContext = workflowsBuilderService.buildTopologyContext(deploymentTopology);

        List<AbstractTask> tasks = Lists.newArrayList();
        Map<String, List<LocationResourceTemplate>> availableSubstitutions = deploymentNodeSubstitutionService.getAvailableSubstitutions(deploymentTopology);
        if (MapUtils.isNotEmpty(availableSubstitutions)) {
            Map<String, String> substitutions = deploymentTopology.getSubstitutedNodes();
            if (substitutions == null) {
                substitutions = Maps.newHashMap();
            }
            for (Entry<String, List<LocationResourceTemplate>> availableSubstitution : availableSubstitutions.entrySet()) {
                if (substitutions.get(availableSubstitution.getKey()) == null) {
                    addTask(availableSubstitution.getKey(), topologyContext, tasks);
                }
            }
        }
        return tasks;
    }

    private void addTask(String nodeTemplateName, TopologyContext topologyContext, List<AbstractTask> tasks) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeTemplateName);
        NodeType nodeType = topologyContext.findElement(NodeType.class, nodeTemplate.getType());
        TopologyTask task = new TopologyTask(nodeTemplateName, nodeType);
        task.setCode(TaskCode.NODE_NOT_SUBSTITUTED);
        tasks.add(task);
    }
}
