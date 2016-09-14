package alien4cloud.model.deployment;

import lombok.Getter;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import org.alien4cloud.tosca.model.Csar;

/**
 * The type of resource which is at the origin of the deployment
 * 
 * @author Minh Khang VU
 */
@Getter
public enum DeploymentSourceType {

    APPLICATION(Application.class), CSAR(Csar.class);

    private Class<?> sourceType;

    DeploymentSourceType(Class<?> sourceType) {
        this.sourceType = sourceType;
    }

    public static DeploymentSourceType fromSourceType(Class<?> fromSourceType) {
        DeploymentSourceType[] availableSources = values();
        for (DeploymentSourceType source : availableSources) {
            if (source.getSourceType() == fromSourceType) {
                return source;
            }
        }
        throw new NotFoundException("Source type not found for " + fromSourceType.getName());
    }
}
