package alien4cloud.tosca.container.model.template;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.container.deserializer.DeploymentArtifactDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Setter
@JsonDeserialize(using = DeploymentArtifactDeserializer.class)
@SuppressWarnings("PMD.UnusedPrivateField")
public class DeploymentArtifact {
    /**
     * <p>
     * This attribute specifies the type of this artifact. The QName value of this attribute SHOULD correspond to the QName of an ArtifactType defined in the
     * same Definitions document or in an imported document.
     * </p>
     * <p>
     * The artifactType attribute specifies the artifact type specific content of the DeploymentArtifact element body and indicates the type of Artifact
     * Template referenced by the Deployment Artifact via the artifactRef attribute.
     * </p>
     */
    @Getter
    private String artifactType;
    /**
     * <p>
     * This OPTIONAL attribute contains a QName that identifies an Artifact Template to be used as deployment artifact. This Artifact Template can be defined in
     * the same Definitions document or in a separate, imported document.<br>
     * The type of Artifact Template referenced by the artifactRef attribute MUST be the same type or a sub-type of the type specified in the artifactType
     * attribute.
     * </p>
     * <p>
     * Note: if no Artifact Template is referenced, the artifact type specific content of the DeploymentArtifact element alone is assumed to represent the
     * actual artifact. For example, the contents of a simple config file could be defined in place within the DeploymentArtifact element.
     * </p>
     * <p>
     * Note, that a deployment artifact specified with the Node Template under definition overrides any deployment artifact of the same name and the same
     * artifactType (or any Artifact Type it is derived from) specified with the Node Type Implementation implementing the Node Type given as value of the type
     * attribute of the Node Template under definition. Otherwise, the deployment artifacts of Node Type Implementations and the deployment artifacts defined
     * with the Node Template are combined.
     * </p>
     */
    @Getter
    private String artifactRef;

    private String artifactName;

    /**
     * Non Tosca compliant property, the artifactRepository indicate where the artifact is stored. It might be in the archive it-self (in this case this
     * property is null), in alien's internal artifact repository (alien) or nexus, git, svn ...
     */
    @Getter
    private String artifactRepository;

    public String getArtifactName() {
        return artifactName != null ? artifactName : artifactRef;
    }
}
