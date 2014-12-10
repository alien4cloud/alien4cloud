package alien4cloud.plugin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.exception.IndexingServiceException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Describe a plugin for Alien 4 Cloud
 *
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
@ESObject
public class Plugin {
    public static final String LIST_FETCH_CONTEXT = "list";

    private PluginDescriptor descriptor;
    private String pluginPathId;
    private boolean enabled;
    private boolean configurable;

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
        if (descriptor.getVersion() == null) {
            throw new IndexingServiceException("Plugin version is mandatory");
        }
        return descriptor.getId() + ":" + descriptor.getVersion();
    }

    public void setId(String id) {
        // Not authorized to set id as it's auto-generated
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
        this.enabled = true;
    }
}