package alien4cloud.tosca.model;

import lombok.*;

/**
 * Represents a simple scalar property value.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ScalarPropertyValue extends AbstractPropertyValue {
    private String value;
}