package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.TypeNodeParser;
import alien4cloud.tosca.parser.impl.ImportParser;

@Component
public class Wd03ArchiveRoot extends AbstractMapper<ArchiveRoot> {
    public static final String VERSION = "tosca_simple_yaml_1_0_0_wd03";

    @Resource
    private ImportParser importParser;
    @Resource
    private Wd03NodeType nodeType;
    @Resource
    private Wd03CapabilityType capabilityType;
    @Resource
    private Wd03ArtifactType artifactType;

    public Wd03ArchiveRoot() {
        super(new TypeNodeParser<ArchiveRoot>(ArchiveRoot.class, "Definition file"));
    }

    @Override
    public void initMapping() {
        quickMap("archive.toscaDefinitionsVersion");
        quickMap("archive.toscaDefaultNamespace");

        getParser().getYamlToObjectMapping().put("template_name", new MappingTarget("archive.name", getScalarParser()));
        getParser().getYamlToObjectMapping().put("template_version", new MappingTarget("archive.version", getScalarParser()));

        quickMap("archive.templateAuthor");
        quickMap("archive.description");

        getParser().getYamlToObjectMapping().put("imports", new MappingTarget("archive.dependencies", importParser));
        getParser().getYamlToObjectMapping().put("node_types", new MappingTarget("nodeTypes", nodeType.getParser()));
        getParser().getYamlToObjectMapping().put("capability_types", new MappingTarget("capabilityTypes", capabilityType.getParser()));
        getParser().getYamlToObjectMapping().put("artifact_types", new MappingTarget("artifactTypes", artifactType.getParser()));
    }
}