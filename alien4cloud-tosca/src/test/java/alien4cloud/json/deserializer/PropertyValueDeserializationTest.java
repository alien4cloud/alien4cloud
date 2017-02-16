package alien4cloud.json.deserializer;

import java.io.IOException;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alien4cloud.rest.utils.RestMapper;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by lucboutier on 08/02/2017.
 */
public class PropertyValueDeserializationTest {
    @Test
    public void testPatchDeserialization() throws IOException {
        RestMapper.REQUEST_OPERATION.set("PATCH");
        ObjectMapper objectMapper = new RestMapper();
        Simple simple = objectMapper.readValue("{ \"id\": \"the_id_value\", \"propertyValue\": null }", Simple.class);
        Assert.assertEquals("the_id_value", simple.getId());
        Assert.assertNotNull(simple.getPropertyValue());
        Assert.assertTrue(simple.getPropertyValue() == RestMapper.NULL_INSTANCES.get(simple.getPropertyValue().getClass()));
        RestMapper.REQUEST_OPERATION.remove();
    }

    @Getter
    @Setter
    public static class Simple {
        private String id;
        @JsonDeserialize(using = PropertyValueDeserializer.class)
        private AbstractPropertyValue propertyValue;
    }
}
