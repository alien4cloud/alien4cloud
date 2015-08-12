package alien4cloud.rest.application;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Posted deployment property object
 * 
 * @author mourouvi
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudDeploymentPropertyValidationRequest {
    /** Id of the cloud on which to deploy. */
    private String cloudId;
    /** Name of the property for which to check constraints. */
    private String deploymentPropertyName;
    /** Value of the property to check. */
    private String deploymentPropertyValue;
}