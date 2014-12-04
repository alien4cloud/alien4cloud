package alien4cloud.application;

import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.version.Version;

import com.google.common.collect.Maps;

@Service
public class ApplicationVersionService {

    private static final Version DEFAULT_VERSION_NAME = new Version("0.0.1-SNAPSHOT");

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Create a new version for an application based on an existing topology.
     *
     * @param applicationId The id of the application for which to create the version.
     * @param topologyId The id of the topology to clone for the version's topology.
     */
    public ApplicationVersion createApplicationVersion(String applicationId, String topologyId) {
        ApplicationVersion version = new ApplicationVersion();
        version.setId(UUID.randomUUID().toString());
        version.setApplicationId(applicationId);
        version.setVersion(DEFAULT_VERSION_NAME);
        version.setReleased(false);
        version.setLatest(true);
        version.setProperties(Maps.<String, String> newHashMap());

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

        version.setTopologyId(topology.getId());
        alienDAO.save(version);
        return version;
    }

    /**
     * Get all environments for a given application.
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

    /**
     * Get an application version from it's id and throw a {@link NotFoundException} in case no application version matches the requested id
     * 
     * @param applicationVersionId
     * @return The requested application version
     */
    public ApplicationVersion getOrFail(String applicationVersionId) {
        ApplicationVersion applicationVersion = alienDAO.findById(ApplicationVersion.class, applicationVersionId);
        if (applicationVersion == null) {
            throw new NotFoundException("Application version [" + applicationVersionId + "] cannot be found");
        }
        return applicationVersion;
    }
}
