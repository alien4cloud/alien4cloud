package alien4cloud.rest.topology;

import alien4cloud.model.topology.RelationshipTemplate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class AddRelationshipTemplateRequest {

    private RelationshipTemplate relationshipTemplate;

    private String archiveName;

    private String archiveVersion;
}
