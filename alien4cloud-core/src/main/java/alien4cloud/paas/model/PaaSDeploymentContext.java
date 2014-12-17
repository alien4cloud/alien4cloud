package alien4cloud.paas.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ESObject;

@Getter
@Setter
@ESObject
@ToString
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSDeploymentContext {

    /**
     * Id of an deployment (install/uninstall operation)
     * For the same recipe, we might have multiple deployment ids.
     */
    private String deploymentId;

    /**
     * A recipe's id represents in general the topology + setup in alien's term.
     * For ex : cloudify 3 will use this information to name the blueprint and
     * deployment.
     */
    private String recipeId;
}
