package alien4cloud.rest.wizard;

import alien4cloud.model.common.MetaPropConfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 */
@Getter
@Setter
@AllArgsConstructor
public class MetaProperty {

    /**
     * The meta-property definition.
     */
    private MetaPropConfiguration configuration;

    /**
     * The property value.
     */
    private String value;


}
