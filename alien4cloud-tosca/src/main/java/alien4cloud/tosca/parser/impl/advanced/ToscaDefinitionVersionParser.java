package alien4cloud.tosca.parser.impl.advanced;

import java.util.HashSet;
import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.ToscaNormativeImports;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;

@Component
public class ToscaDefinitionVersionParser implements INodeParser<String> {
    private static ThreadLocal<Boolean> recursiveCall = new ThreadLocal<>();

    @Override
    public String parse(Node node, ParsingContextExecution context) {
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getParent();
        String toscaDefinitionVersion = ParserUtils.getScalar(node, context);
        if (toscaDefinitionVersion != null) {
            CSARDependency dependency = ToscaNormativeImports.IMPORTS.get(toscaDefinitionVersion);
            if (dependency != null) {
                if (ToscaNormativeImports.TOSCA_NORMATIVE_TYPES.equals(dependency.getName())) {
                    if (recursiveCall.get() == null) {
                        recursiveCall.set(true);
                    } else {
                        return toscaDefinitionVersion;
                    }
                }

                Set<CSARDependency> dependencies = archiveRoot.getArchive().getDependencies();
                if (dependencies == null) {
                    dependencies = new HashSet<>();
                    archiveRoot.getArchive().setDependencies(dependencies);
                }

                // Normative imports are automatically injected and supposed to be accessible, no specific validation is performed here.
                ToscaContext.get().addDependency(dependency);
                dependencies.add(dependency);
                recursiveCall.remove();
            }
        }
        return toscaDefinitionVersion;
    }
}
