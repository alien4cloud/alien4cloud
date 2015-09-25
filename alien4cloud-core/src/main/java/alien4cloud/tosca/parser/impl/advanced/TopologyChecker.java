package alien4cloud.tosca.parser.impl.advanced;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedInheritableToscaElement;
import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.PropertyValue;
import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.AbstractActivity;
import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.NodeActivityStep;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.IChecker;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ToscaParsingUtil;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Component
public class TopologyChecker implements IChecker<Topology> {

    private static final String KEY = "topologyChecker";

    @Resource
    private ICSARRepositorySearchService searchService;

    @Resource
    private ConstraintPropertyService constraintPropertyService;

    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    public String getName() {
        return KEY;
    }

    @Override
    public void before(ParsingContextExecution context, Node node) {
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();

        // we need that node types inherited stuffs have to be merged before we start parsing node templates and requirements
        mergeHierarchy(archiveRoot.getArtifactTypes(), archiveRoot);
        mergeHierarchy(archiveRoot.getCapabilityTypes(), archiveRoot);
        mergeHierarchy(archiveRoot.getNodeTypes(), archiveRoot);
        mergeHierarchy(archiveRoot.getDataTypes(), archiveRoot);
        mergeHierarchy(archiveRoot.getRelationshipTypes(), archiveRoot);
    }

    @Override
    public void check(final Topology instance, ParsingContextExecution context, Node node) {
        if (instance.isEmpty()) {
            // if the topology doesn't contains any node template it won't be imported so add a warning.
            context.getParsingErrors().add(
                    new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.EMPTY_TOPOLOGY, null, node.getStartMark(), null, node.getEndMark(), ""));
        }

        final ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        Set<CSARDependency> dependencies = archiveRoot.getArchive().getDependencies();
        if (dependencies != null) {
            instance.setDependencies(dependencies);
        }

        // here we need to check that the group members really exist
        if (instance.getGroups() != null && !instance.getGroups().isEmpty()) {
            int i = 0;
            for (NodeGroup nodeGroup : instance.getGroups().values()) {
                nodeGroup.setIndex(i++);
                Iterator<String> groupMembers = nodeGroup.getMembers().iterator();
                while (groupMembers.hasNext()) {
                    String nodeTemplateId = groupMembers.next();
                    NodeTemplate nodeTemplate = instance.getNodeTemplates().get(nodeTemplateId);
                    if (nodeTemplate == null) {
                        // add an error to the context
                        context.getParsingErrors().add(
                                new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKOWN_GROUP_MEMBER, null, node.getStartMark(), null, node.getEndMark(),
                                        nodeTemplateId));
                        // and remove the member
                        groupMembers.remove();
                    } else {
                        Set<String> groups = nodeTemplate.getGroups();
                        if (groups == null) {
                            groups = Sets.newHashSet();
                            nodeTemplate.setGroups(groups);
                        }
                        groups.add(nodeGroup.getName());
                    }
                }
            }
        }

        // check properties inputs validity
        if (instance.getNodeTemplates() != null && !instance.getNodeTemplates().isEmpty()) {
            for (Entry<String, NodeTemplate> nodeEntry : instance.getNodeTemplates().entrySet()) {
                String nodeName = nodeEntry.getKey();
                NodeTemplate nodeTemplate = nodeEntry.getValue();
                if (nodeEntry.getValue().getProperties() == null) {
                    continue;
                }
                IndexedNodeType nodeType = ToscaParsingUtil.getNodeTypeFromArchiveOrDependencies(nodeTemplate.getType(), archiveRoot, searchService);
                if (nodeType == null) {
                    // Already caught in NodeTemplateChecker
                    continue;
                }
                for (Entry<String, AbstractPropertyValue> propertyEntry : nodeEntry.getValue().getProperties().entrySet()) {
                    String propertyName = propertyEntry.getKey();
                    AbstractPropertyValue propertyValue = propertyEntry.getValue();
                    if (nodeType.getProperties() == null || !nodeType.getProperties().containsKey(propertyName)) {
                        context.getParsingErrors().add(
                                new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.UNRECOGNIZED_PROPERTY, nodeName, node.getStartMark(), "Property "
                                        + propertyName + " does not exist in type " + nodeType.getElementId(), node.getEndMark(), propertyName));
                        continue;
                    }
                    PropertyDefinition propertyDefinition = nodeType.getProperties().get(propertyName);
                    if (propertyValue instanceof FunctionPropertyValue) {
                        FunctionPropertyValue function = (FunctionPropertyValue) propertyValue;
                        String parameters = function.getParameters().get(0);
                        // check get_input only
                        if (function.getFunction().equals("get_input")) {
                            if (instance.getInputs() == null || !instance.getInputs().keySet().contains(parameters)) {
                                context.getParsingErrors().add(
                                        new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.MISSING_TOPOLOGY_INPUT, nodeName, node.getStartMark(), parameters,
                                                node.getEndMark(), propertyName));
                            }
                        }
                    } else if (propertyValue instanceof PropertyValue<?>) {
                        try {
                            constraintPropertyService.checkPropertyConstraint(propertyName, ((PropertyValue<?>) propertyValue).getValue(), propertyDefinition,
                                    archiveRoot);
                        } catch (ConstraintValueDoNotMatchPropertyTypeException | ConstraintViolationException e) {
                            StringBuilder problem = new StringBuilder("Validation issue ");
                            if (e.getConstraintInformation() != null) {
                                problem.append("for " + e.getConstraintInformation().toString());
                            }
                            problem.append(e.getMessage());
                            context.getParsingErrors().add(
                                    new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.VALIDATION_ERROR, nodeName, node.getStartMark(), problem.toString(),
                                            node.getEndMark(), propertyName));
                        }
                    }
                }
            }
        }

        // manage the workflows
        TopologyContext topologyContext = workflowBuilderService.buildCachedTopologyContext(new TopologyContext() {
            @Override
            public Topology getTopology() {
                return instance;
            }
            @Override
            public <T extends IndexedToscaElement> T findElement(Class<T> clazz, String id) {
                return ToscaParsingUtil.getElementFromArchiveOrDependencies(clazz, id, archiveRoot, searchService);
            }
        });
        finalizeParsedWorkflows(topologyContext, context, node);
        // workflowBuilderService.initWorkflows(topologyContext);

    }

    /**
     * Called after yaml parsing.
     */
    private void finalizeParsedWorkflows(TopologyContext topologyContext, ParsingContextExecution context, Node node) {
        if (topologyContext.getTopology().getWorkflows() == null || topologyContext.getTopology().getWorkflows().isEmpty()) {
            return;
        }
        for (Workflow wf : topologyContext.getTopology().getWorkflows().values()) {
            wf.setStandard(WorkflowUtils.isStandardWorkflow(wf));
            if (wf.getSteps() != null) {
                for (AbstractStep step : wf.getSteps().values()) {
                    if (step.getFollowingSteps() != null) {
                        Iterator<String> followingIds = step.getFollowingSteps().iterator();
                        while (followingIds.hasNext()) {
                            String followingId = followingIds.next();
                            AbstractStep followingStep = wf.getSteps().get(followingId);
                            if (followingStep == null) {
                                followingIds.remove();
                                // TODO: add an error in parsing context ?
                                context.getParsingErrors().add(
                                        new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKNWON_WORKFLOW_STEP, null, node.getStartMark(), null, node
                                                .getEndMark(), followingId));
                            } else {
                                followingStep.addPreceding(step.getName());
                            }
                        }
                    }
                    if (step instanceof NodeActivityStep) {
                        AbstractActivity activity = ((NodeActivityStep) step).getActivity();
                        if (activity == null) {
                            // add an error ?
                        } else {
                            activity.setNodeId(((NodeActivityStep) step).getNodeId());
                        }
                    }
                }
            }
            WorkflowUtils.fillHostId(wf, topologyContext);
            int errorCount = workflowBuilderService.validateWorkflow(topologyContext, wf);
            if (errorCount > 0) {
                context.getParsingErrors().add(
                        new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.WORKFLOW_HAS_ERRORS, null, node.getStartMark(), null, node.getEndMark(), wf
                                .getName()));
            }
        }
    }
    
    private <T extends IndexedInheritableToscaElement> void mergeHierarchy(Map<String, T> indexedElements, ArchiveRoot archiveRoot) {
        if (indexedElements == null) {
            return;
        }
        for (T element : indexedElements.values()) {
            mergeHierarchy(element, archiveRoot);
        }
    }

    private <T extends IndexedInheritableToscaElement> void mergeHierarchy(T indexedElement, ArchiveRoot archiveRoot) {
        List<String> derivedFrom = indexedElement.getDerivedFrom();
        if (derivedFrom == null) {
            return;
        }
        Map<String, T> hierarchy = Maps.newHashMap();
        for (String parentId : derivedFrom) {
            T parentElement = (T) ToscaParsingUtil.getElementFromArchiveOrDependencies(indexedElement.getClass(), parentId, archiveRoot, searchService);
            hierarchy.put(parentElement.getId(), parentElement);
        }
        List<T> hierarchyList = IndexedModelUtils.orderByDerivedFromHierarchy(hierarchy);
        hierarchyList.add(indexedElement);
        for (int i = 0; i < hierarchyList.size() - 1; i++) {
            T from = hierarchyList.get(i);
            T to = hierarchyList.get(i + 1);
            if (Objects.equal(to.getArchiveName(), archiveRoot.getArchive().getName())
                    && Objects.equal(to.getArchiveVersion(), archiveRoot.getArchive().getVersion())) {
                // we only merge element that come with current archive (the one we are parsing).
                // by this way, we don't remerge existing elements
                IndexedModelUtils.mergeInheritableIndex(from, to);
            }
        }
    }

}
