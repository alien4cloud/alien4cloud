package alien4cloud.service;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentRuntimeStateService;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.service.exceptions.MissingSubstitutionException;
import alien4cloud.topology.TopologyServiceCore;

/**
 * This service handles the service resources managed by alien4cloud through deployments.
 */
@Service
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
     * @param fromRuntime If we should try to create the servic frm the runtime topology related to the environment.
     * @return the id of the created service
     *
     * @throws AlreadyExistException if a service with the given name, or related to the given environment already exists
     * @throws alien4cloud.exception.NotFoundException if <b>fromRuntime</b> is set to true, but the environment is not deployed
     * @throws MissingSubstitutionException if topology related to the environment doesn't define a substitution type
     *
     */
    public synchronized String create(String serviceName, String environmentId, boolean fromRuntime) {
        ApplicationEnvironment environment = checkAndGetApplicationEnvironment(environmentId);

        // check that the service does not exists already for this environment/topologyVersion couple
        if (alienDAO.buildQuery(ServiceResource.class).setFilters(fromKeyValueCouples("environmentId", environmentId)).count() > 0) {
            throw new AlreadyExistException(
                    "A service resource for environment <" + environmentId + "> and version <" + environment.getTopologyVersion() + "> already exists.");
        }

        Topology topology = getTopology(environment, fromRuntime);

        if (topology.getSubstitutionMapping() == null) {
            throw new MissingSubstitutionException("Substitution is required to expose a topology.");
        }

        // TODO shouldn't we set the status of the created service to started if created from a deployed topology?
        // The elementId of the type created out of the substitution is currently the archive name.
        return serviceResourceService.create(serviceName, environment.getTopologyVersion(), topology.getArchiveName(), environment.getTopologyVersion(),
                environmentId);
    }

    private Topology getTopology(ApplicationEnvironment environment, boolean runtimeTopology) {
        if (runtimeTopology) {
            // let's get the deployed topology
            return deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(environment.getId());
        } else {
            // Else let's get the topology currently associated with the environment (next deployment target)
            return topologyServiceCore.getOrFail(Csar.createId(environment.getApplicationId(), environment.getTopologyVersion()));
        }
    }

    /**
     * Get the service resource associated with an environment.
     * 
     * @param environmentId The environment for which to get the service resource.
     * @return A service resource instance if there is one associated with the environment or null if not.
     */
    public ServiceResource get(String environmentId) {
        checkAndGetApplicationEnvironment(environmentId);
        return alienDAO.buildQuery(ServiceResource.class).setFilters(fromKeyValueCouples("environmentId", environmentId)).prepareSearch().find();
    }

    /**
     * Get application environment, checks for DEPLOYMENT_MANAGEMENT rights on it.
     *
     * @param environmentId
     * @return the environment if the current user has the proper rights on it
     * @throws java.nio.file.AccessDeniedException if the current user doesn't have proper rights on the requested environment
     */
    private ApplicationEnvironment checkAndGetApplicationEnvironment(String environmentId) {
        ApplicationEnvironment environment = environmentService.getOrFail(environmentId);
        Application application = applicationService.getOrFail(environment.getApplicationId());
        // Only a user with deployment rôle on the environment can create an associated service.
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);
        return environment;
    }

    private ServiceResource getOrFail(String environmentId) {
        ServiceResource serviceResource = get(environmentId);
        if (serviceResource == null) {
            throw new NotFoundException("Service linked to the environement [" + environmentId + "] not found.");
        }

        return serviceResource;
    }

    /**
     * Unbind the service resource from the application environment
     *
     * Note that the service will still exists, but will only be updatable via service api
     * 
     * @param environmentId The environment for which to get the service resource.
     */
    public void unbind(String environmentId) {
        ServiceResource serviceResource = getOrFail(environmentId);
        serviceResource.setEnvironmentId(null);
        serviceResourceService.save(serviceResource, false);
    }
}
