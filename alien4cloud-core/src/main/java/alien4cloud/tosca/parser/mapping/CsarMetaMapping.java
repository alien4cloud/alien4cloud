package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.container.model.CSARDependency;
import alien4cloud.tosca.model.ToscaMeta;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.SetParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

@Deprecated
@Component
public class CsarMetaMapping extends AbstractMapper<ToscaMeta> {
    @Resource
    private CsarMetaDependencyMapping dependencyMapping;

    public CsarMetaMapping() {
        super(new TypeNodeParser<ToscaMeta>(ToscaMeta.class, "Alien CSAR meta file."));
    }

    @Override
    public void initMapping() {
        quickMap("name");
        quickMap("version");
        quickMap("license");
        quickMap("createdBy");
        quickMap("entryDefinitions");
        quickMap(new ListParser<String>(getScalarParser(), "definitions"), "definitions");

        getParser().getYamlToObjectMapping().put("dependencies",
                new MappingTarget("dependencies", new SetParser<CSARDependency>(dependencyMapping.getParser(), "Dependencies")));
    }
}