package alien4cloud.deployment;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.common.MetaPropertiesService;
import alien4cloud.common.TagService;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TopologyTask;
import alien4cloud.topology.validation.LocationPolicyValidationService;
import alien4cloud.topology.validation.TopologyAbstractNodeValidationService;
import alien4cloud.topology.validation.TopologyPropertiesValidationService;
import alien4cloud.utils.services.ConstraintPropertyService;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Perform validation of a topology before deployment.
 */
@Service
public class DeploymentNodeSubstitutionValidationService {

    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private MetaPropertiesService metaPropertiesService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private TagService tagService;
    @Resource
    private ConstraintPropertyService constraintPropertyService;
    @Resource
    private TopologyPropertiesValidationService topologyPropertiesValidationService;
    @Resource
    private TopologyAbstractNodeValidationService topologyAbstractNodeValidationService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;
    @Inject
    private LocationPolicyValidationService locationPolicyValidationService;
    @Inject
    private OrchestratorPropertiesValidationService orchestratorPropertiesValidationService;

    /**
     * Perform validation of a deployment topology against substitutions.
     *
     * @param deploymentTopology The topology to check.
     * @return A List of tasks: Nodes that can be substituted but are not.
     */
    public List<AbstractTask> validateNodeSubstitutions(DeploymentTopology deploymentTopology) {
        List<AbstractTask> tasks = Lists.newArrayList();
        Map<String, Set<String>> availableSubtitutions = deploymentTopology.getAvailableSubstitutions();
        if (MapUtils.isNotEmpty(availableSubtitutions)) {
            Map<String, LocationResourceTemplate> substitutions = deploymentTopology.getSubstitutedNodes();
            if (substitutions == null) {
                substitutions = Maps.newHashMap();
            }
            for (Entry<String, Set<String>> availableSubstitution : availableSubtitutions.entrySet()) {
                if (substitutions.get(availableSubstitution.getKey()) == null) {
                    addTask(availableSubstitution.getKey(), tasks);
                }
            }
        }
        return tasks;
    }

    private void addTask(String nodeTemplateName, List<AbstractTask> tasks) {
        TopologyTask task = new TopologyTask(nodeTemplateName, null);
        task.setCode(TaskCode.NODE_NOT_SUBSTITUTED);
        tasks.add(task);
    }
}
