package alien4cloud.plugin;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.plugin.model.PluginDescriptor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Describe a plugin for Alien 4 Cloud
 *
 * @author luc boutier
 */
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ESObject
public class Plugin {
    @Getter
    @Setter
    private PluginDescriptor descriptor;
    @Getter
    @Setter
    private String pluginPathId;
    @Getter
    @Setter
    @TermFilter
    private boolean enabled;
    @Getter
    @Setter
    private boolean configurable;

    /** Do not use that */
    @Deprecated
    private String esId;

    @Id
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    public String getId() {
        if (descriptor == null) {
            throw new IndexingServiceException("Plugin descriptor is mandatory");
        }
        if (descriptor.getId() == null) {
            throw new IndexingServiceException("Plugin id is mandatory");
        }
        return descriptor.getId();
    }

    public void setId(String id) {
        // This is used to keep track of the id in elasticsearch for 1.3.1 migration (no more version in id) See ApplicationBootstrap for more details.
        this.esId = id;
    }

    /**
     * Method to get the id as saved in elasticsearch.
     * 
     * @return Returns the plugin id as saved in elasticsearch.
     */
    @Deprecated
    public String getEsId() {
        return esId;
    }

    /**
     * Create a plugin from a given descriptor. The plugin is disabled by default.
     *
     * @param descriptor The descriptor for the plugin.
     * @param pluginPathId The id under which the plugin exists on the file system.
     */
    public Plugin(PluginDescriptor descriptor, String pluginPathId) {
        this.descriptor = descriptor;
        this.pluginPathId = pluginPathId;
        this.enabled = false;
    }
}