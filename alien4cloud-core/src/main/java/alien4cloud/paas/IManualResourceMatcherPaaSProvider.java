package alien4cloud.paas;

import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.cloud.CloudResourceType;

/**
 * This interface defines contract for PaaS provider which do not discover resources automatically.
 * User will have to configure the mapping between alien resources and PaaS (IaaS) resources
 *
 * @author Minh Khang VU
 */
public interface IManualResourceMatcherPaaSProvider {

    /**
     * Call to initialize or notify the paaS provider about configuration change
     *
     * @param config the config to take into account
     */
    void updateMatcherConfig(CloudResourceMatcherConfig config);

    /**
     * Call to determine available ids for the given resource type
     * 
     * @param resourceType the type of the resource
     * @return ids for the given resource type
     */
    String[] getAvailableResourceIds(CloudResourceType resourceType);
}
