package org.alien4cloud.tosca.model.templates;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractPolicy {

    private String name;

    public abstract String getType();

    // needed for JSON deserialization ?
    public abstract void setType(String type);

}
