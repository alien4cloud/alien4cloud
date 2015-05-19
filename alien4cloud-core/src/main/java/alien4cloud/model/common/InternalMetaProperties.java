package alien4cloud.model.common;

/**
 * Handle internal meta properties
 * 
 * @author mourouvi
 *
 */
public enum InternalMetaProperties {

    cloud_meta_, cloud_tags_, app_meta_, app_tags_, env_meta_, env_tags_;

    /**
     * True when the given meta name starts by one of the internal meta
     * Not case sensitive
     * 
     * @return
     */
    static public boolean isInternalMeta(String metaPropertyName) {
        for (InternalMetaProperties meta : InternalMetaProperties.values()) {
            if (metaPropertyName.toLowerCase().startsWith(meta.name()))
                return true;
        }
        return false;
    }

    static public boolean isCloudMeta(String metaPropertyName) {
        return metaPropertyName.toLowerCase().startsWith(InternalMetaProperties.cloud_meta_.toString())
                || metaPropertyName.toLowerCase().startsWith(InternalMetaProperties.cloud_tags_.toString());
    }

    static public boolean isApplicationMeta(String metaPropertyName) {
        return metaPropertyName.toLowerCase().startsWith(InternalMetaProperties.app_meta_.toString())
                || metaPropertyName.toLowerCase().startsWith(InternalMetaProperties.app_tags_.toString());
    }

    static public boolean isEnvironmentMeta(String metaPropertyName) {
        return metaPropertyName.toLowerCase().startsWith(InternalMetaProperties.env_meta_.toString())
                || metaPropertyName.toLowerCase().startsWith(InternalMetaProperties.env_tags_.toString());
    }
}
