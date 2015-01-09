package alien4cloud.tosca.parser.mapping;

import org.springframework.stereotype.Component;

import alien4cloud.model.components.CSARDependency;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

@Component
public class CsarMetaDependencyMapping extends AbstractMapper<CSARDependency> {

    public CsarMetaDependencyMapping() {
        super(new TypeNodeParser<CSARDependency>(CSARDependency.class, "CSAR dependency."));
    }

    @Override
    public void initMapping() {
        quickMap("name");
        quickMap("version");
    }
}