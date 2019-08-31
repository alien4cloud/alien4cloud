package alien4cloud.rest.wizard.model;

import alien4cloud.model.application.Application;
import alien4cloud.paas.model.DeploymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TopologyOverview {

    private String description;

    /**
     * The meta properties related to the application. Key is the human readable name of the meta-property.
     */
    private List<MetaProperty> namedMetaProperties;

    private String[] componentCategories;

    /**
     * Subsets of application's topology nodes that have functional meaning.
     * Generally, we just have here nodes of types that have some meta-properties filled.
     *
     * key is the category name.
     */
    private Map<String, List<ApplicationModule>> componentsPerCategory;

    private String topologyId;
    private String topologyVersion;

}
