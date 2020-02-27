package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.InvalidVersionException;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.util.List;
import java.util.Map;

/**
 * Import parser that doesn't validate anything
 * For validation of version or presence in catalog, see {@link ImportParser}
 */
@Slf4j
@Component
public class LaxImportParser implements INodeParser<CSARDependency> {
    @Resource
    private ScalarParser scalarParser;
    @Resource
    private BaseParserFactory baseParserFactory;

    @Override
    public CSARDependency parse(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            return parseScalarNode((ScalarNode) node, context);
        } else {
            MapParser<String> mapParser = baseParserFactory.<String>getMapParser(scalarParser,
                    "string");
            Map<String, String> value = mapParser.parse(node, context);
            if (!value.isEmpty()) {
                Map.Entry<String, String> entry = value.entrySet().iterator().next();
                String dependencyName = entry.getKey();
                String dependencyVersion = entry.getValue();
                // Check if the provided version is an URL
                return checkVersion(dependencyName, dependencyVersion, node, context);
            }
            return null;
        }
    }

    public CSARDependency checkVersion(String dependencyName, String dependencyVersion,
                                              Node node, ParsingContextExecution context) {
        // check that version has the righ format
        try {
            VersionUtil.parseVersion(dependencyVersion);
        } catch (InvalidVersionException e) {
            context.getParsingErrors()
                    .add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.SYNTAX_ERROR,
                            "Version specified in the dependency is not a valid version.", node.getStartMark(),
                            "Dependency should be specified as name:version", node.getEndMark(), "Import"));
            return null;
        }
        return new CSARDependency(dependencyName, dependencyVersion);
    }

    public CSARDependency parseScalarNode(ScalarNode node, ParsingContextExecution context) {
        String valueAsString = scalarParser.parse(node, context);
        if (StringUtils.isNotBlank(valueAsString)) {
            if (valueAsString.contains(":")) {
                String[] dependencyStrs = valueAsString.split(":");
                if (dependencyStrs.length == 2) {
                    // Eliminate unwanted chars
                    String dependencyName = dependencyStrs[0]
                            .trim()
                            .replaceAll("^\"|^\'|\"$|\'$", "");
                    String dependencyVersion = dependencyStrs[1]
                            .trim()
                            .replaceAll("^\"|^\'|\"$|\'$", "");
                    return checkVersion(dependencyName, dependencyVersion, node, context);
                }
                context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR, "Import definition is not valid",
                        node.getStartMark(), "Dependency should be specified as name:version", node.getEndMark(), "Import"));
            } else {
                context.getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR, "Relative import is currently not supported in Alien 4 Cloud",
                                node.getStartMark(), "Dependency should be specified as name:version", node.getEndMark(), "Import"));
            }
        }
        return null;
    }
}