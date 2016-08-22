package alien4cloud.plugin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.elasticsearch.annotation.ObjectField;

/**
 * Describe a plugin configuration: PluginId + configuration Object
 * 
 * @author 'Igor Ngouagna'
 * 
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@JsonInclude(Include.NON_NULL)
@ESObject
public class PluginConfiguration {
    @Id
    private String pluginId;
    @ObjectField(enabled = false)
    private Object configuration;
}
