package org.alien4cloud.tosca.model.types;

import static alien4cloud.dao.model.FetchContext.*;

import java.util.Map;

import org.alien4cloud.tosca.model.definitions.IValue;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermsFacet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alien4cloud.json.deserializer.AttributeDeserializer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ESObject
public class CapabilityType extends AbstractInheritableToscaType {
    @ObjectField(enabled = false)
    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    @JsonDeserialize(contentUsing = AttributeDeserializer.class)
    private Map<String, IValue> attributes;
    /**
     * An optional list of one or more valid names of Node Types that are supported as valid sources of any relationship established to the declared Capability
     * Type.
     */
    @TermsFacet
    private String[] validSources;
}