package alien4cloud.deployment;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.application.TopologyCompositionService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.deployment.DeploymentSetup;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.services.ConstraintPropertyService;

import com.google.common.collect.Maps;

/**
 * Manages deployment setups.
 */
@Slf4j
@Service
public class DeploymentSetupService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private ConstraintPropertyService constraintPropertyService;
    @Resource
    private InputsPreProcessorService inputsPreProcessorService;
    @Resource
    private TopologyCompositionService topologyCompositionService;

    public DeploymentSetup get(ApplicationVersion version, ApplicationEnvironment environment) {
        return getById(generateId(version.getId(), environment.getId()));
    }

    private DeploymentSetup getById(String deploymentSetupId) {
        return alienDAO.findById(DeploymentSetup.class, deploymentSetupId);
    }

    public DeploymentSetup getOrFail(ApplicationVersion version, ApplicationEnvironment environment) {
        DeploymentSetup setup = get(version, environment);
        if (setup == null) {
            throw new NotFoundException("No setup found for version [" + version.getId() + "] and environment [" + environment.getId() + "]");
        } else {
            return setup;
        }
    }

    public DeploymentSetup getOrFail(ApplicationEnvironment environment) {
        DeploymentSetup setup = getById(generateId(environment.getCurrentVersionId(), environment.getId()));
        if (setup == null) {
            throw new NotFoundException("No setup found for version [" + environment.getCurrentVersionId() + "] and environment [" + environment.getId() + "]");
        } else {
            return setup;
        }
    }

    /**
     * Create a deployment setup with a failure when the composed key (version.id, environment.id) already exists
     *
     * @param version
     * @param environment
     * @return the created deployment setup
     */
    public DeploymentSetup createOrFail(ApplicationVersion version, ApplicationEnvironment environment) {
        // check if deploymentSetup already exists
        DeploymentSetup deploymentSetup = get(version, environment);
        if (deploymentSetup == null) {
            deploymentSetup = new DeploymentSetup();
            deploymentSetup.setId(generateId(version.getId(), environment.getId()));
            deploymentSetup.setEnvironmentId(environment.getId());
            deploymentSetup.setVersionId(version.getId());
            alienDAO.save(deploymentSetup);
        } else {
            throw new AlreadyExistException("A deployment setup for application <" + environment.getApplicationId() + "> for version [" + version.getId()
                    + "] / environment [" + environment.getId() + "] already exists");
        }
        return deploymentSetup;
    }

    @AllArgsConstructor
    private static class MappingGenerationResult<T> {
        private Map<String, T> mapping;
        private boolean valid;
        private boolean changed;
    }

    /**
     * Generate the id of a deployment setup.
     *
     * @param versionId The id of the version of the deployment setup.
     * @param environmentId The id of the environment of the deployment setup.
     * @return The generated id.
     */
    private String generateId(String versionId, String environmentId) {
        return versionId + "::" + environmentId;
    }

    /**
     * Delete a deployment setup based on the id of the related environment.
     *
     * @param environmentId The id of the environment
     */
    public void deleteByEnvironmentId(String environmentId) {
        alienDAO.delete(DeploymentSetup.class, QueryBuilders.termQuery("environmentId", environmentId));
    }

    /**
     * Delete a deployment setup based on the id of the related version.
     *
     * @param versionId The id of the version
     */
    public void deleteByVersionId(String versionId) {
        alienDAO.delete(DeploymentSetup.class, QueryBuilders.termQuery("versionId", versionId));
    }

    /**
     * Get all deployments setup based on the id of the related version.
     *
     * @param versionId The id of the version
     */
    public GetMultipleDataResult<DeploymentSetup> getByVersionId(String versionId) {
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("versionId", new String[] { versionId });
        return alienDAO.search(DeploymentSetup.class, null, filters, 0, 20);
    }

    /**
     * Get a topology Id for a deploymentSetup
     *
     * @param deploymentSetupId
     * @return a topology id
     */
    public String getTopologyId(String deploymentSetupId) {
        DeploymentSetup deploymentSetup = getById(deploymentSetupId);
        if (deploymentSetup != null) {
            ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(deploymentSetup.getEnvironmentId());
            ApplicationVersion applicationVersion = applicationVersionService.getOrFail(applicationEnvironment.getCurrentVersionId());
            return applicationVersion.getTopologyId();
        }
        return null;
    }

    /**
     * Get the linked application environment
     *
     * @param deploymentSetupId
     * @return an application environment
     */
    public ApplicationEnvironment getApplicationEnvironment(String deploymentSetupId) {
        DeploymentSetup deploymentSetup = getById(deploymentSetupId);
        if (deploymentSetup != null) {
            ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(deploymentSetup.getEnvironmentId());
            return applicationEnvironment;
        }
        return null;
    }

    /**
     * Get the linked application version
     *
     * @param deploymentSetupId
     * @return an application version
     */
    public ApplicationVersion getApplicationVersion(String deploymentSetupId) {
        DeploymentSetup deploymentSetup = getById(deploymentSetupId);
        if (deploymentSetup != null) {
            ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(deploymentSetup.getEnvironmentId());
            ApplicationVersion applicationVersion = applicationVersionService.getOrFail(applicationEnvironment.getCurrentVersionId());
            return applicationVersion;
        }
        return null;
    }

    /**
     * Get all deployment setup linked to a topology
     *
     * @param topologyId the topology id
     * @return all deployment setup that is linked to this topology
     */
    public DeploymentSetup[] getByTopologyId(String topologyId) {
        List<DeploymentSetup> deploymentSetups = Lists.newArrayList();
        ApplicationVersion version = applicationVersionService.getByTopologyId(topologyId);
        if (version != null) {
            ApplicationEnvironment[] environments = applicationEnvironmentService.getByVersionId(version.getId());
            if (environments != null && environments.length > 0) {
                for (ApplicationEnvironment environment : environments) {
                    deploymentSetups.add(get(version, environment));
                }
            }
        }
        return deploymentSetups.toArray(new DeploymentSetup[deploymentSetups.size()]);
    }
}