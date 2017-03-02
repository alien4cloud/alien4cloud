package alien4cloud.service;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.deployment.DeploymentRuntimeStateService;
import alien4cloud.model.deployment.Deployment;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.service.exceptions.MissingSubstitutionException;
import alien4cloud.topology.TopologyServiceCore;

/**
 * This service handles the service resources managed by alien4cloud through deployments.
 */
public class ManagedServiceResourceService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService environmentService;
    @Inject
    private ServiceResourceService serviceResourceService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;

    /**
     * Create a Service resource associated with the given environment.
     * 
     * @param environmentId The environment to create a service for, the service version will be the one of the environment current associated version.
     * @param serviceName The name of the service as it should appears.
     * @return the id of the created service
     */
    public synchronized String create(String environmentId, String serviceName) {
        ApplicationEnvironment environment = environmentService.getOrFail(environmentId);
        Application application = applicationService.getOrFail(environment.getApplicationId());
        // Only a user with deployment rôle on the environment can create an associated service.
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);

        // check that the service does not exists already for this environment/topologyVersion couple
        if (alienDAO.buildQuery(ServiceResource.class).setFilters(fromKeyValueCouples("environmentId", environmentId)).count() > 0) {
            throw new AlreadyExistException(
                    "A service resource for environment <" + environmentId + "> and version <" + environment.getTopologyVersion() + "> already exists.");
        }

        Topology topology;
        Deployment deployment = environmentService.getActiveDeployment(environmentId);
        if (deployment == null) {
            // If the environment is not deployed let's create the service from the topology currently associated with the environment (next deployment target)
            topology = topologyServiceCore.getOrFail(Csar.createId(environment.getApplicationId(), environment.getTopologyVersion()));
        } else {
            // Else let's create the environment from the deployed topology
            topology = deploymentRuntimeStateService.getRuntimeTopology(deployment.getId());
        }

        if (topology.getSubstitutionMapping() == null) {
            throw new MissingSubstitutionException("Substitution is required to expose a topology.");
        }

        // The elementId of the type created out of the substitution is currently the archive name.
        return serviceResourceService.create(serviceName, environment.getTopologyVersion(), topology.getArchiveName(), environment.getTopologyVersion(),
                environmentId);
    }

    /**
     * Get the service resource associated with an environment.
     * 
     * @param environmentId The environment for which to get the service resource.
     * @return A service resource instance if there is one associated with the environment or null if not.
     */
    public ServiceResource get(String environmentId) {
        return alienDAO.buildQuery(ServiceResource.class).setFilters(fromKeyValueCouples("environmentId", environmentId)).prepareSearch().find();
    }
}
