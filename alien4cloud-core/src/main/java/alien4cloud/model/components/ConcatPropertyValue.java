package alien4cloud.model.components;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.json.deserializer.OperationParameterDeserializer;
import alien4cloud.ui.form.annotation.FormProperties;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@FormProperties({ "function_concat", "parameters" })
public class ConcatPropertyValue extends AbstractPropertyValue {
    private String function_concat;
    @JsonDeserialize(contentUsing = OperationParameterDeserializer.class)
    private List<IOperationParameter> parameters;
}
