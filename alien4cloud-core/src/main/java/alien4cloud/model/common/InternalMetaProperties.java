package alien4cloud.model.common;

/**
 * 
 * @author mourouvi
 *
 */
public enum InternalMetaProperties {

    cloud_meta_, cloud_tags_, app_meta_, app_tags_, env_meta_, env_tags_;

    /**
     * True when the given meta name starts by one of the internal meta
     * 
     * @return
     */
    static public boolean isInternalMeta(String metaPropertyName) {
        for (InternalMetaProperties meta : InternalMetaProperties.values()) {
            if (metaPropertyName.startsWith(meta.name()))
                return true;
        }
        return false;
    }

}
