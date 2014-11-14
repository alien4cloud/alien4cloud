package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import alien4cloud.component.model.IndexedArtifactType;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.impl.advanced.TypeReferenceParserFactory;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

public abstract class Wd03AbstractArtifactDefinition<T> extends AbstractMapper<T> {
    @Resource
    private TypeReferenceParserFactory typeReferenceParserFactory;

    public Wd03AbstractArtifactDefinition(Class<T> clazz) {
        super(new TypeNodeParser<T>(clazz, "Artifact definition"));
    }

    @Override
    public void initMapping() {
        instance.getYamlOrderedToObjectMapping().put(0, new MappingTarget("artifactRef", getScalarParser()));
        instance.getYamlToObjectMapping().put("type",
                new MappingTarget("artifactType", typeReferenceParserFactory.getTypeReferenceParser(IndexedArtifactType.class)));
        quickMap("description");
        quickMap("mimeType");
    }
}