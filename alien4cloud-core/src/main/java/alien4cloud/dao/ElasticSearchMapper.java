package alien4cloud.dao;

import alien4cloud.json.serializer.BoundSerializer;
import alien4cloud.utils.jackson.ConditionalAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ElasticSearchMapper extends ObjectMapper {

    private static final long serialVersionUID = 1L;

    public ElasticSearchMapper() {
        super();
        this._serializationConfig = this._serializationConfig.withAttribute(BoundSerializer.BOUND_SERIALIZER_AS_NUMBER, "true");
        this._serializationConfig = this._serializationConfig.withAttribute(ConditionalAttributes.ES, "true");
        this._deserializationConfig = this._deserializationConfig.withAttribute(ConditionalAttributes.ES, "true");
    }
}
