package alien4cloud.tosca.container.deserializer;

import java.io.IOException;
import java.util.Map;

import alien4cloud.tosca.container.model.type.ImplementationArtifact;
import alien4cloud.tosca.container.model.type.Operation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.Maps;

public class OperationDeserializer extends StdDeserializer<Operation> {

    private static final long serialVersionUID = -7699372202765948774L;

    private Map<String, String> extensionsToArtifactTypesMapping;

    protected OperationDeserializer() {
        super(Operation.class);
        // TODO complete with all known artifact types
        extensionsToArtifactTypesMapping = Maps.newHashMap();
        extensionsToArtifactTypesMapping.put("sh", "tosca.artifacts.ShellScript");
        extensionsToArtifactTypesMapping.put("war", "tosca.artifacts.WarFile");
    }

    @Override
    public Operation deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        TreeNode node = mapper.readTree(jp);
        if (node.isValueNode()) {
            Operation operation = new Operation();
            operation.setImplementationArtifact(ArtifactDeserializerHelper.deserializeArtifact(mapper, node, ImplementationArtifact.class));
            return operation;
        }
        return ComplexTypeDeserializerHelper.deserializeComplexObject(mapper, node, Operation.class);
    }
}