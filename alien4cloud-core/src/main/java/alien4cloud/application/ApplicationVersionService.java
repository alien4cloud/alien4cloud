package alien4cloud.application;

import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.ApplicationVersionException;

import com.google.common.collect.Maps;

@Service
public class ApplicationVersionService {
    private static final String DEFAULT_VERSION_NAME = "0.1.0-SNAPSHOT";

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Create a new version for an application based on an existing topology with the default version name.
     *
     * @param applicationId The id of the application for which to create the version.
     * @param topologyId The id of the topology to clone for the version's topology.
     */
    public ApplicationVersion createApplicationVersion(String applicationId, String topologyId) {
        return createApplicationVersion(applicationId, topologyId, DEFAULT_VERSION_NAME);
    }

    /**
     * Create a new version for an application based on an existing topology.
     *
     * @param applicationId The id of the application for which to create the version.
     * @param topologyId The id of the topology to clone for the version's topology.
     * @param version The number version of the new application version.
     */
    public ApplicationVersion createApplicationVersion(String applicationId, String topologyId, String version) {
        if (isApplicationVersionNameExist(applicationId, version)) {
            throw new AlreadyExistException("An application version already exist for this application with the version :" + version);
        }

        VersionUtil.parseVersion(version);
        ApplicationVersion appVersion = new ApplicationVersion();
        appVersion.setId(UUID.randomUUID().toString());
        appVersion.setApplicationId(applicationId);
        appVersion.setVersion(version);
        appVersion.setReleased(false);
        appVersion.setLatest(true);
        appVersion.setProperties(Maps.<String, String> newHashMap());

        Topology topology;
        if (topologyId != null) { // "cloning" the topology
            topology = alienDAO.findById(Topology.class, topologyId);
        } else {
            topology = new Topology();
        }
        topology.setId(UUID.randomUUID().toString());
        topology.setDelegateId(applicationId);
        topology.setDelegateType(Application.class.getSimpleName().toLowerCase());
        alienDAO.save(topology);

        appVersion.setTopologyId(topology.getId());
        alienDAO.save(appVersion);
        return appVersion;
    }

    /**
     * Get all application version for a given application.
     *
     * @param applicationId The id of the application for which to get environments.
     * @return An array of the environments for the requested application id.
     */
    public ApplicationVersion[] getByApplicationId(String applicationId) {
        GetMultipleDataResult<ApplicationVersion> result = alienDAO.find(ApplicationVersion.class,
                MapUtil.newHashMap(new String[] { "applicationId" }, new String[][] { new String[] { applicationId } }), Integer.MAX_VALUE);
        return result.getData();
    }

    private void deleteVersion(ApplicationVersion version) {
        if (version != null) {
            alienDAO.delete(Topology.class, version.getTopologyId());
            alienDAO.delete(ApplicationVersion.class, version.getId());
        }
    }

    /**
     * Delete a version and the related topologies.
     *
     * @param id The id of the version to delete.
     */
    public void delete(String id) {
        ApplicationVersion version = alienDAO.findById(ApplicationVersion.class, id);
        if (version == null) {
            throw new ApplicationVersionException("No application version find with the id :" + id);
        }
        deleteVersion(version);
    }

    /**
     * Delete all versions related to an application.
     *
     * @param applicationId The application id.
     */
    public void deleteByApplication(String applicationId) {
        ApplicationVersion[] versions = getByApplicationId(applicationId);
        for (ApplicationVersion version : versions) {
            deleteVersion(version);
        }
    }

    public boolean isApplicationVersionDeployed(String applicationVersionId) {
        GetMultipleDataResult<Deployment> dataResult = alienDAO.search(
                Deployment.class,
                null,
                MapUtil.newHashMap(new String[] { "deploymentSetup.versionId", "endDate" }, new String[][] { new String[] { applicationVersionId },
                        new String[] { null } }), 1);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            return true;
        }
        return false;
    }

    public boolean isApplicationVersionNameExist(String applicationId, String applicationVersionName) {
        GetMultipleDataResult<ApplicationVersion> dataResult = alienDAO.search(
                ApplicationVersion.class,
                null,
                MapUtil.newHashMap(new String[] { "applicationId", "version" }, new String[][] { new String[] { applicationId },
                        new String[] { applicationVersionName } }), 1);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            return true;
        }
        return false;
    }
}
