package alien4cloud.model.common;

import java.util.Map;

/**
 * Interface to be implemented by resources that supports meta-properties management.
 */
public interface IMetaProperties {
    /**
     * Set the map of meta-properties on the resource.
     *
     * @param metaProperties the map of meta-properties.
     */
    void setMetaProperties(Map<String, String> metaProperties);

    /**
     * Get the map of meta-properties on the resource.
     *
     * @return the map of meta-properties.
     */
    Map<String, String> getMetaProperties();
}