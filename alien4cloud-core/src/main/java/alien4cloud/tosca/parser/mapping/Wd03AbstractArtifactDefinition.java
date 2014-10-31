package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import alien4cloud.component.model.IndexedArtifactType;
import alien4cloud.tosca.parser.KeyValueMappingTarget;
import alien4cloud.tosca.parser.TypeNodeParser;
import alien4cloud.tosca.parser.TypeReferenceParserFactory;

public abstract class Wd03AbstractArtifactDefinition<T> extends AbstractMapper<T> {
    @Resource
    private TypeReferenceParserFactory typeReferenceParserFactory;

    public Wd03AbstractArtifactDefinition(Class<T> clazz) {
        super(new TypeNodeParser<T>(clazz, "Artifact definition"));
    }

    @Override
    public void initMapping() {
        instance.getYamlOrderedToObjectMapping().put(0, new KeyValueMappingTarget("id", true, "artifactRef", getScalarParser()));
        quickMap(typeReferenceParserFactory.getTypeReferenceParser(IndexedArtifactType.class), "type");
        quickMap("description");
        quickMap("mimeType");
    }
}