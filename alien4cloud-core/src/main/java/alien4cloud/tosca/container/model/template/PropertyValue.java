package alien4cloud.tosca.container.model.template;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.tosca.container.deserializer.PropertyValueDeserializer;
import alien4cloud.tosca.container.serializer.PropertyValueSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = PropertyValueDeserializer.class)
@JsonSerialize(using = PropertyValueSerializer.class)
@EqualsAndHashCode
public class PropertyValue {
    private String value;
}