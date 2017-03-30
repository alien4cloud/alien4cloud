package alien4cloud.rest.utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Maps;

import alien4cloud.utils.jackson.ConditionalAttributes;

/**
 * Object mapper configured with REST options to serialize/deserialize maps as array.
 */
public class RestMapper extends ObjectMapper {
    private static final long serialVersionUID = 1L;

    public static final Map<Class, Object> NULL_INSTANCES = Maps.newHashMap();
    public static final ThreadLocal<String> REQUEST_OPERATION = new ThreadLocal<>();
    public static final String PATCH = "PATCH";

    static {
        NULL_INSTANCES.put(String.class, "null");
    }

    public RestMapper() {
        super();
        this._serializationConfig = this._serializationConfig.withAttribute(ConditionalAttributes.REST, "true");
        this._deserializationConfig = this._deserializationConfig.withAttribute(ConditionalAttributes.REST, "true");

        this.registerModule(new DeserializerModule(new PatchBeanDeserializerModifier()));
    }

    private static final class DeserializerModule extends SimpleModule {
        private BeanDeserializerModifier deserializerModifier;

        public DeserializerModule(BeanDeserializerModifier deserializerModifier) {
            super("DeserializerModule", Version.unknownVersion());
            this.deserializerModifier = deserializerModifier;
        }

        @Override
        public void setupModule(SetupContext context) {
            super.setupModule(context);
            context.addBeanDeserializerModifier(deserializerModifier);
        }
    }

    private static class PatchBeanDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
            if (BeanDeserializer.class.equals(deserializer.getClass())) {
                return new NullBeanDeserializer((BeanDeserializer) deserializer);
            } else if (deserializer instanceof StdScalarDeserializer) {
                return new NullWrapperDeserializer((StdScalarDeserializer) deserializer);
            }
            // this deserializer does not support nullable value (arrays for example).
            return deserializer;
        }
    }

    private static class NullBeanDeserializer extends BeanDeserializer {
        public NullBeanDeserializer(BeanDeserializerBase src) {
            super(src);
        }

        @Override
        public Object getNullValue(DeserializationContext ctxt) throws JsonMappingException {
            if (PATCH.equals(RestMapper.REQUEST_OPERATION.get())) {
                try {
                    Object instance = NULL_INSTANCES.get(handledType());
                    if (instance == null) {
                        instance = _valueInstantiator.createUsingDefault(ctxt);
                    }
                    NULL_INSTANCES.put(handledType(), instance);
                    return instance;
                } catch (IOException e) {
                }
            }
            return getNullValue();
        }
    }

    private static class NullWrapperDeserializer extends StdScalarDeserializer {
        private StdScalarDeserializer wrapped;

        public NullWrapperDeserializer(StdScalarDeserializer wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        @Override
        public boolean isCachable() {
            return wrapped.isCachable();
        }

        @Override
        public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            return wrapped.deserialize(jsonParser, deserializationContext);
        }

        @Override
        public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
            return wrapped.deserializeWithType(jp, ctxt, typeDeserializer);
        }

        @Override
        public Object getNullValue(DeserializationContext ctxt) throws JsonMappingException {
            if (PATCH.equals(RestMapper.REQUEST_OPERATION.get())) {
                try {
                    Object instance = NULL_INSTANCES.get(handledType());
                    if (instance == null) {
                        instance = handledType().getConstructor().newInstance();
                    }
                    NULL_INSTANCES.put(handledType(), instance);
                    return instance;
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                }
            }
            return wrapped.getNullValue();
        }
    }
}