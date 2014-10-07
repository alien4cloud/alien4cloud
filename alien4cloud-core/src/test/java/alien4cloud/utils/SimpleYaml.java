package alien4cloud.utils;

import lombok.Getter;
import lombok.Setter;

/**
 * Object for testing yaml parser. See test resources validfile.yaml and invalidfile.yaml.
 * 
 * @author luc boutier
 */
@Getter
@Setter
public class SimpleYaml {
    private String id;
    private String value;
    private InnerYaml innerYaml;

    @Getter
    @Setter
    public class InnerYaml {
        String id;
        String toto;
    }
}