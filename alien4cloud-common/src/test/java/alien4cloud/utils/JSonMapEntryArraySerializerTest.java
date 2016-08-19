package alien4cloud.utils;

import java.io.IOException;
import java.util.Map;

import lombok.*;

import org.elasticsearch.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;

import alien4cloud.utils.jackson.ConditionalAttributes;
import alien4cloud.utils.jackson.ConditionalOnAttribute;
import alien4cloud.utils.jackson.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.jackson.JSonMapEntryArraySerializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class JSonMapEntryArraySerializerTest {

    @Test
    public void serializationWithDefaultContextShouldNotSerializeToArray() throws JsonProcessingException {
        TestObject testObject = new TestObject();
        testObject.map.put("key1", new InnerObject("innerStr1", 1));
        ObjectMapper mapper = new ObjectMapper();
        String str = mapper.writeValueAsString(testObject);

        Assert.assertEquals("{\"map\":{\"key1\":{\"str\":\"innerStr1\",\"integer\":1}}}", str);
    }

    @Test
    public void deserializationWithDefaultContext() throws IOException {
        TestObject testObject = new TestObject();
        testObject.map.put("key1", new InnerObject("innerStr1", 1));
        ObjectMapper mapper = new ObjectMapper();
        String str = mapper.writeValueAsString(testObject);
        TestObject readObject = mapper.readValue(str, TestObject.class);

        Assert.assertEquals(testObject, readObject);
    }

    @Test
    public void serializationWithMapContextShouldSerializeToArray() throws JsonProcessingException {
        TestObject testObject = new TestObject();
        testObject.map.put("key1", new InnerObject("innerStr1", 1));
        MapRestMapper mapper = new MapRestMapper();
        String str = mapper.writeValueAsString(testObject);
        Assert.assertEquals("{\"map\":[{\"key\":\"key1\",\"value\":{\"str\":\"innerStr1\",\"integer\":1}}]}", str);
    }

    @Test
    public void deserializationWithMapContext() throws IOException {
        TestObject testObject = new TestObject();
        testObject.map.put("key1", new InnerObject("innerStr1", 1));
        MapRestMapper mapper = new MapRestMapper();
        String str = mapper.writeValueAsString(testObject);
        TestObject readObject = mapper.readValue(str, TestObject.class);

        Assert.assertEquals(testObject, readObject);
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    public static class TestObject {
        @ConditionalOnAttribute(ConditionalAttributes.REST)
        @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
        @JsonSerialize(using = JSonMapEntryArraySerializer.class)
        private Map<String, InnerObject> map = Maps.newHashMap();
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor(suppressConstructorProperties = true)
    public static class InnerObject {
        private String str;
        private Integer integer;
    }

    public static class MapRestMapper extends ObjectMapper {
        private static final long serialVersionUID = 1L;

        public MapRestMapper() {
            super();
            this._serializationConfig = this._serializationConfig.withAttribute(ConditionalAttributes.REST, "true");
            this._deserializationConfig = this._deserializationConfig.withAttribute(ConditionalAttributes.REST, "true");
        }
    }
}