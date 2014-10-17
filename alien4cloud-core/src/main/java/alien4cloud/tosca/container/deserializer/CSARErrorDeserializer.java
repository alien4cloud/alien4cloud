package alien4cloud.tosca.container.deserializer;

import java.io.IOException;
import java.util.Set;

import alien4cloud.tosca.container.validation.CSARError;
import alien4cloud.tosca.container.validation.CSARErrorCode;
import alien4cloud.tosca.container.validation.CSARValidationError;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.Sets;

public class CSARErrorDeserializer extends StdDeserializer<Set<CSARError>> {

    private static final long serialVersionUID = -2024770253276442456L;

    protected CSARErrorDeserializer() {
        super(Set.class);
    }

    @Override
    public Set<CSARError> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        TreeNode node = jp.readValueAsTree();
        if (node.isArray()) {
            int errorsSize = node.size();
            Set<CSARError> errors = Sets.newHashSet();
            for (int i = 0; i < errorsSize; i++) {
                TreeNode child = node.get(i);
                String typeAsString = mapper.treeToValue(child.get("errorCode"), String.class);
                CSARErrorCode type = CSARErrorCode.fromErrorCode(typeAsString);
                if (type != null) {
                    errors.add((CSARError) mapper.treeToValue(child, type.getCorrespondedErrorClass()));
                } else {
                    errors.add(mapper.treeToValue(child, CSARValidationError.class));
                }
            }
            return errors;
        } else {
            throw JsonMappingException.from(jp, "Encounter non array type for a set of csar error, the json is invalid");
        }
    }
}