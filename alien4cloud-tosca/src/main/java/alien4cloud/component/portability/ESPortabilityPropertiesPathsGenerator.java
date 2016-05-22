package alien4cloud.component.portability;

import com.google.common.collect.Lists;
import java.util.List;
import org.elasticsearch.annotation.query.IPathGenerator;

/***
 * Generate paths to uses when working with facets or terms filter with portability map
 * 
 * Note that this class will not take into account the value of the attribute "paths"
 * 
 * @author igor
 *
 */
public class ESPortabilityPropertiesPathsGenerator implements IPathGenerator {

    @Override
    public String[] getPaths(String[] annotationPaths) {
        List<String> paths = Lists.newArrayList();
        for (PortabilityPropertyEnum property : PortabilityPropertyEnum.values()) {
            paths.add(property.toString() + ".value");
        }
        return paths.toArray(new String[paths.size()]);
    }
}
