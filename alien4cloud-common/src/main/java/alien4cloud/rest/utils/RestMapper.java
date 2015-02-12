package alien4cloud.rest.utils;

import alien4cloud.utils.jackson.ConditionalAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Object mapper configured with REST options to serialize/deserialize maps as array.
 */
public class RestMapper extends ObjectMapper {
    private static final long serialVersionUID = 1L;

    public RestMapper() {
        super();
        this._serializationConfig = this._serializationConfig.withAttribute(ConditionalAttributes.REST, "true");
        this._deserializationConfig = this._deserializationConfig.withAttribute(ConditionalAttributes.REST, "true");
    }
}