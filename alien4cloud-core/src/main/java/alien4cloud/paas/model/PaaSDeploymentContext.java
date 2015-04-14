package alien4cloud.paas.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ESObject;

import alien4cloud.model.deployment.Deployment;

@Getter
@Setter
@ESObject
@ToString
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSDeploymentContext {
    private Deployment deployment;

    /**
     * Id to be used by the orchestration technology (PaaS) for the deployment.
     * 
     * @return Id to be used by the orchestration technology (PaaS) for the deployment.
     */
    public String getDeploymentId() {
        return deployment.getPaasId();
    }
}
