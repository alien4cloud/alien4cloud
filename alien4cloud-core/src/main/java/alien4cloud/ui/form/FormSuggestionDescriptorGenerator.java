package alien4cloud.ui.form;

import java.util.Map;

public interface FormSuggestionDescriptorGenerator {

    Map<String, Object> generateSuggestionDescriptor(Class<?> fromClass, String path);
}
