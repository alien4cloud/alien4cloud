package alien4cloud.rest.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Posted object to update a component tag
 * 
 * @author mourouvi
 * 
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class UpdateTagRequest {
    // custom tag to update
    private String tagKey;
    private String tagValue;
}
