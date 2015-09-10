package alien4cloud.model.topology;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.tosca.parser.impl.advanced.GroupPolicyParser;

/** For any unknown policy, let's record its data; maybe an external system can make use of it. */
@Getter
@Setter
public class GenericPolicy extends AbstractPolicy {

    public GenericPolicy(Map<String,?> parsedData) {
        setName((String)parsedData.get(GroupPolicyParser.NAME));
        setType((String)parsedData.get(GroupPolicyParser.TYPE));
        data = parsedData;
    }
    
    private String type;
    private Map<String, ?> data;

}
