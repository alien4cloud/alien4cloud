package alien4cloud.deployment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentSetup;
import alien4cloud.model.deployment.DeploymentSourceType;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.EmptyMetaPropertyException;
import alien4cloud.paas.exception.OrchestratorDeploymentIdConflictException;

import com.google.common.collect.Maps;

/**
 * Service responsible for deployment of an application.
 */
@Slf4j
@Service
public class DeployService {
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Inject
    private LocationService locationService;
    @Inject
    private OrchestratorService orchestratorService;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private DeploymentContextService deploymentContextService;

    /**
     * Deploy a topology and return the deployment ID.
     *
     * @param deploymentTopology Location aware and matched topology.
     * @param deploymentSetup The values for the required inputs of the deploymentTopology as well as orchestrator specific properties.
     * @param deploymentSource Application to be deployed or the Csar that contains test toplogy to be deployed
     * @return The id of the generated deployment.
     */
    public String deploy(DeploymentTopology deploymentTopology, DeploymentSetup deploymentSetup, IDeploymentSource deploymentSource) {
        String locationId = TopologyLocationUtils.getLocationIdOrFail(deploymentTopology);
        Location location = locationService.getOrFail(locationId);
        // FIXME check that all nodes to match are matched

        // FIXME Merge the deployment topology with the initial topology ?

        // FIXME Inject all inputs and perform required pre-processing

        // FIXME check that all required properties are defined
        // TODO DeploymentSetupValidator.validate doesn't check that inputs linked to required properties are indeed configured.

        // Get the orchestrator that will perform the deployment
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.get(location.getOrchestratorId());

        String deploymentTopologyId = deploymentTopology.getId();

        // Create a deployment object to be kept in ES.
        Deployment deployment = new Deployment();
        deployment.setId(UUID.randomUUID().toString());
        deployment.setOrchestratorId(location.getOrchestratorId());
        deployment.setLocationIds(new String[] { location.getId() });
        deployment.setOrchestratorDeploymentId(generateOrchestratorDeploymentId(deploymentSetup.getEnvironmentId(), location.getOrchestratorId()));
        deployment.setSourceId(deploymentSource.getId());
        String sourceName;
        if (deploymentSource.getName() == null) {
            sourceName = UUID.randomUUID().toString();
        } else {
            sourceName = deploymentSource.getName();
        }
        deployment.setSourceName(sourceName);
        deployment.setSourceType(DeploymentSourceType.fromSourceType(deploymentSource.getClass()));
        deployment.setStartDate(new Date());
        deployment.setDeploymentSetup(deploymentSetup);
        // mandatory for the moment since we could have deployment with no environment (csar test)
        deployment.setTopologyId(deploymentTopology.getInitialTopologyId());
        alienDao.save(deployment);

        // save the topology as a deployed topology.
        // change the Id before saving
        deploymentTopology.setId(deployment.getId());
        alienMonitorDao.save(deploymentTopology);
        // put back the old Id for deployment
        deploymentTopology.setId(deploymentTopologyId);
        // Build the context for deployment and deploy
        orchestratorPlugin.deploy(deploymentContextService.buildTopologyDeploymentContext(deployment, deploymentTopology), null);
        log.info("Deployed topology [{}] on location [{}], generated deployment with id [{}]", deploymentTopology.getInitialTopologyId(), location.getId(),
                deployment.getId());
        return deployment.getId();
    }

    /**
     * Generate the human readable deployment id for the orchestrator.
     *
     * @param envId Id of the deployed environment.
     * @param orchestratorId Id of the orchestrator on which the deployment is performed.
     * @return The orchestrator deployment id.
     * @throws alien4cloud.paas.exception.OrchestratorDeploymentIdConflictException
     */
    private String generateOrchestratorDeploymentId(String envId, String orchestratorId) throws OrchestratorDeploymentIdConflictException {
        log.debug("Generating deployment paaS Id...");
        log.debug("All spaces will be replaced by an \"_\" charactr. You might consider it while naming your applications.");
        ApplicationEnvironment env = applicationEnvironmentService.getOrFail(envId);
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        String namePattern = orchestrator.getDeploymentNamePattern();
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(namePattern);
        String orchestratorDeploymentId = (String) exp.getValue(new OrchestratorIdContext(env, applicationService.getOrFail(env.getApplicationId()),
                namePattern.contains("metaProperties[")));
        orchestratorDeploymentId = orchestratorDeploymentId.trim().replaceAll(" ", "_");

        // ensure that the id is not used by another deployment.
        if (deploymentService.isActiveDeployment(orchestratorId, orchestratorDeploymentId)) {
            throw new OrchestratorDeploymentIdConflictException("Conflict detected with the generated paasId <" + orchestratorDeploymentId + ">.");
        }

        return orchestratorDeploymentId;
    }

    // Inner class used to build context for generation of the orchestrator id.
    @Getter
    class OrchestratorIdContext {
        private ApplicationEnvironment environment;
        private Application application;
        private Map<String, String> metaProperties;
        private String time;

        private Map<String, String> constructMapOfMetaProperties(final Application app) {
            Map<String, String[]> filters = new HashMap<String, String[]>();
            filters.put("target", new String[] { "application" }); // get all meta properties configuration for applications.
            FacetedSearchResult result = alienDao.facetedSearch(MetaPropConfiguration.class, null, filters, null, 0, 20);
            MetaPropConfiguration metaProp;
            Map<String, String> metaProperties = Maps.newHashMap();
            for (int i = 0; i < result.getData().length; i++) {
                metaProp = (MetaPropConfiguration) result.getData()[i];
                if (app.getMetaProperties().get(metaProp.getId()) != null) {
                    metaProperties.put(metaProp.getName(), app.getMetaProperties().get(metaProp.getId()));
                } else if (metaProp.getDefault() != null) {
                    metaProperties.put(metaProp.getName(), metaProp.getDefault());
                } else {
                    throw new EmptyMetaPropertyException("The meta property " + metaProp.getName() + " is null and don't have a default value.");
                }
            }
            return metaProperties;
        }

        public OrchestratorIdContext(ApplicationEnvironment env, Application app, boolean hasMetaProperties) {
            this.environment = env;
            this.application = app;
            SimpleDateFormat ft = new SimpleDateFormat("yyyyMMddHHmm");
            this.time = ft.format(new Date());
            if (hasMetaProperties) {
                this.metaProperties = constructMapOfMetaProperties(app);
            }
        }
    }
}