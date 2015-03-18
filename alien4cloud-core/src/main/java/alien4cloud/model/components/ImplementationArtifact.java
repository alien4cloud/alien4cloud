package alien4cloud.model.components;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.ui.form.annotation.FormProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Specifies an implementation artifact for interfaces or operations of a {@link NodeType node type} or {@link RelationshipType relation type}.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@FormProperties({ "interfaceName", "operationName", "artifactType", "artifactRef" })
public class ImplementationArtifact implements IArtifact {
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

    @Override
    @JsonIgnore
    public String getArtifactRepository() {
        return null;
    }
}