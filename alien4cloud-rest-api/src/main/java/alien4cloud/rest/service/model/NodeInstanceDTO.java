package alien4cloud.rest.service.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Maps;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

/**
 * Represents a node instance as defined in a service.
 */
@Getter
@Setter
@ApiModel("Represents a simple node instance with it's properties and attributes.")
public class NodeInstanceDTO {
    @ApiModelProperty(value = "The tosca node type of the instance.", notes = "The field is not required for patch operation where undefined will be considered as no update.", required = true)
    @NotBlank(groups = UpdateValidationGroup.class)
    private String type;
    @ApiModelProperty(value = "The version of the tosca node type of the instance.", notes = "The field is not required for patch operation where undefined will be considered as no update.", required = true)
    @NotBlank(groups = UpdateValidationGroup.class)
    private String typeVersion;

    @ApiModelProperty(value = "Map of property values that must match the properties defined in the instance type.")
    @ConditionalOnAttribute(ConditionalAttributes.REST)
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, AbstractPropertyValue> properties;

    @ApiModelProperty(value = "Map of capability that contains the values of the properties as defined in the instance type.")
    @ConditionalOnAttribute(value = { ConditionalAttributes.REST, ConditionalAttributes.ES_1_2 })
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    private Map<String, Capability> capabilities;

    @ApiModelProperty(value = "Map of values for the runtime attributes of a tosca instance.")
    private Map<String, String> attributeValues = Maps.newHashMap();
}