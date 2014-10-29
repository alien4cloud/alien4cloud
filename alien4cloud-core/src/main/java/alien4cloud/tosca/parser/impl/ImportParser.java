package alien4cloud.tosca.parser.impl;

import javax.annotation.Resource;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.csar.services.CsarService;
import alien4cloud.tosca.container.model.CSARDependency;
import alien4cloud.tosca.model.Csar;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.ToscaParsingError;

@Component
public class ImportParser implements INodeParser<CSARDependency> {
    @Resource
    private CsarService csarService;
    @Resource
    private ScalarParser scalarParser;

    @Override
    public boolean isDeffered() {
        return false;
    }

    @Override
    public CSARDependency parse(Node node, ParsingContext context) {
        String valueAsString = scalarParser.parse(node, context);
        if (valueAsString == null || valueAsString.trim().isEmpty()) {
            return null;
        }
        if (valueAsString.contains(":")) {
            String[] dependencyStrs = valueAsString.split(":");
            if (dependencyStrs.length == 2) {
                CSARDependency dependency = new CSARDependency(dependencyStrs[0], dependencyStrs[1]);
                Csar csar = csarService.getIfExists(dependency.getName(), dependency.getVersion());
                if (csar == null) {
                    // error is not a blocker as it may be caused by
                    context.getParsingErrors().add(
                            new ToscaParsingError(false, null, "Import definition is not valid", node.getStartMark(),
                                    "Specified dependency is not found in Alien 4 Cloud repository.", node.getEndMark(), valueAsString));
                    return null;
                }
                return dependency;
            }
            context.getParsingErrors().add(
                    new ToscaParsingError(true, null, "Import definition is not valid", node.getStartMark(), "Dependency should be specified as name:version",
                            node.getEndMark(), ""));
        } else {
            // TODO check that the relative file exists and trigger import
            throw new NotImplementedException("Relative import is currently not implemented in Alien 4 Cloud");
        }
        return null;
    }
}