package org.alien4cloud.tosca.model.definitions;

import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.google.common.collect.Sets;

@Getter
@Setter
@EqualsAndHashCode(of = "name")
@NoArgsConstructor
public class OperationOutput {
    String name;
    /** formated attributes names (nodeId:attrbuteName) referencing this output */
    Set<String> relatedAttributes = Sets.newHashSet();

    public OperationOutput(String name) {
        this.name = name;
    }
}
