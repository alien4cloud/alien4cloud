package alien4cloud.paas.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.topology.AbstractTemplate;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.ScalingPolicy;
import alien4cloud.paas.IPaaSTemplate;

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
    private IndexedNodeType indexedToscaElement;
    /** The path to the archive that contains the node type. **/
    private Path csarPath;
    /** The node tempalte that actually is the parent from the current node. */
    private PaaSNodeTemplate parent;
    /** flag to know if children must be processed in sequence or in parallel. */
    private boolean createChildrenSequence = false;
    /** List of node templates that are hosted on this node template. */
    private List<PaaSNodeTemplate> children = Lists.newArrayList();

    /** node template attached on this node template. */
    // TODO separate the simple PaaSNodeTemplate from the ComputePaaSNodeTemplate
    private PaaSNodeTemplate attachedNode;

    // TODO put it in ComputePaaSNodeTemplate + must manage multiple network
    private List<PaaSNodeTemplate> networkNodes;

    /** List of relationships template with their types. */
    private List<PaaSRelationshipTemplate> relationshipTemplates = Lists.newArrayList();
    /** The scaling poilicy associated with the node if any. */
    private ScalingPolicy scalingPolicy;
    /** Groups to which this node belong to **/
    private Set<String> groups;

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

    /**
     * Get a relationship template from it's id.
     *
     * @param id The id of the relationship template to get.
     * @return The {@link PaaSRelationshipTemplate} that matches the id or null if not found.
     */
    public PaaSRelationshipTemplate getRelationshipTemplate(String id, String sourceId) {
        for (PaaSRelationshipTemplate relationshipTemplate : relationshipTemplates) {
            if (relationshipTemplate.getId().equals(id) && relationshipTemplate.getSource().equals(sourceId)) {
                return relationshipTemplate;
            }
        }
        return null;
    }

    @Override
    public AbstractTemplate getTemplate() {
        return nodeTemplate;
    }
}