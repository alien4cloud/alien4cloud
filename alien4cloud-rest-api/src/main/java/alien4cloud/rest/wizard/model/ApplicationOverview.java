package alien4cloud.rest.wizard.model;

import alien4cloud.model.application.Application;
import alien4cloud.paas.model.DeploymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApplicationOverview extends TopologyOverview {

    private Application application;

    private DeploymentStatus deploymentStatus;

}
