package alien4cloud.tosca.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A TOSCA function to be used as the value for a property (or operation parameter).
 */
@Getter
@Setter
public class FunctionPropertyValue extends AbstractPropertyValue {
    private String function;
    private List<String> parameters;
}