package alien4cloud.deployment.matching.services.nodes;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Service to manage matching configurations within alien 4 cloud.
 */
@Service
public class MatchingConfigurationService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Get the matching configuration for a given type.
     *
     * @param type The type for which to get matching configuration.
     */
    public MatchingConfiguration getMatchingConfiguration(IndexedNodeType type) {
        // get the matching configuration for the given type.
        MatchingConfiguration configuration = getMatchingConfiguration(type.getElementId());
        if (configuration != null) {
            return configuration;
        }
        for (String derivedType : type.getDerivedFrom()) {
            configuration = getMatchingConfiguration(derivedType);
            if (configuration != null) {
                return configuration;
            }
        }
        return null;
    }

    private MatchingConfiguration getMatchingConfiguration(String type) {
        return null;
    }
}