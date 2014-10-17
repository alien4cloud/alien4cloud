package alien4cloud.tosca.container.model;

import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.container.exception.CSARParsingException;
import alien4cloud.tosca.container.validation.CSARErrorCode;

import com.google.common.collect.Maps;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudServiceArchive {

    private CSARMeta meta;

    @Setter(value = AccessLevel.NONE)
    private Map<String, Definitions> definitions = Maps.newHashMap();

    /**
     * Map definition key to all non inheritable archive elements, will be filled up by the post processor
     */
    @Setter(value = AccessLevel.NONE)
    private Map<String, ToscaElement> archiveNonInheritableElements = Maps.newHashMap();

    /**
     * Map definition key to all inheritable archive elements, will be filled up by the post processor
     */
    @Setter(value = AccessLevel.NONE)
    private Map<String, ToscaInheritableElement> archiveInheritableElements = Maps.newHashMap();

    /**
     * This map stores the mapping between an element id to its definition key (the path of the definition). It helps to keep track of which element is found in
     * which definition. It helps at least for better tracking when something goes wrong in one of the definitions file (if there are a lots of them)
     */
    @Setter(value = AccessLevel.NONE)
    private Map<String, String> elementIdToDefinitionKeyMapping = Maps.newHashMap();

    /**
     * Add a definitions to the archive.
     * 
     * @param def
     * @throws CSARParsingException when 2 definitions have the same key
     */
    public void putDefinitions(String definitionKey, Definitions def) throws CSARParsingException {
        if (definitions.put(definitionKey, def) != null) {
            throw new CSARParsingException(definitionKey, CSARErrorCode.DUPLICATED_DEFINITION_DECLARATION,
                    "Duplicated definition entry detected in CSAR meta file");
        }
    }

    /**
     * Retrieve the definition key from the element id.
     * 
     * @param elementId
     * @return
     */
    public String getElementDefinition(String elementId) {
        return this.elementIdToDefinitionKeyMapping.get(elementId);
    }
}