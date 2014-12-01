package alien4cloud.paas.model;

import java.nio.file.Path;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.tosca.container.model.topology.NodeTemplate;
import alien4cloud.tosca.container.model.topology.ScalingPolicy;

import com.google.common.collect.Lists;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class PaaSNodeTemplate implements IPaaSTemplate<IndexedNodeType> {
    /** The unique id for the node template within the topology. */
    private String id;
    /** The node template that contains user's settings. */
    private NodeTemplate nodeTemplate;
    /** Node type for the wrapped node template. */
    private IndexedNodeType indexedNodeType;
    /** The path to the archive that contains the node type. **/
    private Path csarPath;
    /** The node tempalte that actually is the parent from the current node. */
    private PaaSNodeTemplate parent;
    /** List of node templates that are hosted on this node template. */
    private List<PaaSNodeTemplate> children = Lists.newArrayList();

    /** node template attached on this node template. */
    // TODO separate the simple PaaSNodeTemplate from the ComputePaaSNodeTemplate
    private PaaSNodeTemplate attachedNode;

    // TODO put it in ComputePaaSNodeTemplate + must manage multiple network
    private PaaSNodeTemplate networkNode;

    /** List of relationships template with their types. */
    private List<PaaSRelationshipTemplate> relationshipTemplates = Lists.newArrayList();
    /** The scaling poilicy associated with the node if any. */
    private ScalingPolicy scalingPolicy;

    /**
     * Create a PaaS node template from a given node template (out of a topology).
     * 
     * @param wrapped
     *            The node template wrapped by this {@link PaaSNodeTemplate}.
     */
    public PaaSNodeTemplate(String id, NodeTemplate wrapped) {
        this.id = id;
        this.nodeTemplate = wrapped;
    }

    @Override
    public void setIndexedToscaElement(IndexedNodeType indexedNodeType) {
        this.indexedNodeType = indexedNodeType;
    }

    /**
     * Get a relationship template from it's id.
     * 
     * @param id The id of the relationship template to get.
     * @return The {@link PaaSRelationshipTemplate} that matches the id or null if not found.
     */
    public PaaSRelationshipTemplate getRelationshipTemplate(String id) {
        for (PaaSRelationshipTemplate relationshipTemplate : relationshipTemplates) {
            if (relationshipTemplate.getId().equals(id)) {
                return relationshipTemplate;
            }
        }
        return null;
    }
}