package alien4cloud.component.dao;

import java.util.Date;
import java.util.List;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.Tag;
import alien4cloud.tosca.model.CapabilityDefinition;
import alien4cloud.tosca.model.RequirementDefinition;

/**
 * Utility class to generate test data.
 */
public class TestModelUtil {
    public static IndexedNodeType createIndexedNodeType(String id, String archiveName, String archiveVersion, String description,
            List<CapabilityDefinition> capabilities, List<RequirementDefinition> requirements, List<String> derivedFroms, List<String> defaultCapabilities,
            List<Tag> tags, Date creationDate, Date lastUpdateDate) {
        IndexedNodeType nodeType = new IndexedNodeType();
        nodeType.setElementId(id);
        nodeType.setArchiveName(archiveName);
        nodeType.setArchiveVersion(archiveVersion);
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