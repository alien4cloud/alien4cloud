package alien4cloud.tosca.parser.mapping;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedArtifactType;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

@Component
public class Wd03ArtifactType extends Wd03InheritableToscaElement<IndexedArtifactType> {
    public Wd03ArtifactType() {
        super(new TypeNodeParser<IndexedArtifactType>(IndexedArtifactType.class, "Artifact type"), IndexedArtifactType.class);
    }

    @Override
    public void initMapping() {
        super.initMapping();
        quickMap("mimeType");
        quickMap(new ListParser<String>(getScalarParser(), "file_ext"), "fileExt");
    }
}