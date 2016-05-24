package alien4cloud.json.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class TextDeserializer extends StdDeserializer<String> {

    private static final long serialVersionUID = 8412937979915808796L;

    public TextDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return jp.getText();
    }
}