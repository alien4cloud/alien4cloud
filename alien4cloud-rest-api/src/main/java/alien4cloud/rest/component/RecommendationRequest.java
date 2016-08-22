package alien4cloud.rest.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * A recommendation request object for a default capability
 * 
 * @author 'Igor Ngouagna'
 * 
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class RecommendationRequest {
    /**
     * The Id of the component to be recommended as default for a capability
     */
    private String componentId;

    /**
     * The capability to be recommended for.
     */
    private String capability;
}
