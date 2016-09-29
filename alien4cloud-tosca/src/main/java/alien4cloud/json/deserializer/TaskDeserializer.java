package alien4cloud.json.deserializer;

import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import alien4cloud.topology.task.AbstractRelationshipTask;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.topology.task.ArtifactTask;
import alien4cloud.topology.task.InputArtifactTask;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.topology.task.NodeFiltersTask;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.RequirementsTask;
import alien4cloud.topology.task.ScalableTask;
import alien4cloud.topology.task.SuggestionsTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TopologyTask;
import alien4cloud.topology.task.WorkflowTask;

/**
 * Custom deserializer to handle multiple {@link AbstractTask} types.
 */
public class TaskDeserializer extends AbstractFieldValueDiscriminatorPolymorphicDeserializer<AbstractTask> {

    public TaskDeserializer() {
        super("code", AbstractTask.class);
        addToRegistry(PropertiesTask.class, TaskCode.INPUT_PROPERTY, TaskCode.PROPERTIES, TaskCode.ORCHESTRATOR_PROPERTY);
        addToRegistry(ScalableTask.class, TaskCode.SCALABLE_CAPABILITY_INVALID);
        addToRegistry(LocationPolicyTask.class, TaskCode.LOCATION_POLICY);
        addToRegistry(NodeFiltersTask.class, TaskCode.NODE_FILTER_INVALID);
        addToRegistry(RequirementsTask.class, TaskCode.SATISFY_LOWER_BOUND);
        addToRegistry(WorkflowTask.class, TaskCode.WORKFLOW_INVALID);
        addToRegistry(SuggestionsTask.class, TaskCode.REPLACE, TaskCode.IMPLEMENT);
        addToRegistry(ArtifactTask.class, TaskCode.ARTIFACT_INVALID);
        addToRegistry(InputArtifactTask.class, TaskCode.INPUT_ARTIFACT_INVALID);
        addToRegistry(AbstractRelationshipTask.class, TaskCode.IMPLEMENT_RELATIONSHIP);
    }

    @Override
    protected AbstractTask deserializeAfterRead(JsonParser jp, DeserializationContext ctxt, ObjectMapper mapper, ObjectNode root)
            throws JsonProcessingException {
        AbstractTask result = super.deserializeAfterRead(jp, ctxt, mapper, root);

        if (result == null) {
            Map data = mapper.treeToValue(root, Map.class);
            if (data.containsKey("nodeTemplateName")) {
                result = mapper.treeToValue(root, TopologyTask.class);
            } else {
                failedToFindImplementation(jp, root);
            }
        }

        return result;
    }

    private void addToRegistry(Class<? extends AbstractTask> clazz, TaskCode... taskCodes) {
        for (TaskCode taskCode : taskCodes) {
            addToRegistry(taskCode.toString(), clazz);
        }
    }
}