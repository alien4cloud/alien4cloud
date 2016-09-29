package org.alien4cloud.tosca.model.types;

import static alien4cloud.dao.model.FetchContext.*;

import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.elasticsearch.annotation.MapKeyValue;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermsFacet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
public class AbstractInheritableToscaType extends AbstractToscaType {
    @TermsFacet
    private boolean isAbstract;

    @FetchContext(contexts = { QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false })
    @TermsFacet
    private List<String> derivedFrom;

    @MapKeyValue
    @ConditionalOnAttribute(value = { ConditionalAttributes.REST, ConditionalAttributes.ES_1_2 })
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { SUMMARY, QUICK_SEARCH, TAG_SUGGESTION }, include = { false, false, false })
    private Map<String, PropertyDefinition> properties;
}