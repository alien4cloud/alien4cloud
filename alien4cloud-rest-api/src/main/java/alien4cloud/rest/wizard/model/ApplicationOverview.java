package alien4cloud.rest.wizard.model;

import alien4cloud.model.application.Application;
import alien4cloud.paas.model.DeploymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApplicationOverview {

    private Application application;

    private String description;

    /**
     * The meta properties related to the application. Key is the human readable name of the meta-property.
     */
    private List<MetaProperty> namedMetaProperties;

    /**
     * A subset of application's topology nodes that have functional meaning.
     * Generally, we just have here nodes of types that have some meta-properties filled.
     */
    private List<ApplicationModule> modules;

    private DeploymentStatus deploymentStatus;

    private TopologyGraph topologyGraph;

    private String topologyId;
    private String topologyVersion;

}
