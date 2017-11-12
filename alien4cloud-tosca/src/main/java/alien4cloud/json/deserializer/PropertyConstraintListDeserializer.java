package alien4cloud.json.deserializer;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.List;

import org.alien4cloud.tosca.model.definitions.PropertyConstraint;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.Lists;

public class PropertyConstraintListDeserializer extends StdDeserializer<List<PropertyConstraint>> {
    private final PropertyConstraintDeserializer propertyConstraintDeserializer;

    public PropertyConstraintListDeserializer() throws IntrospectionException, IOException, ClassNotFoundException {
        super(List.class);
        propertyConstraintDeserializer = new PropertyConstraintDeserializer();
    }

    @Override
    public List<PropertyConstraint> deserialize(JsonParser p, DeserializationContext ctx) throws IOException, JsonProcessingException {
        JsonToken t;
        List<PropertyConstraint> list = Lists.newArrayList();
        while ((t = p.nextToken()) != JsonToken.END_ARRAY) {
            PropertyConstraint pc;
            if (t != JsonToken.VALUE_NULL) {
                pc = propertyConstraintDeserializer.deserialize(p, ctx);
                list.add(pc);
            }
        }
        return list;
    }
}
