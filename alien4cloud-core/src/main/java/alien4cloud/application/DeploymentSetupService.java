package alien4cloud.application;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.ComputeTemplate;
import alien4cloud.tosca.container.model.template.PropertyValue;
import alien4cloud.tosca.container.model.type.PropertyDefinition;

import com.google.common.collect.Maps;

/**
 * Manages deployment setups.
 */
@Service
public class DeploymentSetupService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    private DeploymentSetup get(String versionId, String environmentId) {
        return alienDAO.findById(DeploymentSetup.class, generateId(versionId, environmentId));
    }

    public DeploymentSetup getOrFail(String versionId, String environmentId) {
        DeploymentSetup setup = get(versionId, environmentId);
        if (setup == null) {
            throw new NotFoundException("No setup found for version [" + versionId + "] and environment [" + environmentId + "]");
        } else {
            return setup;
        }
    }

    public DeploymentSetup create(ApplicationVersion version, ApplicationEnvironment environment) {
        DeploymentSetup deploymentSetup = new DeploymentSetup();
        deploymentSetup.setId(generateId(version.getId(), environment.getId()));
        deploymentSetup.setEnvironmentId(environment.getId());
        deploymentSetup.setVersionId(version.getId());
        alienDAO.save(deploymentSetup);
        return deploymentSetup;
    }

    public void fillWithDefaultValues(DeploymentSetup deploymentSetup, Map<String, List<ComputeTemplate>> matchResult,
            Map<String, PropertyDefinition> propertyDefinitionMap) {
        // Generate default matching for deployment setup
        Map<String, ComputeTemplate> cloudResourcesMapping = Maps.newHashMap();
        for (Map.Entry<String, List<ComputeTemplate>> entry : matchResult.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                cloudResourcesMapping.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        deploymentSetup.setCloudResourcesMapping(cloudResourcesMapping);
        // Reset deployment properties as it might have changed between cloud
        Map<String, PropertyValue> propertyValueMap = Maps.newHashMap();
        for (Map.Entry<String, PropertyDefinition> propertyDefinitionEntry : propertyDefinitionMap.entrySet()) {
            propertyValueMap.put(propertyDefinitionEntry.getKey(), new PropertyValue(propertyDefinitionEntry.getValue().getDefault()));
        }
        deploymentSetup.setProviderDeploymentProperties(propertyValueMap);
    }

    public String generateId(String versionId, String environmentId) {
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
     * @param environmentId The id of the version
     */
    public void deleteByVersionId(String environmentId) {
        alienDAO.delete(DeploymentSetup.class, QueryBuilders.termQuery("versionId", environmentId));
    }
}
