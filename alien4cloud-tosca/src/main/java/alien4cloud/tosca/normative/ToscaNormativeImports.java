package alien4cloud.tosca.normative;

import org.alien4cloud.tosca.model.CSARDependency;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * The definition of the TOSCA version implies an automatic import of dependencies.
 *
 * This class contains the CSARDependency definition based on the actual import definition.
 */
public final class ToscaNormativeImports {
    public static final Map<String, CSARDependency> IMPORTS;

    static {
        IMPORTS = Maps.newHashMap();
        IMPORTS.put("tosca_simple_yaml_1_0", new CSARDependency("tosca-normative-types", "1.0.0"));
        IMPORTS.put("http://docs.oasis-open.org/tosca/ns/simple/yaml/1.0", new CSARDependency("tosca-normative-types", "1.0.0"));
    }
}
