package org.alien4cloud.tosca.model.templates;

import alien4cloud.tosca.parser.impl.advanced.GroupPolicyParser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/** For any unknown policy, let's record its data; maybe an external system can make use of it. */
@Getter
@Setter
@NoArgsConstructor
public class GenericPolicy extends AbstractPolicy {

    public GenericPolicy(Map<String,?> parsedData) {
        setName((String)parsedData.get(GroupPolicyParser.NAME));
        setType((String)parsedData.get(GroupPolicyParser.TYPE));
        data = parsedData;
    }
    
    private String type;
    private Map<String, ?> data;

}
