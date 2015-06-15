package alien4cloud.plugin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Describe a plugin configuration: PluginId + configuration Object
 * 
 * @author 'Igor Ngouagna'
 * 
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
@ESObject
public class PluginConfiguration {
    @Id
    private String pluginId;
    private Object configuration;
}
