package org.alien4cloud.tosca.model.types;

import static alien4cloud.dao.model.FetchContext.QUICK_SEARCH;
import static alien4cloud.dao.model.FetchContext.SUMMARY;
import static alien4cloud.dao.model.FetchContext.TAG_SUGGESTION;

import java.util.Map;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.Interface;
import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.query.FetchContext;

import alien4cloud.json.deserializer.AttributeDeserializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class AbstractInstantiableToscaType extends AbstractInheritableToscaType {
    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, DeploymentArtifact> artifacts;

    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    @JsonDeserialize(contentUsing = AttributeDeserializer.class)
    private Map<String, IValue> attributes;

    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, Interface> interfaces;
}