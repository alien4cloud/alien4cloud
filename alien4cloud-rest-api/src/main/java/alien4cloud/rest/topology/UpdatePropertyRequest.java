package alien4cloud.rest.topology;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Posted object to update a NodeTemplate property
 * 
 * @author mourouvi
 * 
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class UpdatePropertyRequest {
    // property to update
    private String propertyName;
    private String propertyValue;
}
