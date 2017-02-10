package alien4cloud.utils;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import alien4cloud.rest.utils.RestMapper;
import alien4cloud.utils.model.Address;
import alien4cloud.utils.model.Person;

/**
 * Unit test to check parsing of json patch.
 */
public class JsonPatchDeserializerTest {

    @Test
    public void deserializeIgnorePatch() throws IOException {
        RestMapper.REQUEST_OPERATION.set("PATCH");
        ObjectMapper objectMapper = new RestMapper();
        Person person = objectMapper.readValue("{ \"name\": \"name\" }", Person.class);
        Assert.assertEquals("name", person.getName());
        Assert.assertNull(person.getAddress());
        RestMapper.REQUEST_OPERATION.remove();
    }

    @Test
    public void deserializeSetNullPatch() throws IOException {
        RestMapper.REQUEST_OPERATION.set("PATCH");
        ObjectMapper objectMapper = new RestMapper();
        Person person = objectMapper.readValue("{ \"name\": \"name\", \"address\": null }", Person.class);
        Assert.assertEquals("name", person.getName());
        Assert.assertNotNull(person.getAddress());
        Assert.assertTrue(person.getAddress() == RestMapper.NULL_INSTANCES.get(Address.class));
        RestMapper.REQUEST_OPERATION.remove();
    }
}
