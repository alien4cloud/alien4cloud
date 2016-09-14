package alien4cloud.common;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;

import alien4cloud.model.common.SimpleSuggestionEntry;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;

@Getter
@Setter
public class ParsedPropertiesDefinitions {
    private Map<String, PropertyDefinition> definitions = Maps.newHashMap();
    private Map<String, List<String>> policies = Maps.newHashMap();
    private List<SimpleSuggestionEntry> suggestions = Lists.newArrayList();
}
