package alien4cloud.paas.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ESObject;

@Getter
@Setter
@ESObject
@ToString(callSuper = true)
public class PaaSDeploymentStatusMonitorEvent extends AbstractMonitorEvent {
    private DeploymentStatus deploymentStatus;
}
