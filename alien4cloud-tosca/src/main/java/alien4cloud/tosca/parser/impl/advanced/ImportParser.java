package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ImportParser implements INodeParser<CSARDependency> {
    @Resource
    private ScalarParser scalarParser;

    @Override
    public CSARDependency parse(Node node, ParsingContextExecution context) {
        String valueAsString = scalarParser.parse(node, context);
        if (valueAsString == null || valueAsString.trim().isEmpty()) {
            return null;
        }
        if (valueAsString.contains(":")) {
            String[] dependencyStrs = valueAsString.split(":");
            if (dependencyStrs.length == 2) {
                CSARDependency dependency = new CSARDependency(dependencyStrs[0], dependencyStrs[1]);
                Csar csar = ToscaContext.get().getArchive(dependency.getName(), dependency.getVersion());
                log.debug("Import {} {} {}", dependency.getName(), dependency.getVersion(), csar);
                if (csar == null) {
                    // error is not a blocker, as long as no type is missing we just mark it as a warning.
                    context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.MISSING_DEPENDENCY, "Import definition is not valid",
                            node.getStartMark(), "Specified dependency is not found in Alien 4 Cloud repository.", node.getEndMark(), valueAsString));
                } else {
                    ToscaContext.get().addDependency(dependency);
                }
                return dependency;
            }
            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR, "Import definition is not valid",
                    node.getStartMark(), "Dependency should be specified as name:version", node.getEndMark(), "Import"));
        } else {
            // TODO check that the relative file exists and trigger import
            throw new NotSupportedException("Relative import is currently not supported in Alien 4 Cloud");
        }
        return null;
    }
}