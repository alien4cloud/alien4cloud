package alien4cloud.tosca.container.model.type;

import alien4cloud.ui.form.annotation.FormProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Specifies an implementation artifact for interfaces or operations of a {@link NodeType node type} or {@link RelationshipType relation type}.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@FormProperties({ "interfaceName", "operationName", "artifactType", "artifactRef" })
public class ImplementationArtifact {
    /**
     * <p>
     * This OPTIONAL attribute specifies the name of the interface that is implemented by the actual implementation artifact.
     * </p>
     * <p>
     * If not specified, the implementation artifact is assumed to provide the implementation for all interfaces defined by the {@link NodeType node type} or
     * {@link RelationshipType relationship type} referred to by the relationshipType attribute of the containing node type impl.
     * {@link RelationshipTypeImplementation relationship type implementation}.
     * </p>
     */
    private String interfaceName;

    /**
     * <p>
     * This OPTIONAL attribute specifies the name of the operation that is implemented by the actual implementation artifact.
     * </p>
     * <p>
     * If specified, the interfaceName MUST be specified and the specified operationName MUST refer to an operation of the specified interface. If not
     * specified, the implementation artifact is assumed to provide the implementation for all operations defined within the specified interface.
     * </p>
     */
    private String operationName;

    /**
     * <p>
     * Specifies the type of this artifact.
     * </p>
     */
    private String artifactType;

    /**
     * <p>
     * Identifies an Artifact Template to be used as implementation artifact. This Artifact Template can be defined in the same Definitions document or in a
     * separate, imported document.
     * </p>
     * 
     * <p>
     * The type of Artifact Template referenced by the artifactRef attribute MUST be the same type or a sub-type of the type specified in the artifactType
     * attribute.
     * </p>
     * 
     * <p>
     * Note: if no Artifact Template is referenced, the artifact type specific content of the ImplementationArtifact element alone is assumed to represent the
     * actual artifact. For example, a simple script could be defined in place within the ImplementationArtifact element.
     * </p>
     */
    private String artifactRef;

    /**
     * The name of the archive in which the artifact lies.
     */
    private String archiveName;
    /**
     * The version of the archive in which the artifact lies.
     */
    private String archiveVersion;
}