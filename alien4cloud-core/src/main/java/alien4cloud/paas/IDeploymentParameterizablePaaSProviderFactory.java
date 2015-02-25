package alien4cloud.paas;

import java.util.Map;

import alien4cloud.model.components.PropertyDefinition;

public interface IDeploymentParameterizablePaaSProviderFactory<T extends IPaaSProvider> extends IPaaSProviderFactory<T> {

    /**
     * Get the deployment property definition
     *
     * @return A map containing property definitions
     */
    Map<String, PropertyDefinition> getDeploymentPropertyDefinitions();
}
