package org.alien4cloud.tosca.model.templates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubstitutionTarget {
    private String nodeTemplateName;
    private String targetId;
    private String serviceRelationshipType;

    public SubstitutionTarget(String nodeTemplateName, String targetId) {
        this.nodeTemplateName = nodeTemplateName;
        this.targetId = targetId;
    }
}