package alien4cloud.tosca.parser.impl.advanced;

import java.util.HashSet;
import java.util.Set;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.*;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import org.alien4cloud.tosca.model.CSARDependency;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.ToscaNormativeImports;

@Component
public class ToscaDefinitionVersionParser implements INodeParser<String> {
    @Override
    public String parse(Node node, ParsingContextExecution context) {
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getParent();
        String toscaDefinitionVersion = ParserUtils.getScalar(node, context);
        if (toscaDefinitionVersion != null) {
            CSARDependency dependency = ToscaNormativeImports.IMPORTS.get(toscaDefinitionVersion);
            if (dependency != null) {
                Set<CSARDependency> dependencies = archiveRoot.getArchive().getDependencies();
                if (dependencies == null) {
                    dependencies = new HashSet<>();
                    archiveRoot.getArchive().setDependencies(dependencies);
                }

                // Normative imports are automatically injected and supposed to be accessible, no specific validation is performed here.
                ToscaContext.get().addDependency(dependency);
                dependencies.add(dependency);
            }
        }
        return toscaDefinitionVersion;
    }
}
