package alien4cloud.tosca.serializer;

import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;

/**
 * A {@code ToscaImportsUtils} is a helper class that generates TOSCA imports.
 *
 * @author Loic Albertin
 */
public class ToscaImportsUtils {

    public static String generateImports(Set<CSARDependency> dependencies) {
        StringBuilder sb = new StringBuilder();
        dependencies.forEach(d -> {
            if (sb.length() != 0) {
                sb.append("\n");
            }
            sb.append("  - ");
            sb.append(d.getName());
            sb.append(":");
            sb.append(d.getVersion());
        });
        return sb.toString();
    }
}
