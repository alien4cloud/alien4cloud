package alien4cloud.tosca;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.tosca.container.exception.CSARDuplicatedElementDeclarationException;
import alien4cloud.tosca.container.model.CloudServiceArchive;
import alien4cloud.tosca.container.model.Definitions;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.container.model.type.ImplementationArtifact;
import alien4cloud.tosca.container.model.type.Interface;
import alien4cloud.tosca.container.model.type.NodeType;
import alien4cloud.tosca.container.model.type.Operation;
import alien4cloud.tosca.container.model.type.RequirementDefinition;
import alien4cloud.tosca.container.validation.CSARErrorCode;
import alien4cloud.tosca.model.ArchiveRoot;

@Component
public class ArchivePostProcessor {
    /**
     * Post process the archive: For every definition of the model it fills the id fields in the tosca elements from the key of the elements map.
     * 
     * @param archive The archive to post process
     * @throws CSARDuplicatedElementDeclarationException in case an element is declared multiple times.
     */
    public void postProcessArchive(ArchiveRoot archive) throws CSARDuplicatedElementDeclarationException {
        doPostProcess(archive);
    }

    private void doPostProcess(ArchiveRoot archive) {
        Map<String, ? extends IndexedToscaElement> globalElementsMap = Maps.newHashMap();
        postProcessElementMaps(globalElementsMap, archive.getNodeTypes(), archive);
        for (ArchiveRoot root : archive.getLocalImports()) {

        }
        postProcessNodeTypes(archive, archive.getNodeTypes());
    }

    @SuppressWarnings("unchecked")
    // @SafeVarargs
    private final void postProcessElementMaps(ArchiveRoot archive, Map<String, ? extends IndexedToscaElement> globalElementsMap,
            Map<String, ? extends IndexedToscaElement>... elementMaps) throws CSARDuplicatedElementDeclarationException {
        for (Map<String, ? extends IndexedToscaElement> elementMap : elementMaps) {
            for (Entry<String, ? extends IndexedToscaElement> elementEntry : elementMap.entrySet()) {
                IndexedToscaElement element = elementEntry.getValue();
                // Fill id into each tosca element
                element.setId(elementEntry.getKey());
                // Obligation to cast because of java generic mechanism
                if (((Map<String, IndexedToscaElement>) globalElementsMap).put(elementEntry.getKey(), element) != null) {
                    throw new CSARDuplicatedElementDeclarationException(archive.getFileName(), CSARErrorCode.DUPLICATED_ELEMENT_DECLARATION,
                            elementEntry.getKey(), "Element with key [" + elementEntry.getKey() + "] already existed in the archive definition");
                }
            }
        }
    }

    private void postProcessNodeTypes(ArchiveRoot archive, Map<String, IndexedNodeType> nodeTypes) {
        for (IndexedNodeType nodeType : nodeTypes.values()) {
            for (Interface interfaz : nodeType.getInterfaces().values()) {
                for (Operation operation : interfaz.getOperations().values()) {
                    postProcessImplementationArtifact(archive, operation.getImplementationArtifact());
                }
            }
        }
    }

    private void postProcessImplementationArtifact(ArchiveRoot archive, ImplementationArtifact artifact) {
        if (artifact != null) {
            artifact.setArchiveName(archive.getArchive().getName());
            artifact.setArchiveVersion(archive.getArchive().getVersion());
        }
    }
}