package alien4cloud.paas.wf;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.springframework.stereotype.Component;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.wf.exception.BadWorkflowOperationException;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.paas.wf.validation.WorkflowValidator;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.WorkflowTask;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WorkflowsBuilderService {

    @Resource
    private ICSARRepositorySearchService csarRepoSearchService;

    @Resource
    private InstallWorkflowBuilder installWorkflowBuilder;

    @Resource
    private UninstallWorkflowBuilder uninstallWorkflowBuilder;

    @Resource
    private CustomWorkflowBuilder customWorkflowBuilder;

    @Resource
    private WorkflowValidator workflowValidator;

    public TopologyContext initWorkflows(TopologyContext topologyContext) {
        Map<String, Workflow> wfs = topologyContext.getTopology().getWorkflows();
        if (wfs == null) {
            wfs = Maps.newLinkedHashMap();
            topologyContext.getTopology().setWorkflows(wfs);
        }
        if (!wfs.containsKey(Workflow.INSTALL_WF)) {
            Workflow install = new Workflow();
            install.setStandard(true);
            install.setName(Workflow.INSTALL_WF);
            wfs.put(Workflow.INSTALL_WF, install);
            reinitWorkflow(Workflow.INSTALL_WF, topologyContext);
        }
        if (!wfs.containsKey(Workflow.UNINSTALL_WF)) {
            Workflow uninstall = new Workflow();
            uninstall.setStandard(true);
            uninstall.setName(Workflow.UNINSTALL_WF);
            wfs.put(Workflow.UNINSTALL_WF, uninstall);
            reinitWorkflow(Workflow.UNINSTALL_WF, topologyContext);
        }
        return topologyContext;
    }

    public Workflow ceateWorkflow(Topology topology, String name) {
        String workflowName = getWorkflowName(topology, name, 0);
        Workflow wf = new Workflow();
        wf.setName(workflowName);
        wf.setStandard(false);
        Map<String, Workflow> wfs = topology.getWorkflows();
        if (wfs == null) {
            wfs = Maps.newLinkedHashMap();
            topology.setWorkflows(wfs);
        }
        wfs.put(workflowName, wf);
        return wf;
    }

    private String getWorkflowName(Topology topology, String name, int index) {
        String workflowName = name;
        if (index > 0) {
            workflowName += "_" + index;
        }
        if (topology.getWorkflows() != null && topology.getWorkflows().containsKey(workflowName)) {
            return getWorkflowName(topology, name, ++index);
        } else {
            return workflowName;
        }
    }

    private void debugWorkflow(Topology topology) {
        if (log.isDebugEnabled()) {
            for (Workflow wf : topology.getWorkflows().values()) {
                log.debug(WorkflowUtils.debugWorkflow(wf));
            }
        }
    }

    public int validateWorkflow(TopologyContext topologyContext, Workflow workflow) {
        return workflowValidator.validate(topologyContext, workflow);
    }

    public void addNode(TopologyContext topologyContext, String nodeName, NodeTemplate nodeTemplate) {
        boolean forceOperation = WorkflowUtils.isComputeOrNetwork(nodeName, topologyContext);
        for (Workflow wf : topologyContext.getTopology().getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
            builder.addNode(wf, nodeName, topologyContext, forceOperation);
            WorkflowUtils.fillHostId(wf, topologyContext);
            workflowValidator.validate(topologyContext, wf);
        }
        debugWorkflow(topologyContext.getTopology());
    }

    public void removeNode(Topology topology, String nodeName, NodeTemplate nodeTemplate) {
        TopologyContext topologyContext = buildTopologyContext(topology);
        for (Workflow wf : topology.getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
            builder.removeNode(wf, nodeName);
            WorkflowUtils.fillHostId(wf, topologyContext);
            workflowValidator.validate(topologyContext, wf);
        }
        debugWorkflow(topology);
    }

    public void addRelationship(TopologyContext topologyContext, String nodeTemplateName, String relationshipName) {
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeTemplateName);
        RelationshipTemplate relationshipTemplate = nodeTemplate.getRelationships().get(relationshipName);
        for (Workflow wf : topologyContext.getTopology().getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
            builder.addRelationship(wf, nodeTemplateName, nodeTemplate, relationshipTemplate, topologyContext);
            WorkflowUtils.fillHostId(wf, topologyContext);
            workflowValidator.validate(topologyContext, wf);
        }
        debugWorkflow(topologyContext.getTopology());
    }

    public void removeRelationship(Topology topology, String nodeTemplateName, String relationshipName, RelationshipTemplate relationshipTemplate) {
        TopologyContext topologyContext = buildTopologyContext(topology);
        String relationhipTarget = relationshipTemplate.getTarget();
        for (Workflow wf : topology.getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
            builder.removeRelationship(wf, nodeTemplateName, relationhipTarget);
            WorkflowUtils.fillHostId(wf, topologyContext);
            workflowValidator.validate(topologyContext, wf);
        }
    }

    public Workflow removeEdge(Topology topology, String workflowName, String from, String to) {
        TopologyContext topologyContext = buildTopologyContext(topology);
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.removeEdge(wf, from, to);
        workflowValidator.validate(topologyContext, wf);
        return wf;
    }

    public Workflow connectStepFrom(Topology topology, String workflowName, String stepId, String[] stepNames) {
        TopologyContext topologyContext = buildTopologyContext(topology);
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.connectStepFrom(wf, stepId, stepNames);
        workflowValidator.validate(topologyContext, wf);
        return wf;
    }

    public Workflow connectStepTo(Topology topology, String workflowName, String stepId, String[] stepNames) {
        TopologyContext topologyContext = buildTopologyContext(topology);
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.connectStepTo(wf, stepId, stepNames);
        workflowValidator.validate(topologyContext, wf);
        return wf;
    }

    private AbstractWorkflowBuilder getWorkflowBuilder(Workflow workflow) {
        if (workflow.isStandard()) {
            if (workflow.getName().equals(Workflow.INSTALL_WF)) {
                return installWorkflowBuilder;
            } else if (workflow.getName().equals(Workflow.UNINSTALL_WF)) {
                return uninstallWorkflowBuilder;
            }
        }
        return customWorkflowBuilder;
    }

    public Workflow removeStep(Topology topology, String workflowName, String stepId, boolean force) {
        TopologyContext topologyContext = buildTopologyContext(topology);
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.removeStep(wf, stepId, force);
        if (log.isDebugEnabled()) {
            log.debug(WorkflowUtils.debugWorkflow(wf));
        }
        workflowValidator.validate(topologyContext, wf);
        return wf;
    }

    public Workflow renameStep(Topology topology, String workflowName, String stepId, String newStepName) {
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.renameStep(wf, stepId, newStepName);
        if (log.isDebugEnabled()) {
            log.debug(WorkflowUtils.debugWorkflow(wf));
        }
        return wf;
    }

    public Workflow addActivity(Topology topology, String workflowName, String relatedStepId, boolean before, AbstractActivity activity) {
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        TopologyContext topologyContext = buildTopologyContext(topology);
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.addActivity(wf, relatedStepId, before, activity, topologyContext);
        WorkflowUtils.fillHostId(wf, topologyContext);
        if (log.isDebugEnabled()) {
            log.debug(WorkflowUtils.debugWorkflow(wf));
        }
        workflowValidator.validate(topologyContext, wf);
        return wf;
    }

    public Workflow swapSteps(Topology topology, String workflowName, String stepId, String targetId) {
        TopologyContext topologyContext = buildTopologyContext(topology);
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.swapSteps(wf, stepId, targetId);
        WorkflowUtils.fillHostId(wf, topologyContext);
        if (log.isDebugEnabled()) {
            log.debug(WorkflowUtils.debugWorkflow(wf));
        }
        workflowValidator.validate(topologyContext, wf);
        return wf;
    }

    public void renameNode(Topology topology, String nodeTemplateName, String newNodeTemplateName) {
        if (topology.getWorkflows() == null) {
            return;
        }
        TopologyContext topologyContext = buildTopologyContext(topology);
        for (Workflow wf : topology.getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
            builder.renameNode(wf, nodeTemplateName, newNodeTemplateName);
            WorkflowUtils.fillHostId(wf, topologyContext);
            workflowValidator.validate(topologyContext, wf);
        }
    }

    public Workflow reinitWorkflow(String workflowName, TopologyContext topologyContext) {
        Workflow wf = topologyContext.getTopology().getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        if (!wf.isStandard()) {
            throw new BadWorkflowOperationException(String.format("Reinit can not be performed on non standard workflow '%s'", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        wf = builder.reinit(wf, topologyContext);
        WorkflowUtils.fillHostId(wf, topologyContext);
        workflowValidator.validate(topologyContext, wf);
        return wf;
    }

    public interface TopologyContext {
        Topology getTopology();

        <T extends AbstractToscaType> T findElement(Class<T> clazz, String id);
    }

    public TopologyContext buildTopologyContext(Topology topology) {
        return buildCachedTopologyContext(new DefaultTopologyContext(topology));
    }

    public TopologyContext buildCachedTopologyContext(TopologyContext topologyContext) {
        return new CachedTopologyContext(topologyContext);
    }

    private class DefaultTopologyContext implements TopologyContext {
        private Topology topology;

        public DefaultTopologyContext(Topology topology) {
            super();
            this.topology = topology;
        }

        @Override
        public Topology getTopology() {
            return topology;
        }

        @Override
        public <T extends AbstractToscaType> T findElement(Class<T> clazz, String id) {
            return csarRepoSearchService.getElementInDependencies(clazz, id, topology.getDependencies());
        }
    }

    private class CachedTopologyContext implements TopologyContext {
        private TopologyContext wrapped;

        private Map<Class<? extends AbstractToscaType>, Map<String, AbstractToscaType>> cache = Maps.newHashMap();

        public CachedTopologyContext(TopologyContext wrapped) {
            super();
            this.wrapped = wrapped;
        }

        @Override
        public Topology getTopology() {
            return wrapped.getTopology();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends AbstractToscaType> T findElement(Class<T> clazz, String id) {
            Map<String, AbstractToscaType> typeCache = cache.get(clazz);
            if (typeCache == null) {
                if (log.isTraceEnabled()) {
                    log.trace(String.format("TopologyContext type cache not found for type <%s>, init one ...", clazz.getSimpleName()));
                }
                typeCache = Maps.newHashMap();
                cache.put(clazz, typeCache);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace(String.format("TopologyContext type cache found for type <%s>, using it !", clazz.getSimpleName()));
                }
            }
            AbstractToscaType element = typeCache.get(id);
            if (element == null) {
                if (log.isTraceEnabled()) {
                    log.trace(String.format("Element not found from cache for type <%s> id <%s>, look for in source ...", clazz.getSimpleName(), id));
                }
                element = wrapped.findElement(clazz, id);
                typeCache.put(id, element);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace(String.format("Element found from cache for type <%s> id <%s>, hit !", clazz.getSimpleName(), id));
                }
            }
            return (T) element;
        }

    }

    public List<WorkflowTask> validateWorkflows(Topology topology) {
        List<WorkflowTask> tasks = Lists.newArrayList();
        if (topology.getWorkflows() != null) {
            TopologyContext topologyContext = buildTopologyContext(topology);
            for (Workflow workflow : topology.getWorkflows().values()) {
                int errorCount = validateWorkflow(topologyContext, workflow);
                if (errorCount > 0) {
                    WorkflowTask workflowTask = new WorkflowTask();
                    workflowTask.setCode(TaskCode.WORKFLOW_INVALID);
                    workflowTask.setWorkflowName(workflow.getName());
                    workflowTask.setErrorCount(errorCount);
                    tasks.add(workflowTask);
                }
            }
        }
        return tasks;
    }

    /**
     * Get a workflow from a topoogy or fail with a {@link NotFoundException}
     * 
     * @param workflowName name of the wrolkflow to retrieve
     * @param topology {@link Topology } in which to retrieve the workflow
     * @return The workflow found with the given name
     */
    public Workflow getWorkflow(String workflowName, Topology topology) {
        Workflow workflow = topology.getWorkflows().get(workflowName);
        if (workflow == null) {
            throw new NotFoundException("Workflow <" + workflowName + "> not found in topology <" + topology.getId() + ">");
        }

        return workflow;
    }
}
