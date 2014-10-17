package alien4cloud.model.deployment;

/**
 * Represent a resource that can be deployed on the cloud, it can be an application or test in a cloud service archive
 * 
 * @author Minh Khang VU
 */
public interface IDeploymentSource {

    String getId();

    String getName();
}
