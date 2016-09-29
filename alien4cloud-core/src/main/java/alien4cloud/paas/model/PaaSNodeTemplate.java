package alien4cloud.paas.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.ScalingPolicy;

import com.google.common.collect.Lists;

@Getter
@Setter
public class PaaSNodeTemplate extends AbstractPaaSTemplate<NodeType, NodeTemplate> {

    /** The path to the archive that contains the node type. **/
    private Path csarPath;
    /** The node tempalte that actually is the parent from the current node. */
    private PaaSNodeTemplate parent;
    /** flag to know if children must be processed in sequence or in parallel. */
    private boolean createChildrenSequence = false;
    /** List of node templates that are hosted on this node template. */
    private List<PaaSNodeTemplate> children = Lists.newArrayList();

    /** node template attached on this node template. Usually block storages */
    // TODO separate the simple PaaSNodeTemplate from the ComputePaaSNodeTemplate
    private List<PaaSNodeTemplate> storageNodes = Lists.newArrayList();

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
        super(id, wrapped);
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

    /**
     * @deprecated use {@link #getTemplate()} instead.
     */
    public NodeTemplate getNodeTemplate() {
        return getTemplate();
    }

}