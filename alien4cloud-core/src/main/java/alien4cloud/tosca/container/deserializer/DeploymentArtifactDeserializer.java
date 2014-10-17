package alien4cloud.tosca.container.deserializer;

import java.io.IOException;

import alien4cloud.tosca.container.model.template.DeploymentArtifact;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Specific de-serializer that can de-serialize artifacts based on their extentions.
 * 
 * @author luc boutier
 */
public class DeploymentArtifactDeserializer extends StdDeserializer<DeploymentArtifact> {
    private static final long serialVersionUID = 1L;

    protected DeploymentArtifactDeserializer() {
        super(DeploymentArtifact.class);
    }

    @Override
    public DeploymentArtifact deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        TreeNode node = mapper.readTree(jp);
        if (node.isValueNode()) {
            return ArtifactDeserializerHelper.deserializeArtifact(mapper, node, DeploymentArtifact.class);
        }
        return ComplexTypeDeserializerHelper.deserializeComplexObject(mapper, node, DeploymentArtifact.class);
    }
}