package alien4cloud.deployment;

import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.runtime.Execution;
import alien4cloud.model.runtime.ExecutionInputs;
import com.google.common.collect.Maps;
import org.alien4cloud.secret.services.SecretProviderService;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.model.PaaSDeploymentContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages topology workflows.
 */
@Service
@Slf4j
public class WorkflowExecutionService {
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private SecretProviderService secretProviderService;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Launch a given workflow.
     */
    public synchronized void launchWorkflow(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials,
            String applicationEnvironmentId, String workflowName, Map<String, Object> params, IPaaSCallback<String> iPaaSCallback) {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        final DeploymentTopology topology = alienMonitorDao.findById(DeploymentTopology.class, deployment.getId());
        // get the secret provider configuration from the location
        Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(topology);
        Map<String, Location> locations = deploymentTopologyService.getLocations(locationIds);
        SecretProviderConfigurationAndCredentials authResponse = null;
        if (secretProviderService.isSecretProvided(secretProviderConfigurationAndCredentials)) {
            authResponse = secretProviderService.generateToken(locations,
                    secretProviderConfigurationAndCredentials.getSecretProviderConfiguration().getPluginName(),
                    secretProviderConfigurationAndCredentials.getCredentials());
        }
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deploymentTopology, authResponse);
        orchestratorPlugin.launchWorkflow(deploymentContext, workflowName, params, iPaaSCallback);
    }

    public void saveExecutionInputs(ExecutionInputs executionInputs) {
        alienDAO.save(executionInputs);
    }

    public Map<String, AbstractPropertyValue> getLastExecutionInputs(String applicationEnvironmentId, String workflowName) {
        QueryBuilder qb = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("environmentId",applicationEnvironmentId))
                .must(QueryBuilders.termQuery("workflowName",workflowName));

        FacetedSearchResult<ExecutionInputs> result = alienDAO.facetedSearch(ExecutionInputs.class, "", null, qb, null, 0, 1, "timestamp", "date", true);

        if (result.getData().length > 0) {
            return result.getData()[0].getInputs();
        } else {
            return Maps.newHashMap();
        }
    }
}
