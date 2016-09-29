package alien4cloud.application;

import alien4cloud.utils.VersionUtil;
import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.springframework.stereotype.Service;

import alien4cloud.common.AbtractVersionService;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.utils.MapUtil;

@Service
public class ApplicationVersionService extends AbtractVersionService<ApplicationVersion> {

    @Override
    protected ApplicationVersion buildVersionImplem() {
        return new ApplicationVersion();
    }

    @Override
    protected ApplicationVersion[] buildVersionImplemArray(int length) {
        return new ApplicationVersion[length];
    }

    @Override
    protected Class<ApplicationVersion> getVersionImplemClass() {
        return ApplicationVersion.class;
    }

    @Override
    protected ArchiveDelegateType getDelegateType() {
        return ArchiveDelegateType.APPLICATION;
    }

    @Override
    protected String getDelegatePropertyName() {
        return "applicationId";
    }

    /**
     * Create a new version for an application based on an existing topology with the default version name.
     *
     * @param applicationId The id of the application for which to create the version.
     * @param topologyId The id of the topology to clone for the version's topology.
     */
    public ApplicationVersion createApplicationVersion(String applicationId, String topologyId) {
        return createVersion(applicationId, topologyId, VersionUtil.DEFAULT_VERSION_NAME, null);
    }

    /**
     * Create a new version for an application based on an existing topology.
     *
     * @param applicationId The id of the application for which to create the version.
     * @param topologyId The id of the topology to clone for the version's topology.
     * @param version The number version of the new application version.
     */
    public ApplicationVersion createApplicationVersion(String applicationId, String topologyId, String version, String desc) {
        return createVersion(applicationId, topologyId, version, desc);
    }

    /**
     * Get all application versions for a given application
     *
     * @param applicationId The id of the application for which to get environments.
     * @return An array of the applications versions for the requested application id.
     */
    public ApplicationVersion[] getByApplicationId(String applicationId) {
        return getByDelegateId(applicationId);
    }

    /**
     * Delete all versions related to an application.
     *
     * @param applicationId The application id.
     */
    public void deleteByApplication(String applicationId) {
        deleteByDelegate(applicationId);
    }

    /**
     * Check if an application version is deployed.
     * 
     * @param applicationVersionId
     * @return isDeployed A boolean.
     */
    public boolean isApplicationVersionDeployed(String applicationVersionId) {

        GetMultipleDataResult<Deployment> dataResult = alienDAO.search(Deployment.class, null,
                MapUtil.newHashMap(new String[] { "versionId", "endDate" }, new String[][] { new String[] { applicationVersionId }, new String[] { null } }),
                1);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            return true;
        }
        return false;
    }

    /**
     * Check if a name version is already use by an other application version is a specific application.
     * 
     * @param applicationId
     * @param applicationVersionName
     * @return isUsed A boolean.
     */
    public boolean isApplicationVersionNameExist(String applicationId, String applicationVersionName) {
        return isVersionNameExist(applicationId, applicationVersionName);
    }

}
