package alien4cloud.ui.form;

import java.util.Map;

import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.types.AbstractToscaType;
import alien4cloud.dao.ElasticSearchDAO;
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
        if (AbstractToscaType.class.isAssignableFrom(fromClass)) {
            index = ElasticSearchDAO.TOSCA_ELEMENT_INDEX;
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