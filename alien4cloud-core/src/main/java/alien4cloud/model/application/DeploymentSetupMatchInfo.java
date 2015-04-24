package alien4cloud.model.application;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.cloud.CloudResourceTopologyMatchResult;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
public class DeploymentSetupMatchInfo extends DeploymentSetup {
    private boolean isValid;

    private CloudResourceTopologyMatchResult matchResult;

    /** Default constructor for de-serialization */
    public DeploymentSetupMatchInfo() {
    }

    /**
     * Initialize a {@link DeploymentSetupMatchInfo} from an existing {@link DeploymentSetup};
     * 
     * @param initFrom The deployment setup from which to init the {@link DeploymentSetupMatchInfo}.
     */
    public DeploymentSetupMatchInfo(DeploymentSetup initFrom) {
        super(initFrom.getId(), initFrom.getVersionId(), initFrom.getEnvironmentId(), initFrom.getProviderDeploymentProperties(),
                initFrom.getInputProperties(), initFrom.getCloudResourcesMapping(), initFrom.getNetworkMapping(), initFrom.getStorageMapping(), initFrom
                        .getAvailabilityZoneMapping());
    }

    @JsonIgnore
    public DeploymentSetup getDeploymentSetup() {
        return new DeploymentSetup(getId(), getVersionId(), getEnvironmentId(), getProviderDeploymentProperties(), getInputProperties(),
                getCloudResourcesMapping(), getNetworkMapping(), getStorageMapping(), getAvailabilityZoneMapping());
    }
}