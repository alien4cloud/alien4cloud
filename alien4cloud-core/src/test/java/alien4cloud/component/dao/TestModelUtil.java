package alien4cloud.component.dao;

import java.util.Date;
import java.util.List;

import alien4cloud.common.AlienConstants;
import org.alien4cloud.tosca.model.types.NodeType;
import alien4cloud.model.common.Tag;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;

/**
 * Utility class to generate test data.
 */
public class TestModelUtil {
    public static NodeType createIndexedNodeType(String id, String archiveName, String archiveVersion, String description,
            List<CapabilityDefinition> capabilities, List<RequirementDefinition> requirements, List<String> derivedFroms, List<String> defaultCapabilities,
            List<Tag> tags, Date creationDate, Date lastUpdateDate) {
        NodeType nodeType = new NodeType();
        nodeType.setElementId(id);
        nodeType.setArchiveName(archiveName);
        nodeType.setArchiveVersion(archiveVersion);
        nodeType.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
        nodeType.setCapabilities(capabilities);
        nodeType.setDescription(description);
        nodeType.setDefaultCapabilities(defaultCapabilities);
        nodeType.setRequirements(requirements);
        nodeType.setDerivedFrom(derivedFroms);
        nodeType.setTags(tags);
        nodeType.setCreationDate(creationDate);
        nodeType.setLastUpdateDate(lastUpdateDate);
        return nodeType;
    }
}