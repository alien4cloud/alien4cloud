package org.alien4cloud.tosca.model.templates;

import java.util.Map;

import lombok.NoArgsConstructor;
import alien4cloud.tosca.parser.impl.advanced.GroupPolicyParser;

@NoArgsConstructor
public class HaPolicy extends AbstractPolicy {

    public static final String HA_POLICY = "tosca.policy.ha";

    public HaPolicy(Map<String, Object> nodeMap) {
        setName((String) nodeMap.get(GroupPolicyParser.NAME));
        // is there any other data in nodeMap we care about?
    }

    @Override
    public String getType() {
        return HA_POLICY;
    }
    
    @Override
    public void setType(String type) {
        // for json serialization
    }
}