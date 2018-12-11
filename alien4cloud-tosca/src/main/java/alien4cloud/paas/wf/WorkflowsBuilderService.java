package alien4cloud.paas.wf;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.declarative.DefaultDeclarativeWorkflows;
import org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.wf.exception.BadWorkflowOperationException;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.paas.wf.validation.WorkflowValidator;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.WorkflowTask;
import alien4cloud.tosca.parser.ToscaParser;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.YamlParserUtil;
import lombok.extern.slf4j.Slf4j;

import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.*;

@Component
@Slf4j
public class WorkflowsBuilderService {

    @Resource
    private ICSARRepositorySearchService csarRepoSearchService;

    @Resource
    private WorkflowValidator workflowValidator;

    @Resource
    private CustomWorkflowBuilder customWorkflowBuilder;

    @Resource
    private WorkflowSimplifyService workflowSimplifyService;

    private Map<String, DefaultDeclarativeWorkflows> defaultDeclarativeWorkflowsPerDslVersion;

    private DefaultDeclarativeWorkflows loadDefaultDeclarativeWorkflow(String configName) throws IOException {
        return YamlParserUtil.parse(DefaultDeclarativeWorkflows.class.getClassLoader().getResourceAsStream(configName), DefaultDeclarativeWorkflows.class);
    }

    @PostConstruct
    public void loadDefaultDeclarativeWorkflows() throws IOException {
        this.defaultDeclarativeWorkflowsPerDslVersion = new HashMap<>();
        this.defaultDeclarativeWorkflowsPerDslVersion.put(ToscaParser.NORMATIVE_DSL_100, loadDefaultDeclarativeWorkflow("declarative-workflows-2.0.0.yml"));
        this.defaultDeclarativeWorkflowsPerDslVersion.put(ToscaParser.NORMATIVE_DSL_100_URL, loadDefaultDeclarativeWorkflow("declarative-workflows-2.0.0.yml"));
        this.defaultDeclarativeWorkflowsPerDslVersion.put(ToscaParser.ALIEN_DSL_200, loadDefaultDeclarativeWorkflow("declarative-workflows-2.0.0-jobs.yml"));
        this.defaultDeclarativeWorkflowsPerDslVersion.put(ToscaParser.ALIEN_DSL_120, loadDefaultDeclarativeWorkflow("declarative-workflows-old.yml"));
        this.defaultDeclarativeWorkflowsPerDslVersion.put(ToscaParser.ALIEN_DSL_130, loadDefaultDeclarativeWorkflow("declarative-workflows-old.yml"));
        this.defaultDeclarativeWorkflowsPerDslVersion.put(ToscaParser.ALIEN_DSL_140, loadDefaultDeclarativeWorkflow("declarative-workflows-old.yml"));
    }

    public DefaultDeclarativeWorkflows getDeclarativeWorkflows(String dslVersion) {
        return defaultDeclarativeWorkflowsPerDslVersion.get(dslVersion);
    }

    public void initWorkflows(TopologyContext topologyContext) {
        Map<String, Workflow> wfs = topologyContext.getTopology().getWorkflows();
        if (wfs == null) {
            wfs = Maps.newLinkedHashMap();
            topologyContext.getTopology().setWorkflows(wfs);
        }
        if (!wfs.containsKey(INSTALL)) {
            initStandardWorkflow(INSTALL, topologyContext);
        }
        if (!wfs.containsKey(UNINSTALL)) {
            initStandardWorkflow(UNINSTALL, topologyContext);
        }
        if (!wfs.containsKey(START)) {
            initStandardWorkflow(START, topologyContext);
        }
        if (!wfs.containsKey(STOP)) {
            initStandardWorkflow(STOP, topologyContext);
        }
        if (!wfs.containsKey(RUN)) {
            initStandardWorkflow(RUN, topologyContext);
        }
        if (!wfs.containsKey(CANCEL)) {
            initStandardWorkflow(CANCEL, topologyContext);
        }
        postProcessTopologyWorkflows(topologyContext);
    }

	public void postProcessTopologyWorkflows(TopologyContext tc) {
    	postProcessTopologyWorkflows(tc, NormativeWorkflowNameConstants.STANDARD_WORKFLOWS);
	}

    public void postProcessTopologyWorkflows(TopologyContext topologyContext, Set<String> whiteList) {
    	// Put aside the original workflow
    	whiteList.forEach(name -> topologyContext.getTopology().getUnprocessedWorkflows().put(name, WorkflowUtils.cloneWorkflow(topologyContext.getTopology().getWorkflow(name))));
    	// Simplify workflow
        workflowSimplifyService.simplifyWorkflow(topologyContext, whiteList);
        whiteList.forEach(name -> workflowValidator.validate(topologyContext, topologyContext.getTopology().getWorkflow(name)));
        debugWorkflow(topologyContext.getTopology());
    }

    public void refreshTopologyWorkflows(TopologyContext tc) {
        // Copy the original workflow than put them into the simplified workflow map
        tc.getTopology().getWorkflows().putAll(WorkflowUtils.cloneWorkflowMap(tc.getTopology().getUnprocessedWorkflows()));
        workflowSimplifyService.simplifyWorkflow(tc);
        for (Workflow wf : tc.getTopology().getWorkflows().values()) {
            workflowValidator.validate(tc, wf);
        }
        debugWorkflow(tc.getTopology());
    }

    private void initStandardWorkflow(String name, TopologyContext topologyContext) {
        Workflow workflow = new Workflow();
        workflow.setName(name);
        workflow.setStandard(true);
        workflow.setHasCustomModifications(false);
        topologyContext.getTopology().getWorkflows().put(name, workflow);
        reinitWorkflow(name, topologyContext, false);
    }

    public Workflow createWorkflow(Topology topology, String name) {
        String workflowName = getWorkflowName(topology, name, 0);
        Workflow wf = new Workflow();
        wf.setName(workflowName);
        wf.setStandard(false);
        wf.setHasCustomModifications(true);
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

    public void addNode(TopologyContext topologyContext, String nodeName) {
        boolean forceOperation = WorkflowUtils.isComputeOrNetwork(nodeName, topologyContext);
        // Use the unprocessed workflow to perform add node as we know that every steps / links will be present as it's defined in declarative workflow
        topologyContext.getTopology().getWorkflows().putAll(topologyContext.getTopology().getUnprocessedWorkflows());
        for (Workflow wf : topologyContext.getTopology().getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
            builder.addNode(wf, nodeName, topologyContext, forceOperation);
            WorkflowUtils.fillHostId(wf, topologyContext);
        }
        postProcessTopologyWorkflows(topologyContext);
    }

    public void removeNode(Topology topology, Csar csar, String nodeName) {
        TopologyContext topologyContext = buildTopologyContext(topology, csar);
        topologyContext.getTopology().getWorkflows().putAll(topologyContext.getTopology().getUnprocessedWorkflows());
        for (Workflow wf : topology.getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
            builder.removeNode(wf, nodeName);
            WorkflowUtils.fillHostId(wf, topologyContext);
        }
        postProcessTopologyWorkflows(topologyContext);
        debugWorkflow(topology);
    }

    public void addRelationship(TopologyContext topologyContext, String nodeTemplateName, String relationshipName) {
        topologyContext.getTopology().getWorkflows().putAll(topologyContext.getTopology().getUnprocessedWorkflows());
        NodeTemplate nodeTemplate = topologyContext.getTopology().getNodeTemplates().get(nodeTemplateName);
        RelationshipTemplate relationshipTemplate = nodeTemplate.getRelationships().get(relationshipName);
        for (Workflow wf : topologyContext.getTopology().getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
            builder.addRelationship(wf, nodeTemplateName, nodeTemplate, relationshipName, relationshipTemplate, topologyContext);
            WorkflowUtils.fillHostId(wf, topologyContext);
        }
        postProcessTopologyWorkflows(topologyContext);
        debugWorkflow(topologyContext.getTopology());
    }

    public void removeRelationship(Topology topology, Csar csar, String sourceNodeId, String relationshipName, RelationshipTemplate relationshipTemplate) {
        TopologyContext topologyContext = buildTopologyContext(topology, csar);
        topologyContext.getTopology().getWorkflows().putAll(topologyContext.getTopology().getUnprocessedWorkflows());
        NodeTemplate sourceNode = topology.getNodeTemplates().get(sourceNodeId);
        String targetNodeId = relationshipTemplate.getTarget();
        NodeTemplate targetNode = topologyContext.getTopology().getNodeTemplates().get(targetNodeId);
        for (Workflow wf : topology.getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
            // Remove relationships from source to target
            // Remove relationships from target to source
            Map<String, RelationshipTemplate> sourceRelationships = sourceNode.getRelationships().entrySet().stream()
                    .filter(relationshipEntry -> relationshipEntry.getValue().getTarget().equals(targetNodeId))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<String, RelationshipTemplate> targetRelationships = AlienUtils.safe(targetNode.getRelationships()).entrySet().stream()
                    .filter(relationshipEntry -> relationshipEntry.getValue().getTarget().equals(sourceNodeId))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            builder.removeRelationships(wf, sourceNodeId, sourceRelationships, targetNodeId, targetRelationships);
            sourceRelationships.entrySet().stream().filter(entry -> !entry.getKey().equals(relationshipName))
                    .forEach(entry -> builder.addRelationship(wf, sourceNode.getName(), sourceNode, entry.getKey(), entry.getValue(), topologyContext));
            targetRelationships.forEach((id, relationship) -> builder.addRelationship(wf, targetNodeId, targetNode, id, relationship, topologyContext));
            // Remove unique relationship that we really want to remove
            WorkflowUtils.fillHostId(wf, topologyContext);
        }
        postProcessTopologyWorkflows(topologyContext);
        debugWorkflow(topologyContext.getTopology());
    }

    public Workflow removeEdge(Topology topology, Csar csar, String workflowName, String from, String to) {
        TopologyContext topologyContext = buildTopologyContext(topology, csar);
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
        builder.removeEdge(wf, from, to);
        workflowValidator.validate(topologyContext, wf);
        return wf;
    }

    public Workflow connectStepFrom(Topology topology, Csar csar, String workflowName, String stepId, String[] stepNames) {
        TopologyContext topologyContext = buildTopologyContext(topology, csar);
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
        builder.connectStepFrom(wf, stepId, stepNames);
        workflowValidator.validate(topologyContext, wf);
        return wf;
    }

    public Workflow connectStepTo(Topology topology, Csar csar, String workflowName, String stepId, String[] stepNames) {
        TopologyContext topologyContext = buildTopologyContext(topology, csar);
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
        builder.connectStepTo(wf, stepId, stepNames);
        workflowValidator.validate(topologyContext, wf);
        return wf;
    }

    private AbstractWorkflowBuilder getWorkflowBuilder(String dslVersion, Workflow workflow) {
        if (workflow.isStandard()) {
            if (dslVersion == null || !this.defaultDeclarativeWorkflowsPerDslVersion.containsKey(dslVersion)) {
                dslVersion = ToscaParser.LATEST_DSL;
            }
            DefaultDeclarativeWorkflows defaultDeclarativeWorkflows = this.defaultDeclarativeWorkflowsPerDslVersion.get(dslVersion);
            return new DefaultWorkflowBuilder(defaultDeclarativeWorkflows);
        } else {
            return customWorkflowBuilder;
        }
    }

    public void removeStep(Topology topology, Csar csar, String workflowName, String stepId) {
        TopologyContext topologyContext = buildTopologyContext(topology, csar);
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
        builder.removeStep(wf, stepId, false);
        if (log.isDebugEnabled()) {
            log.debug(WorkflowUtils.debugWorkflow(wf));
        }
        workflowValidator.validate(topologyContext, wf);
    }

    public void renameStep(Topology topology, Csar csar, String workflowName, String stepId, String newStepName) {
        TopologyContext topologyContext = buildTopologyContext(topology, csar);
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
        builder.renameStep(wf, stepId, newStepName);
        if (log.isDebugEnabled()) {
            log.debug(WorkflowUtils.debugWorkflow(wf));
        }
    }

    public void addActivity(Topology topology, Csar csar, String workflowName, String relatedStepId, boolean before, String target, String targetRelationship,
            AbstractWorkflowActivity activity) {
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        TopologyContext topologyContext = buildTopologyContext(topology, csar);
        AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
        builder.addActivity(wf, relatedStepId, before, target, targetRelationship, activity, topologyContext);
        WorkflowUtils.fillHostId(wf, topologyContext);
        if (log.isDebugEnabled()) {
            log.debug(WorkflowUtils.debugWorkflow(wf));
        }
        workflowValidator.validate(topologyContext, wf);
    }

    public void swapSteps(Topology topology, Csar csar, String workflowName, String stepId, String targetId) {
        TopologyContext topologyContext = buildTopologyContext(topology, csar);
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
        builder.swapSteps(wf, stepId, targetId);
        WorkflowUtils.fillHostId(wf, topologyContext);
        if (log.isDebugEnabled()) {
            log.debug(WorkflowUtils.debugWorkflow(wf));
        }
        workflowValidator.validate(topologyContext, wf);
    }

    public void renameNode(Topology topology, Csar csar, String nodeTemplateName, String newNodeTemplateName) {
        if (topology.getWorkflows() == null) {
            return;
        }
        TopologyContext topologyContext = buildTopologyContext(topology, csar);
        for (Workflow wf : topology.getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
            builder.renameNode(wf, nodeTemplateName, newNodeTemplateName);
            if (topology.getUnprocessedWorkflows().containsKey(wf.getName())) {
                builder.renameNode(topology.getUnprocessedWorkflows().get(wf.getName()), nodeTemplateName, newNodeTemplateName);
            }
            WorkflowUtils.fillHostId(wf, topologyContext);
            workflowValidator.validate(topologyContext, wf);
        }
    }

    public void reinitWorkflow(String workflowName, TopologyContext topologyContext, boolean simplify) {
        Workflow wf = topologyContext.getTopology().getWorkflows().get(workflowName);
        if (wf == null) {
            throw new NotFoundException(String.format("The workflow '%s' can not be found", workflowName));
        }
        if (!wf.isStandard()) {
            throw new BadWorkflowOperationException(String.format("Reinit can not be performed on non standard workflow '%s'", workflowName));
        }
        AbstractWorkflowBuilder builder = getWorkflowBuilder(topologyContext.getDSLVersion(), wf);
        wf = builder.reinit(wf, topologyContext);
        WorkflowUtils.fillHostId(wf, topologyContext);
        if (simplify) {
            postProcessTopologyWorkflows(topologyContext, Sets.newHashSet(workflowName));
        }
    }

    public TopologyContext buildTopologyContext(Topology topology) {
        return buildTopologyContext(topology, null);
    }

    public TopologyContext buildTopologyContext(Topology topology, Csar csar) {
        DefaultTopologyContext topologyContext;
        if (csar == null) {
            topologyContext = new DefaultTopologyContext(topology);
        } else {
            topologyContext = new DefaultTopologyContext(topology, csar);
        }
        return buildCachedTopologyContext(topologyContext);
    }

    public TopologyContext buildCachedTopologyContext(TopologyContext topologyContext) {
        return new CachedTopologyContext(topologyContext);
    }

    private class DefaultTopologyContext implements TopologyContext {
        private Topology topology;
        private String dslVersion;

        DefaultTopologyContext(Topology topology) {
            super();
            this.topology = topology;
            Csar topologyCsar = csarRepoSearchService.getArchive(topology.getArchiveName(), topology.getArchiveVersion());
            if (topologyCsar == null) {
                throw new NotFoundException("Topology's csar " + topology.getArchiveName() + ":" + topology.getArchiveVersion() + " cannot be found");
            }
            this.dslVersion = topologyCsar.getToscaDefinitionsVersion();
        }

        DefaultTopologyContext(Topology topology, Csar csar) {
            super();
            this.topology = topology;
            this.dslVersion = csar.getToscaDefinitionsVersion();
        }

        @Override
        public String getDSLVersion() {
            return dslVersion;
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

        CachedTopologyContext(TopologyContext wrapped) {
            super();
            this.wrapped = wrapped;
        }

        @Override
        public Topology getTopology() {
            return wrapped.getTopology();
        }

        @Override
        public String getDSLVersion() {
            return wrapped.getDSLVersion();
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
