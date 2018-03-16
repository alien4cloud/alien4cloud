package alien4cloud.tosca.serializer;

import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * A {@code ToscaImportsUtils} is a helper class that generates TOSCA imports.
 *
 * @author Loic Albertin
 */
public class ToscaImportsUtils {

    public static String generateImports(Set<CSARDependency> dependencies) {
        StringBuilder sb = new StringBuilder();
        safe(dependencies).forEach(d -> {
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
