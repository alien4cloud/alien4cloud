package alien4cloud.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import alien4cloud.topology.task.AbstractRelationshipTask;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.topology.task.ArtifactTask;
import alien4cloud.topology.task.IllegalOperationsTask;
import alien4cloud.topology.task.InputArtifactTask;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.topology.task.LogTask;
import alien4cloud.topology.task.NodeFiltersTask;
import alien4cloud.topology.task.NodeMatchingTask;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.RequirementsTask;
import alien4cloud.topology.task.ScalableTask;
import alien4cloud.topology.task.SuggestionsTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.UnavailableLocationTask;
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
        addToRegistry(NodeMatchingTask.class, TaskCode.NO_NODE_MATCHES);
        addToRegistry(NodeFiltersTask.class, TaskCode.NODE_FILTER_INVALID);
        addToRegistry(RequirementsTask.class, TaskCode.SATISFY_LOWER_BOUND);
        addToRegistry(WorkflowTask.class, TaskCode.WORKFLOW_INVALID);
        addToRegistry(SuggestionsTask.class, TaskCode.REPLACE, TaskCode.IMPLEMENT);
        addToRegistry(ArtifactTask.class, TaskCode.ARTIFACT_INVALID);
        addToRegistry(InputArtifactTask.class, TaskCode.INPUT_ARTIFACT_INVALID);
        addToRegistry(AbstractRelationshipTask.class, TaskCode.IMPLEMENT_RELATIONSHIP);
        addToRegistry(IllegalOperationsTask.class, TaskCode.FORBIDDEN_OPERATION);
        addToRegistry(LogTask.class, TaskCode.LOG);
        addToRegistry(UnavailableLocationTask.class, TaskCode.LOCATION_DISABLED, TaskCode.LOCATION_UNAUTHORIZED);
    }

    @Override
    protected AbstractTask deserializeAfterRead(JsonParser jp, DeserializationContext ctxt, ObjectMapper mapper, ObjectNode root)
            throws JsonProcessingException {
        AbstractTask result = super.deserializeAfterRead(jp, ctxt, mapper, root);

        if (result == null) {
            result = mapper.treeToValue(root, LogTask.class);
        }

        return result;
    }

    private void addToRegistry(Class<? extends AbstractTask> clazz, TaskCode... taskCodes) {
        for (TaskCode taskCode : taskCodes) {
            addToRegistry(taskCode.toString(), clazz);
        }
    }
}