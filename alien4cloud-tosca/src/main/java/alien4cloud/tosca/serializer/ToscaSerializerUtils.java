package alien4cloud.tosca.serializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.definitions.AbstractArtifact;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.AbstractPropertyConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.InRangeConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MaxLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MinLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.PatternConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.ValidValuesConstraint;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.CallOperationWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.DelegateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

/**
 * Tools for serializing in YAML/TOSCA. ALl methods should be static but did not found how to use statics from velocity.
 */
public class ToscaSerializerUtils {

    public boolean collectionIsNotEmpty(Collection<?> c) {
        return c != null && !c.isEmpty();
    }

    public boolean mapIsNotEmpty(Map<?, ?> m) {
        return m != null && !m.isEmpty();
    }

    /**
     * Render a description. If the string contain CRLF, then render a multiline literal preserving indentation.
     */
    public String renderDescription(String description, String identation) throws IOException {
        if (description != null && description.contains("\n")) {
            BufferedReader br = new BufferedReader(new StringReader(description));
            StringWriter sw = new StringWriter();
            sw.write("|");
            sw.write("\n");
            String line = br.readLine();
            boolean isFirst = true;
            while (line != null) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sw.write("\n");
                }
                sw.write(identation);
                sw.write(line);
                line = br.readLine();
            }
            return sw.toString();
        } else {
            return "\"" + ToscaPropertySerializerUtils.escapeDoubleQuote(description) + "\"";
        }
    }

    /**
     * Check if the map is not null, not empty and contains at least one not null value.
     * This function is recursive:
     * <ul>
     * <li>if a map entry is a also a map, then we'll look for non null values in it (recursively).
     * <li>if a map entry is a collection, then will return true if the collection is not empty.
     * <li>if a map entry is a ScalarPropertyValue, then will return true if the value is not null.
     * </ul>
     */
    public boolean mapIsNotEmptyAndContainsNotnullValues(Map<?, ?> m) {
        if (mapIsNotEmpty(m)) {
            for (Object o : m.values()) {
                if (o != null) {
                    if (o instanceof Map<?, ?>) {
                        if (mapIsNotEmptyAndContainsNotnullValues((Map<?, ?>) o)) {
                            return true;
                        }
                    } else if (o instanceof Collection<?>) {
                        if (!((Collection<?>) o).isEmpty()) {
                            return true;
                        }
                    } else if (o instanceof ScalarPropertyValue) {
                        if (((ScalarPropertyValue) o).getValue() != null) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String getCsvToString(Collection<?> list) {
        return getCsvToString(list, false);
    }

    public static String getCsvToString(Collection<?> list, boolean renderScalar) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        if (list != null) {
            for (Object o : list) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sb.append(", ");
                }
                if (renderScalar) {
                    sb.append(ToscaPropertySerializerUtils.renderScalar(o.toString()));
                } else {
                    sb.append(o.toString());
                }
            }
        }
        return sb.toString();
    }

    public boolean hasCapabilitiesContainingNotNullProperties(NodeTemplate nodeTemplate) {
        Map<String, Capability> capabilities = nodeTemplate.getCapabilities();
        if (capabilities == null || capabilities.isEmpty()) {
            return false;
        }
        for (Capability capability : capabilities.values()) {
            if (capability == null) {
                continue;
            }
            if (mapIsNotEmptyAndContainsNotnullValues(capability.getProperties())) {
                return true;
            }
        }
        return false;
    }

    public boolean doesInterfacesContainsImplementedOperation(Map<String, Interface> interfaces) {
        if (interfaces == null || interfaces.isEmpty()) {
            return false;
        }
        for (Interface interfaze : interfaces.values()) {
            if (doesInterfaceContainsImplementedOperation(interfaze)) {
                return true;
            }
        }
        return false;
    }

    public boolean doesInterfaceContainsImplementedOperation(Interface interfaze) {
        if (interfaze == null) {
            return false;
        }
        Map<String, Operation> operations = interfaze.getOperations();
        if (operations == null || operations.isEmpty()) {
            return false;
        }
        for (Operation operation : operations.values()) {
            if (isOperationImplemented(operation)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOperationImplemented(Operation operation) {
        if (operation == null) {
            return false;
        }
        if (operation.getImplementationArtifact() != null) {
            return true;
        }
        return false;
    }

    public String renderConstraint(AbstractPropertyConstraint c) {
        StringBuilder builder = new StringBuilder();
        if (c instanceof GreaterOrEqualConstraint) {
            builder.append("greater_or_equal: ");
            builder.append(ToscaPropertySerializerUtils.renderScalar(((GreaterOrEqualConstraint) c).getGreaterOrEqual()));
        } else if (c instanceof GreaterThanConstraint) {
            builder.append("greater_than: ");
            builder.append(ToscaPropertySerializerUtils.renderScalar(((GreaterThanConstraint) c).getGreaterThan()));
        } else if (c instanceof LessOrEqualConstraint) {
            builder.append("less_or_equal: ");
            builder.append(ToscaPropertySerializerUtils.renderScalar(((LessOrEqualConstraint) c).getLessOrEqual()));
        } else if (c instanceof LessThanConstraint) {
            builder.append("less_than: ");
            builder.append(ToscaPropertySerializerUtils.renderScalar(((LessThanConstraint) c).getLessThan()));
        } else if (c instanceof LengthConstraint) {
            builder.append("length: ");
            builder.append(((LengthConstraint) c).getLength());
        } else if (c instanceof MaxLengthConstraint) {
            builder.append("max_length: ");
            builder.append(((MaxLengthConstraint) c).getMaxLength());
        } else if (c instanceof MinLengthConstraint) {
            builder.append("min_length: ");
            builder.append(((MinLengthConstraint) c).getMinLength());
        } else if (c instanceof PatternConstraint) {
            builder.append("pattern: ");
            builder.append(ToscaPropertySerializerUtils.renderScalar(((PatternConstraint) c).getPattern()));
        } else if (c instanceof EqualConstraint) {
            builder.append("equal: ");
            builder.append(ToscaPropertySerializerUtils.renderScalar(((EqualConstraint) c).getEqual()));
        } else if (c instanceof InRangeConstraint) {
            builder.append("in_range: ");
            builder.append("[");
            builder.append(getCsvToString(((InRangeConstraint) c).getInRange(), true));
            builder.append("]");
        } else if (c instanceof ValidValuesConstraint) {
            builder.append("valid_values: ");
            builder.append("[");
            builder.append(getCsvToString(((ValidValuesConstraint) c).getValidValues(), true));
            builder.append("]");
        }
        return builder.toString();
    }

    public boolean isNodeActivityStep(WorkflowStep abstractStep) {
        return abstractStep instanceof NodeWorkflowStep;
    }

    public String getActivityLabel(AbstractWorkflowActivity activity) {
        if (activity instanceof CallOperationWorkflowActivity) {
            return "call_operation";
        } else if (activity instanceof SetStateWorkflowActivity) {
            return "set_state";
        } else if (activity instanceof DelegateWorkflowActivity) {
            return "delegate";
        } else {
            return activity.getClass().getSimpleName();
        }
    }

    public String getInlineActivityArg(AbstractWorkflowActivity activity) {
        if (activity instanceof CallOperationWorkflowActivity) {
            CallOperationWorkflowActivity callActivity = (CallOperationWorkflowActivity) activity;
            return callActivity.getInterfaceName() + "." + callActivity.getOperationName();
        } else if (activity instanceof SetStateWorkflowActivity) {
            SetStateWorkflowActivity stateActivity = (SetStateWorkflowActivity) activity;
            return stateActivity.getStateName();
        } else if (activity instanceof DelegateWorkflowActivity) {
            DelegateWorkflowActivity delegateWorkflowActivity = (DelegateWorkflowActivity) activity;
            return delegateWorkflowActivity.getDelegate();
        } else {
            return "void";
        }
    }

    public static boolean hasRepositories(String topologyArchiveName, String topologyArchiveVersion, Topology topology) {
        // we don't support node types in Editor context, just check the node templates
        if (topology.getNodeTemplates() != null && CollectionUtils.isNotEmpty(topology.getNodeTemplates().values())) {
            for (NodeTemplate node : topology.getNodeTemplates().values()) {
                if (node.getArtifacts() != null && CollectionUtils.isNotEmpty(node.getArtifacts().values())) {
                    for (DeploymentArtifact artifact : node.getArtifacts().values()) {
                        // Only consider artifact of the topology
                        if ((topologyArchiveName.equals(artifact.getArchiveName()) && topologyArchiveVersion.equals(artifact.getArchiveVersion()))
                                && StringUtils.isNotBlank(artifact.getRepositoryName())) {
                            return true;
                        }
                    }
                }
            }
        }
        return MapUtils.isNotEmpty(topology.getInputArtifacts()) && topology.getInputArtifacts().values().stream()
                .anyMatch(deploymentArtifact -> StringUtils.isNotBlank(deploymentArtifact.getRepositoryName()));
    }

    public static String formatRepositories(String topologyArchiveName, String topologyArchiveVersion, Topology topology) {
        StringBuilder buffer = new StringBuilder();
        Set<String> repositoriesName = Sets.newHashSet();
        for (NodeTemplate node : topology.getNodeTemplates().values()) {
            if (node.getArtifacts() != null && CollectionUtils.isNotEmpty(node.getArtifacts().values())) {
                for (DeploymentArtifact artifact : node.getArtifacts().values()) {
                    // Only generate repositories for the current topology
                    if ((topologyArchiveName.equals(artifact.getArchiveName()) && topologyArchiveVersion.equals(artifact.getArchiveVersion()))
                            && StringUtils.isNotBlank(artifact.getRepositoryURL()) && repositoriesName.add(artifact.getRepositoryName())) {
                        buffer.append("  ").append(artifact.getRepositoryName()).append(":");
                        buffer.append("\n").append(formatRepository(artifact, 2)).append("\n");
                    }
                }
            }
        }
        if (MapUtils.isNotEmpty(topology.getInputArtifacts())) {
            topology.getInputArtifacts().values().forEach(inputArtifact -> {
                if (StringUtils.isNotBlank(inputArtifact.getRepositoryURL()) && repositoriesName.add(inputArtifact.getRepositoryName())) {
                    buffer.append("  ").append(inputArtifact.getRepositoryName()).append(":");
                    buffer.append("\n").append(formatRepository(inputArtifact, 2)).append("\n");
                }
            });
        }
        buffer.setLength(buffer.length() - 1);
        return buffer.toString();
    }

    public static String formatRepository(DeploymentArtifact value, int indent) {
        StringBuilder buffer = new StringBuilder();
        String spaces = ToscaPropertySerializerUtils.indent(indent);
        buffer.append(spaces).append("url: ").append(value.getRepositoryURL());
        buffer.append("\n").append(spaces).append("type: ").append(value.getArtifactRepository());
        if (value.getRepositoryCredential() != null && value.getRepositoryCredential().containsKey("token")) {
            buffer.append("\n").append(spaces).append("credential:");
            spaces += "  ";
            buffer.append("\n").append(spaces).append("token: ").append(value.getRepositoryCredential().get("token"));
            if (value.getRepositoryCredential().containsKey("user")) {
                buffer.append("\n").append(spaces).append("user: ").append(value.getRepositoryCredential().get("user"));
            }
        }
        return buffer.toString();
    }

    public static String formatArtifact(AbstractArtifact value, int indent) {
        String spaces = ToscaPropertySerializerUtils.indent(indent);
        StringBuilder buffer = new StringBuilder();
        if (StringUtils.isNotBlank(value.getArtifactRef())) {
            buffer.append(spaces).append("file: ").append(value.getArtifactRef()).append("\n");
        }
        if (StringUtils.isNotBlank(value.getArtifactType())) {
            buffer.append(spaces).append("type: ").append(value.getArtifactType()).append("\n");
        }
        if (StringUtils.isNotBlank(value.getRepositoryName())) {
            buffer.append(spaces).append("repository: ").append(value.getRepositoryName()).append("\n");
        }
        if (buffer.length() > 1) {
            buffer.setLength(buffer.length() - 1);
        }
        return buffer.toString();
    }

    public static boolean canUseShortNotationForImplementationArtifact(Operation operation) {
        return MapUtils.isEmpty(operation.getInputParameters()) && StringUtils.isEmpty(operation.getImplementationArtifact().getRepositoryName());
    }

    private static final Pattern GET_INPUT_ARTIFACT_PATTERN = Pattern.compile("\\{ *get_input_artifact: +[^}]+}");

    public Map<String, DeploymentArtifact> getTopologyArtifacts(String topologyArchiveName, String topologyArchiveVersion,
            Map<String, DeploymentArtifact> artifacts) {
        if (artifacts == null) {
            return Collections.emptyMap();
        }
        // Only generate artifacts that are really stored inside the topology
        return artifacts.entrySet().stream()
                .filter(artifact -> (topologyArchiveName.equals(artifact.getValue().getArchiveName())
                        && topologyArchiveVersion.equals(artifact.getValue().getArchiveVersion()))
                        || "alien_topology".equals(artifact.getValue().getArtifactRepository())
                        || (artifact.getValue().getArtifactRef() != null && GET_INPUT_ARTIFACT_PATTERN.matcher(artifact.getValue().getArtifactRef()).matches()))
                .collect(Collectors.toMap(Map.Entry::getKey, (Map.Entry::getValue)));
    }

    public Map<String, AbstractPropertyValue> getServiceAttributes(NodeTemplate nodeTemplate) {
        if (nodeTemplate instanceof ServiceNodeTemplate) {
            ServiceNodeTemplate serviceNodeTemplate = (ServiceNodeTemplate) nodeTemplate;
            if (serviceNodeTemplate.getAttributeValues() != null) {
                return serviceNodeTemplate.getAttributeValues().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ScalarPropertyValue(entry.getValue())));
            } else {
                return Collections.emptyMap();
            }
        } else {
            return Collections.emptyMap();
        }
    }

    public Map<String, DeploymentArtifact> getServiceRelationshipArtifacts(NodeTemplate source, NodeTemplate target,
            RelationshipTemplate relationshipTemplate) {
        if (source instanceof ServiceNodeTemplate || target instanceof ServiceNodeTemplate) {
            return relationshipTemplate.getArtifacts();
        } else {
            return Collections.emptyMap();
        }
    }

    public static boolean isNull(Object o) {
        return o == null;
    }
}
