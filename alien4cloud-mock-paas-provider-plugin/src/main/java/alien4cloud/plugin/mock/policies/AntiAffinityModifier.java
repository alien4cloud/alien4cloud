package alien4cloud.plugin.mock.policies;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * Sample modifier that provides anti-affinity configuration for AWS nodes.
 */
@Component("mock-anti-affinity-modifier")
public class AntiAffinityModifier implements ITopologyModifier {
    private final static String AWS_MOCK_COMPUTE_TYPE = "org.alien4cloud.nodes.mock.aws.Compute";
    private final static String ANTI_AFFINITY_POLICY = "org.alien4cloud.mock.policies.AntiAffinity";

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        for (PolicyTemplate policyTemplate : safe(topology.getPolicies()).values()) {
            if (ANTI_AFFINITY_POLICY.equals(policyTemplate.getType())) {
                apply(policyTemplate, topology, context);
            }
        }
    }

    private void apply(PolicyTemplate policy, Topology topology, FlowExecutionContext context) {
        AbstractPropertyValue value = policy.getProperties().get("availability_zones");
        List<NodeTemplate> targets = getTargets(policy, topology, context);
        if (targets == null) {
            return; // Some targets are not instances of org.alien4cloud.nodes.mock.aws.Compute
        }
        if (safe(policy.getTargets()).size() < 2) {
            context.log().error("Anti-affinity policy {} is not correctly configured, at least 2 targets are required.", policy.getName());
            return;
        }
        if (!(value instanceof ListPropertyValue) || ((ListPropertyValue) value).getValue().size() < 2) {
            context.log().error("Anti-affinity policy {} is not correctly configured, zones property is required and must contains at least 2 values.",
                    policy.getName());
            return;
        }

        ListPropertyValue propertyValue = (ListPropertyValue) value;
        for (int i = 0; i < targets.size(); i++) {
            NodeTemplate nodeTemplate = targets.get(i);
            String nodeZone = (String) propertyValue.getValue().get(i % propertyValue.getValue().size());
            if (AWS_MOCK_COMPUTE_TYPE.equals(nodeTemplate.getType())) {
                context.log().info("Anti-affinity policy {} inject zone property {} to node {}", policy.getName(), nodeZone, nodeTemplate.getName());
                nodeTemplate.getProperties().put("zone", new ScalarPropertyValue(nodeZone));
            }
        }
    }

    private List<NodeTemplate> getTargets(PolicyTemplate policy, Topology topology, FlowExecutionContext context) {
        List<NodeTemplate> targets = Lists.newArrayList();
        for (String targetName : safe(policy.getTargets())) {
            NodeTemplate target = safe(topology.getNodeTemplates()).get(targetName);
            // This modifier is injected after matching phase. Nodes must have been matched against valid type.
            if (!AWS_MOCK_COMPUTE_TYPE.equals(target.getType())) {
                context.log().error("Anti-affinity policy {} is not correctly configured, target {} is not an instance of {}.", policy.getName(),
                        AWS_MOCK_COMPUTE_TYPE);
                return null;
            }
            targets.add(target);
        }
        return targets;
    }
}