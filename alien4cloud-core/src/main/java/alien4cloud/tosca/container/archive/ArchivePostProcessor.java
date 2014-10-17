package alien4cloud.tosca.container.archive;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

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

@Component
public class ArchivePostProcessor {

    /**
     * Post process the archive: For every definition of the model it fills the id fields in the tosca elements from the key of the elements map.
     * 
     * @param archive The archive to post process
     * @throws CSARDuplicatedElementDeclarationException
     */
    public void postProcessArchive(CloudServiceArchive archive) throws CSARDuplicatedElementDeclarationException {
        for (Map.Entry<String, Definitions> definitionsEntry : archive.getDefinitions().entrySet()) {
            postProcessDefinitions(archive, definitionsEntry.getValue(), definitionsEntry.getKey());
        }
    }

    private void postProcessDefinitions(CloudServiceArchive archive, Definitions definitions, String definitionKey)
            throws CSARDuplicatedElementDeclarationException {
        // May we do thing by reflection to do not forget the day when the model change ??
        // YAGNI ?
        postProcessElementMaps(archive.getArchiveInheritableElements(), archive.getElementIdToDefinitionKeyMapping(), definitionKey,
                definitions.getNodeTypes(), definitions.getRelationshipTypes(), definitions.getCapabilityTypes(), definitions.getArtifactTypes());
        postProcessNodeTypes(archive, definitions.getNodeTypes());

    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private final void postProcessElementMaps(Map<String, ? extends ToscaElement> globalElementsMap, Map<String, String> elementIdToDefinitionKeyMapping,
            String definitionKey, Map<String, ? extends ToscaElement>... elementMaps) throws CSARDuplicatedElementDeclarationException {
        for (Map<String, ? extends ToscaElement> elementMap : elementMaps) {
            for (Entry<String, ? extends ToscaElement> elementEntry : elementMap.entrySet()) {
                ToscaElement element = (ToscaElement) elementEntry.getValue();
                // Fill id into each tosca element
                element.setId(elementEntry.getKey());
                // Obligation to cast because of java generic mechanism
                if (((Map<String, ToscaElement>) globalElementsMap).put(elementEntry.getKey(), element) != null) {
                    throw new CSARDuplicatedElementDeclarationException(definitionKey, CSARErrorCode.DUPLICATED_ELEMENT_DECLARATION, elementEntry.getKey(),
                            "Element with key [" + elementEntry.getKey() + "] already existed in the archive definition");
                }
                elementIdToDefinitionKeyMapping.put(element.getId(), definitionKey);
            }
        }
    }

    private void postProcessNodeTypes(CloudServiceArchive archive, Map<String, NodeType> nodeTypes) {
        for (NodeType nodeType : nodeTypes.values()) {
            for (Entry<String, CapabilityDefinition> entry : nodeType.getCapabilities().entrySet()) {
                entry.getValue().setId(entry.getKey());
            }

            for (Entry<String, RequirementDefinition> entry : nodeType.getRequirements().entrySet()) {
                entry.getValue().setId(entry.getKey());
            }

            for (Interface interfaz : nodeType.getInterfaces().values()) {
                postProcessImplementationArtifact(archive, interfaz.getImplementationArtifact());
                for (Operation operation : interfaz.getOperations().values()) {
                    postProcessImplementationArtifact(archive, operation.getImplementationArtifact());
                }
            }
        }
    }

    private void postProcessImplementationArtifact(CloudServiceArchive archive, ImplementationArtifact artifact) {
        if (artifact != null) {
            artifact.setArchiveName(archive.getMeta().getName());
            artifact.setArchiveVersion(archive.getMeta().getVersion());
        }
    }
}