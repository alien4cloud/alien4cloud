package alien4cloud.variable;

import alien4cloud.rest.utils.JsonUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import java.util.Map;

/**
 * PropertyResolver supports only String type.
 * <p>
 * This class workaround this limitation by serializing {@link Map} object into JSON during property resolution then
 * de-serialized before returning the value.
 */
class PropertySourcesMapResolver extends PropertySourcesPropertyResolver {

    private static final String JSON_MARKER = "__JSON-MAP__";
    private PropertySources propertySources;

    public PropertySourcesMapResolver(PropertySources propertySources) {
        super(propertySources);
        this.propertySources = propertySources;

        getConversionService().addConverter(new Converter<Map, String>() {
            @Override
            @SneakyThrows
            public String convert(Map source) {
                return JSON_MARKER + JsonUtil.toString(source) + JSON_MARKER;
            }
        });
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private void resolvePlaceholderInMap(Map<Object, Object> map) {
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof String) {
                String resolved = resolveNestedPlaceholders((String) entry.getValue());
                Object newValue = resolved;
                if (resolved.contains(JSON_MARKER)) {
                    String json = StringUtils.substringBetween(resolved, JSON_MARKER);
                    newValue = JsonUtil.toMap(json);
                }
                entry.setValue(newValue);
            } else if (entry.getValue() instanceof Map) {
                resolvePlaceholderInMap((Map) entry.getValue());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
        if (this.propertySources != null) {
            for (PropertySource<?> propertySource : this.propertySources) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("Searching for key '%s' in [%s]", key, propertySource.getName()));
                }
                Object value = propertySource.getProperty(key);
                if (value != null) {
                    if (resolveNestedPlaceholders && value instanceof String) {
                        value = resolveNestedPlaceholders((String) value);
                    } else if (value instanceof Map) {
                        resolvePlaceholderInMap((Map) value);
                    }
                    logKeyFound(key, propertySource, value);
                    return this.conversionService.convert(value, targetValueType);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Could not find key '%s' in any property source", key));
        }
        return null;
    }

}
