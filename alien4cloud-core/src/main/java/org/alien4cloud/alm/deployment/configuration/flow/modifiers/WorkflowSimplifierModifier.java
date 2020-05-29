package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import alien4cloud.aop.LogExecutionTime;
import alien4cloud.paas.wf.TopologyContext;
import alien4cloud.paas.wf.WorkflowSimplifyService;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.validation.WorkflowValidator;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.parser.ToscaParser;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class WorkflowSimplifierModifier implements ITopologyModifier {

    @Resource
    private WorkflowSimplifyService workflowSimplifyService;

    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    @ToscaContextual
    @LogExecutionTime
    public void process(Topology topology, FlowExecutionContext context) {
        log.info("Simplifying Workflow for topology " + topology.getId());

        try {
            WorkflowValidator.disableValidationThreadLocal.set(true);

            TopologyContext topologyContext = workflowBuilderService.buildCachedTopologyContext(new TopologyContext() {
                @Override
                public String getDSLVersion() {
                    return ToscaParser.LATEST_DSL;
                }

                @Override
                public Topology getTopology() {
                    return topology;
                }

                @Override
                public <T extends AbstractToscaType> T findElement(Class<T> clazz, String elementId) {
                    return ToscaContext.get(clazz, elementId);
                }
            });
            workflowSimplifyService.simplifyWorkflow(topologyContext, topology.getWorkflows().keySet());
        } catch (Exception e) {
            context.getLog().error("Could not simplify workflow");
            log.warn("Could not simplify workflow", e);
        } finally {
            WorkflowValidator.disableValidationThreadLocal.remove();
        }
    }
}
