package alien4cloud.dao;

import java.beans.IntrospectionException;
import java.io.IOException;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import alien4cloud.json.deserializer.PropertyConstraintDeserializer;
import alien4cloud.json.serializer.BoundSerializer;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import alien4cloud.utils.jackson.ConditionalAttributes;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElasticSearchMapper extends ObjectMapper {
    private static final long serialVersionUID = 1L;

    private ElasticSearchMapper() {
        super();
        this._serializationConfig = this._serializationConfig.withAttribute(BoundSerializer.BOUND_SERIALIZER_AS_NUMBER, "true");
        this._serializationConfig = this._serializationConfig.withAttribute(ConditionalAttributes.ES, "true");
        this._serializationConfig = this._serializationConfig.withAttribute(ConditionalAttributes.ES_1_2, "true");
        this._deserializationConfig = this._deserializationConfig.withAttribute(ConditionalAttributes.ES, "true");
        this._deserializationConfig = this._deserializationConfig.withAttribute(ConditionalAttributes.ES_1_2, "true");
    }

    public static ElasticSearchMapper getInstance() {
        ElasticSearchMapper elasticSearchMapper = new ElasticSearchMapper();
        SimpleModule module = new SimpleModule("PropDeser", new Version(1, 0, 0, null, null, null));
        try {
            module.addDeserializer(PropertyConstraint.class, new PropertyConstraintDeserializer());
        } catch (ClassNotFoundException | IOException | IntrospectionException e) {
            log.warn("The property constraint deserialialisation failed");
        }
        elasticSearchMapper.registerModule(module);
        return elasticSearchMapper;
    }
}
