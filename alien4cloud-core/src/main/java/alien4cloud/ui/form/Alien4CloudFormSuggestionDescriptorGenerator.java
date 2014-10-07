package alien4cloud.ui.form;

import java.util.Map;

import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.ui.form.exception.FormDescriptorGenerationException;

import com.google.common.collect.Maps;

@Component
public class Alien4CloudFormSuggestionDescriptorGenerator implements FormSuggestionDescriptorGenerator {

    private static final String TYPE_KEY = "_type";
    private static final String PATH_KEY = "_path";
    private static final String INDEX_KEY = "_index";

    @Override
    public Map<String, Object> generateSuggestionDescriptor(Class<?> fromClass, String path) {
        String index;
        if (IndexedToscaElement.class.isAssignableFrom(fromClass)) {
            index = ToscaElement.class.getSimpleName().toLowerCase();
        } else {
            throw new FormDescriptorGenerationException("Unsupported suggestion fromClass [" + fromClass.getName() + "]");
        }
        String type = MappingBuilder.indexTypeFromClass(fromClass);
        Map<String, Object> suggestionMetaData = Maps.newHashMap();
        suggestionMetaData.put(INDEX_KEY, index);
        suggestionMetaData.put(TYPE_KEY, type);
        suggestionMetaData.put(PATH_KEY, path);
        return suggestionMetaData;
    }

}