package alien4cloud.tosca.parser.mapping;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.model.ToscaMeta;
import alien4cloud.tosca.parser.ListParser;
import alien4cloud.tosca.parser.TypeNodeParser;

@Deprecated
@Component
public class CsarMetaMapping extends AbstractMapper<ToscaMeta> {
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
    }
}