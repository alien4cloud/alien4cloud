package alien4cloud.paas.plan;

import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.catalog.repository.CsarFileRepository;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.templates.*;
import org.alien4cloud.tosca.model.types.*;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.paas.exception.InvalidTopologyException;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.paas.model.AbstractPaaSTemplate;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyUtils;
import alien4cloud.tosca.ToscaUtils;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.normative.*;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.TypeMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility to build an hosted on tree from a topology.
 */
@Component
@Slf4j
public class TopologyTreeBuilderService {
    @Resource
    private CsarFileRepository repository;
    @Resource
    private IToscaTypeSearchService csarSearchService;

    public Map<String, PaaSNodeTemplate> buildPaaSNodeTemplates(Topology topology) {
        // cache IndexedToscaElements, CloudServiceArchive and ToscaElements to limit queries.
        TypeMap cache = new TypeMap();
        return buildPaaSNodeTemplates(topology, cache);
    }

    /**
     * Fetch informations from the repository to complete the topology node template informations with additional data such as artifacts paths etc.
     *
     * @param topology The topology for which to build PaaSNodeTemplate map.
     * @return A map of PaaSNodeTemplate that match the one of the NodeTempaltes in the given topology (and filled with artifact paths etc.).
     */
    public Map<String, PaaSNodeTemplate> buildPaaSNodeTemplates(Topology topology, TypeMap cache) {
        Map<String, PaaSNodeTemplate> nodeTemplates = Maps.newHashMap();

        // cache IndexedToscaElements, CloudServiceArchive and ToscaElements to limit queries.
        // TypeMap cache = new TypeMap();

        // Fill in PaaSNodeTemplate by fetching node types and CSAR path from the repositories.
        if (topology.getNodeTemplates() != null) {
            for (Entry<String, NodeTemplate> templateEntry : topology.getNodeTemplates().entrySet()) {
                NodeTemplate template = templateEntry.getValue();

                PaaSNodeTemplate paaSNodeTemplate = new PaaSNodeTemplate(templateEntry.getKey(), template);

                fillType(cache, topology, template, paaSNodeTemplate, NodeType.class);
                mergeInterfaces(paaSNodeTemplate, template);

                if (template.getRelationships() != null) {
                    for (Map.Entry<String, RelationshipTemplate> relationshipEntry : template.getRelationships().entrySet()) {
                        RelationshipTemplate relationshipTemplate = relationshipEntry.getValue();
                        PaaSRelationshipTemplate paaSRelationshipTemplate = new PaaSRelationshipTemplate(relationshipEntry.getKey(), relationshipTemplate,
                                paaSNodeTemplate.getId());
                        fillType(cache, topology, relationshipTemplate, paaSRelationshipTemplate, RelationshipType.class);
                        mergeInterfaces(paaSRelationshipTemplate, relationshipTemplate);
                        paaSNodeTemplate.getRelationshipTemplates().add(paaSRelationshipTemplate);
                    }
                }
                Capability scalableCapability = TopologyUtils.getScalableCapability(topology, templateEntry.getKey(), false);
                if (scalableCapability != null) {
                    ScalingPolicy scalingPolicy = TopologyUtils.getScalingPolicy(scalableCapability);
                    // A node with a scaling policy 1, 1, 1 is a simple node and so do not set scaling policy
                    if (!ScalingPolicy.NOT_SCALABLE_POLICY.equals(scalingPolicy)) {
                        paaSNodeTemplate.setScalingPolicy(scalingPolicy);
                    }
                }
                if (topology.getGroups() != null) {
                    Set<String> nodeGroups = Sets.newHashSet();
                    for (Map.Entry<String, NodeGroup> groupEntry : topology.getGroups().entrySet()) {
                        if (groupEntry.getValue().getMembers() != null && groupEntry.getValue().getMembers().contains(templateEntry.getKey())) {
                            nodeGroups.add(groupEntry.getKey());
                        }
                    }
                    paaSNodeTemplate.setGroups(nodeGroups);
                }
                nodeTemplates.put(templateEntry.getKey(), paaSNodeTemplate);
            }
        }
        return nodeTemplates;
    }

    /**
     * Get the non-natives node of a topology.
     * 
     * @param topology
     * @return a Map of non-natives nodes.
     */
    public Map<String, NodeTemplate> getNonNativesNodes(Topology topology) {
        TypeMap cache = new TypeMap();
        Map<String, NodeTemplate> nonNativesNode = new HashMap<>();

        if (topology.getNodeTemplates() != null) {
            for (Entry<String, NodeTemplate> templateEntry : topology.getNodeTemplates().entrySet()) {
                NodeTemplate template = templateEntry.getValue();
                NodeType indexedToscaElement = getToscaType(template.getType(), cache, topology.getDependencies(), NodeType.class);

                boolean isCompute = ToscaUtils.isFromType(NormativeComputeConstants.COMPUTE_TYPE, indexedToscaElement);
                boolean isNetwork = ToscaUtils.isFromType(NormativeNetworkConstants.NETWORK_TYPE, indexedToscaElement);
                boolean isVolume = ToscaUtils.isFromType(NormativeBlockStorageConstants.BLOCKSTORAGE_TYPE, indexedToscaElement);
                if (!isCompute && !isNetwork && !isVolume) {
                    nonNativesNode.put(templateEntry.getKey(), template);
                }
            }
        }

        return nonNativesNode;
    }

    @SneakyThrows
    private void mergeInterfaces(AbstractPaaSTemplate pasSTemplate, AbstractTemplate abstractTemplate) {
        AbstractToscaType type = pasSTemplate.getIndexedToscaElement();
        Map<String, Interface> typeInterfaces = null;
        if (type instanceof AbstractInstantiableToscaType) {
            typeInterfaces = ((AbstractInstantiableToscaType) type).getInterfaces();
        }
        Map<String, Interface> templateInterfaces = abstractTemplate.getInterfaces();
        // Here merge interfaces: the interface defined in the template should override those from type.
        pasSTemplate.setInterfaces(
                IndexedModelUtils.mergeInterfaces(JsonUtil.toMap(JsonUtil.toString(typeInterfaces), String.class, Interface.class), templateInterfaces));
    }

    /**
     * Build the topology for deployment on the PaaS.
     *
     * @param topology The topology.
     * @return The parsed topology for the PaaS with.
     */
    public PaaSTopology buildPaaSTopology(Topology topology, TypeMap cache) {
        return buildPaaSTopology(buildPaaSNodeTemplates(topology, cache));
    }

    /**
     * Build the topology for deployment on the PaaS.
     *
     * @param topology The topology.
     * @return The parsed topology for the PaaS with.
     */
    @ToscaContextual
    public PaaSTopology buildPaaSTopology(Topology topology) {
        return buildPaaSTopology(buildPaaSNodeTemplates(topology, new TypeMap()));
    }

    /**
     * Build the topology for deployment on the PaaS.
     *
     * @param nodeTemplates The node templates that are part of the topology.
     * @return The parsed topology for the PaaS with.
     */
    public PaaSTopology buildPaaSTopology(Map<String, PaaSNodeTemplate> nodeTemplates) {
        // Build hosted_on tree
        List<PaaSNodeTemplate> computes = new ArrayList<PaaSNodeTemplate>();
        List<PaaSNodeTemplate> networks = new ArrayList<PaaSNodeTemplate>();
        List<PaaSNodeTemplate> volumes = new ArrayList<PaaSNodeTemplate>();
        List<PaaSNodeTemplate> nonNatives = new ArrayList<PaaSNodeTemplate>();
        Map<String, List<PaaSNodeTemplate>> groups = Maps.newHashMap();
        for (Entry<String, PaaSNodeTemplate> entry : nodeTemplates.entrySet()) {
            PaaSNodeTemplate paaSNodeTemplate = entry.getValue();
            boolean isCompute = ToscaUtils.isFromType(NormativeComputeConstants.COMPUTE_TYPE, paaSNodeTemplate.getIndexedToscaElement());
            boolean isNetwork = ToscaUtils.isFromType(NormativeNetworkConstants.NETWORK_TYPE, paaSNodeTemplate.getIndexedToscaElement());
            boolean isVolume = ToscaUtils.isFromType(NormativeBlockStorageConstants.BLOCKSTORAGE_TYPE, paaSNodeTemplate.getIndexedToscaElement());
            if (isVolume) {
                // manage block storage
                processBlockStorage(paaSNodeTemplate, nodeTemplates);
                volumes.add(paaSNodeTemplate);
            } else if (isCompute) {
                // manage compute
                processNetwork(paaSNodeTemplate, nodeTemplates);
                processRelationship(paaSNodeTemplate, nodeTemplates);
                computes.add(paaSNodeTemplate);
            } else if (isNetwork) {
                // manage network
                networks.add(paaSNodeTemplate);
            } else {
                // manage non native
                nonNatives.add(paaSNodeTemplate);
                processRelationship(paaSNodeTemplate, nodeTemplates);
            }
            if (entry.getValue().getGroups() != null) {
                for (String group : entry.getValue().getGroups()) {
                    List<PaaSNodeTemplate> currentGroupMembers = groups.get(group);
                    if (currentGroupMembers == null) {
                        currentGroupMembers = Lists.newArrayList();
                        groups.put(group, currentGroupMembers);
                    }
                    currentGroupMembers.add(entry.getValue());
                }
            }
        }
        // check and register possible operation outputs
        processOperationsOutputs(nodeTemplates);

        return new PaaSTopology(computes, networks, volumes, nonNatives, nodeTemplates, groups);
    }

    private void processRelationship(PaaSNodeTemplate paaSNodeTemplate, Map<String, PaaSNodeTemplate> nodeTemplates) {
        PaaSRelationshipTemplate hostedOnRelationship = getPaaSRelationshipTemplateFromType(paaSNodeTemplate, NormativeRelationshipConstants.HOSTED_ON);
        if (hostedOnRelationship != null) {
            String target = hostedOnRelationship.getRelationshipTemplate().getTarget();
            PaaSNodeTemplate parent = nodeTemplates.get(target);
            parent.getChildren().add(paaSNodeTemplate);
            paaSNodeTemplate.setParent(parent);
        }
        // Relationships are defined from sources to target. We have to make sure that target node also has the relationship injected.
        List<PaaSRelationshipTemplate> allRelationships = getPaaSRelationshipsTemplateFromType(paaSNodeTemplate, NormativeRelationshipConstants.ROOT);
        for (PaaSRelationshipTemplate relationship : allRelationships) {
            // inject the relationship in it's target.
            String target = relationship.getRelationshipTemplate().getTarget();
            nodeTemplates.get(target).getRelationshipTemplates().add(relationship);
        }
    }

    private void processNetwork(PaaSNodeTemplate paaSNodeTemplate, Map<String, PaaSNodeTemplate> nodeTemplates) {
        List<PaaSRelationshipTemplate> networkRelationships = getPaaSRelationshipsTemplateFromType(paaSNodeTemplate, NormativeRelationshipConstants.NETWORK);
        List<PaaSNodeTemplate> networks = Lists.newArrayList();
        if (networkRelationships != null && !networkRelationships.isEmpty()) {
            for (PaaSRelationshipTemplate networkRelationship : networkRelationships) {
                String target = networkRelationship.getRelationshipTemplate().getTarget();
                PaaSNodeTemplate network = nodeTemplates.get(target);
                networks.add(network);
                network.setParent(paaSNodeTemplate);
            }
        }
        paaSNodeTemplate.setNetworkNodes(networks);
    }

    private void processBlockStorage(PaaSNodeTemplate paaSNodeTemplate, Map<String, PaaSNodeTemplate> nodeTemplates) {
        PaaSRelationshipTemplate attachTo = getPaaSRelationshipTemplateFromType(paaSNodeTemplate, NormativeRelationshipConstants.ATTACH_TO);
        if (attachTo != null) {
            String target = attachTo.getRelationshipTemplate().getTarget();
            PaaSNodeTemplate parent = nodeTemplates.get(target);
            parent.getStorageNodes().add(paaSNodeTemplate);
            paaSNodeTemplate.setParent(parent);
        }
    }

    /**
     * Get a single relationship from a given type. Note that the relationship MUST be unique, if not we throw an exception as the workflow cannot be generated.
     *
     * @param paaSNodeTemplate The node for which to get relationship.
     * @param type The type of relationship.
     * @return The unique relationship that matches the given type.
     */
    private PaaSRelationshipTemplate getPaaSRelationshipTemplateFromType(PaaSNodeTemplate paaSNodeTemplate, String type) {
        List<PaaSRelationshipTemplate> relationships = getPaaSRelationshipsTemplateFromType(paaSNodeTemplate, type);
        if (relationships.size() == 1) {
            return relationships.get(0);
        }
        if (relationships.size() > 1) {
            throw new InvalidTopologyException("Relationship that extends <" + type + "> must be unique on a given node.");
        }
        return null;
    }

    /**
     * Get all relationships from a given type (only if the node is the source of the relationship).
     *
     * @param paaSNodeTemplate The node.
     * @param type The type of relationships to get.
     * @return The relationship template
     */
    private List<PaaSRelationshipTemplate> getPaaSRelationshipsTemplateFromType(PaaSNodeTemplate paaSNodeTemplate, String type) {
        List<PaaSRelationshipTemplate> relationships = Lists.newArrayList();
        for (PaaSRelationshipTemplate relationship : paaSNodeTemplate.getRelationshipTemplates()) {
            if (relationship.instanceOf(type) && relationship.getSource().equals(paaSNodeTemplate.getId())) {
                relationships.add(relationship);
            }
        }
        return relationships;
    }

    public <V extends AbstractInheritableToscaType> V getToscaType(String type, TypeMap typeMap, Set<CSARDependency> dependencies, Class<V> clazz) {
        V indexedToscaElement = typeMap.get(clazz, type);
        if (indexedToscaElement == null) {
            indexedToscaElement = csarSearchService.getElementInDependencies(clazz, type, dependencies);
            if (indexedToscaElement == null) {
                throw new NotFoundException("Type <" + type + "> required in the topology cannot be found in the repository.");
            }
            typeMap.put(type, indexedToscaElement);
        }
        return indexedToscaElement;
    }

    @SuppressWarnings("unchecked")
    private <V extends AbstractInheritableToscaType> void fillType(TypeMap typeMap, Topology topology, AbstractTemplate template, IPaaSTemplate<V> paaSTemplate,
            Class<V> clazz) {
        V indexedToscaElement = getToscaType(template.getType(), typeMap, topology.getDependencies(), clazz);
        paaSTemplate.setIndexedToscaElement(indexedToscaElement);
        List<String> derivedFroms = indexedToscaElement.getDerivedFrom();
        List<V> derivedFromTypes = Lists.newArrayList();
        if (derivedFroms != null) {
            for (String derivedFrom : derivedFroms) {
                derivedFromTypes.add(getToscaType(derivedFrom, typeMap, topology.getDependencies(), clazz));
            }
        }
        paaSTemplate.setDerivedFroms(derivedFromTypes);
        try {
            Path csarPath = repository.getCSAR(indexedToscaElement.getArchiveName(), indexedToscaElement.getArchiveVersion());
            paaSTemplate.setCsarPath(csarPath);
        } catch (AlreadyExistException e) {
            log.debug("No csarPath for " + indexedToscaElement + "; not setting in " + paaSTemplate);
        }
    }

    /**
     * For every templates (nodes and relationship), check the attributes and interfaces operations input parameters for usage of the get_operation_output
     * function.
     * if found, register the referenced output on the related operation
     *
     * @param paaSNodeTemplates
     */
    private void processOperationsOutputs(final Map<String, PaaSNodeTemplate> paaSNodeTemplates) {
        // TODO: try to cache already processed nodes
        for (PaaSNodeTemplate paaSNodeTemplate : paaSNodeTemplates.values()) {
            processAttributesForOperationOutputs(paaSNodeTemplate, paaSNodeTemplates);
            processOperationsInputsForOperationOutputs(paaSNodeTemplate, paaSNodeTemplates);
            // do the same for the relationships
            for (PaaSRelationshipTemplate paaSRelationshipTemplate : paaSNodeTemplate.getRelationshipTemplates()) {
                processAttributesForOperationOutputs(paaSRelationshipTemplate, paaSNodeTemplates);
                processOperationsInputsForOperationOutputs(paaSRelationshipTemplate, paaSNodeTemplates);
            }
        }
    }

    /**
     * Check attributes of a paaSNodeTemplate for get_operation_output usage, and register in the related operation the output name
     *
     * @param paaSNodeTemplate
     * @param paaSNodeTemplates
     */
    private void processAttributesForOperationOutputs(final IPaaSTemplate<? extends AbstractInstantiableToscaType> paaSNodeTemplate,
            final Map<String, PaaSNodeTemplate> paaSNodeTemplates) {
        Map<String, IValue> attributes = paaSNodeTemplate.getIndexedToscaElement().getAttributes();
        if (MapUtils.isEmpty(attributes)) {
            return;
        }
        for (Entry<String, IValue> attribute : attributes.entrySet()) {
            String name = attribute.getKey();
            IValue value = attribute.getValue();
            processIValueForOperationOutput(name, value, paaSNodeTemplate, paaSNodeTemplates, true);
        }
    }

    /**
     * Check operations input of every operations of all interfaces of a IPaaSTemplate for get_operation_output usage, and register in the related operation
     * the output name
     *
     * @param paaSTemplate
     * @param paaSNodeTemplates
     */
    private <V extends AbstractInstantiableToscaType> void processOperationsInputsForOperationOutputs(final IPaaSTemplate<V> paaSTemplate,
            final Map<String, PaaSNodeTemplate> paaSNodeTemplates) {
        Map<String, Interface> interfaces = ((AbstractInstantiableToscaType) paaSTemplate.getIndexedToscaElement()).getInterfaces();
        if (interfaces != null) {
            for (Interface interfass : interfaces.values()) {
                for (Operation operation : interfass.getOperations().values()) {
                    Map<String, IValue> inputsParams = operation.getInputParameters();
                    if (inputsParams != null) {
                        for (Entry<String, IValue> input : inputsParams.entrySet()) {
                            String name = input.getKey();
                            IValue value = input.getValue();
                            processIValueForOperationOutput(name, value, paaSTemplate, paaSNodeTemplates, false);
                        }
                    }
                }
            }
        }
    }

    private <V extends AbstractInstantiableToscaType> void processIValueForOperationOutput(String name, IValue iValue, final IPaaSTemplate<V> paaSTemplate,
            final Map<String, PaaSNodeTemplate> paaSNodeTemplates, final boolean fromAttributes) {
        if (iValue instanceof FunctionPropertyValue) {
            FunctionPropertyValue function = (FunctionPropertyValue) iValue;
            if (ToscaFunctionConstants.GET_OPERATION_OUTPUT.equals(function.getFunction())) {
                String formatedAttributeName = null;
                List<? extends IPaaSTemplate> paaSTemplates = FunctionEvaluator.getPaaSTemplatesFromKeyword(paaSTemplate, function.getTemplateName(),
                        paaSNodeTemplates);
                if (fromAttributes) {
                    // nodeId:attributeName
                    formatedAttributeName = AlienUtils.prefixWith(AlienUtils.COLON_SEPARATOR, name, paaSTemplate.getId());
                }
                registerOperationOutput(paaSTemplates, function.getInterfaceName(), function.getOperationName(), function.getElementNameToFetch(),
                        formatedAttributeName);
            }
        } else if (iValue instanceof ConcatPropertyValue) {
            ConcatPropertyValue concatFunction = (ConcatPropertyValue) iValue;
            for (IValue param : concatFunction.getParameters()) {
                processIValueForOperationOutput(name, param, paaSTemplate, paaSNodeTemplates, false);
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <V extends AbstractInstantiableToscaType> void registerOperationOutput(final List<? extends IPaaSTemplate> paaSTemplates,
            final String interfaceName, final String operationName, final String output, final String formatedAttributeName) {
        for (IPaaSTemplate<V> paaSTemplate : paaSTemplates) {
            if (paaSTemplate.getInterfaces() != null) {
                Interface interfass = MapUtils.getObject(paaSTemplate.getInterfaces(), (interfaceName));
                if (interfass != null && interfass.getOperations().containsKey(operationName)) {
                    OperationOutput toAdd = new OperationOutput(output);
                    if (StringUtils.isNotBlank(formatedAttributeName)) {
                        toAdd.getRelatedAttributes().add(formatedAttributeName);
                    }
                    interfass.getOperations().get(operationName).addOutput(toAdd);
                }
            }
        }
    }

}
