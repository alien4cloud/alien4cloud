package alien4cloud.deployment;

import alien4cloud.exception.ApplicationVersionNotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.deployment.DeploymentSetup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.InputArtifactUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.elasticsearch.common.collect.Lists;

import java.util.*;

/**
 * Created by luc on 09/09/2015.
 */
public class DeploymentSetupServiceOLDMATCH {

//    /**
//     * Get the deployment setup
//     * (environment right check done before method call)
//     *
//     * @param applicationId
//     * @param applicationEnvironmentId
//     * @return
//     */
//    public DeploymentSetupMatchInfo getDeploymentSetupMatchInfo(String applicationId, String applicationEnvironmentId) {
//        // get the topology from the version and the cloud from the environment
//        ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, applicationEnvironmentId);
//        if (applicationVersionService.get(environment.getCurrentVersionId()) == null) {
//            throw new ApplicationVersionNotFoundException("An application version is required by an application environment.");
//        }
//        ApplicationVersion version = applicationVersionService.getVersionByIdOrDefault(applicationId, environment.getCurrentVersionId());
//        Topology topology = topologyServiceCore.getOrFail(version.getTopologyId());
//        return preProcessTopologyAndMatch(topology, environment, version);
//    }
//
//    public DeploymentSetupMatchInfo preProcessTopologyAndMatch(Topology topology, ApplicationEnvironment environment, ApplicationVersion version) {
//        DeploymentSetup deploymentSetup = get(version, environment);
//        if (deploymentSetup == null) {
//            deploymentSetup = createOrFail(version, environment);
//        }
//        topologyCompositionService.processTopologyComposition(topology);
//        generateInputProperties(deploymentSetup, topology);
//        inputsPreProcessorService.processGetInput(deploymentSetup, topology, environment);
//        processInputArtifacts(topology);
//        if (environment.getCloudId() != null) {
//            Cloud cloud = cloudService.getMandatoryCloud(environment.getCloudId());
//            try {
//                if (deploymentSetup.getProviderDeploymentProperties() == null) {
//                    generatePropertyDefinition(deploymentSetup, cloud);
//                }
//                return generateCloudResourcesMapping(deploymentSetup, topology, cloud, true);
//            } catch (OrchestratorDisabledException e) {
//                log.warn("Cannot generate mapping for deployment setup as cloud is disabled, it will be re-generated next time");
//            }
//        }
//        return new DeploymentSetupMatchInfo(deploymentSetup);
//    }

//    /**
//     * Fill-in the inputs properties definitions (and default values) based on the properties definitions from the topology.
//     *
//     * @param deploymentSetup The deployment setup to impact.
//     * @param topology The topology that contains the inputs and properties definitions.
//     */
//    private void generateInputProperties(DeploymentSetup deploymentSetup, Topology topology) {
//        Map<String, String> inputProperties = deploymentSetup.getInputProperties();
//        Map<String, PropertyDefinition> inputDefinitions = topology.getInputs();
//        boolean changed = false;
//        if (inputDefinitions == null || inputDefinitions.isEmpty()) {
//            deploymentSetup.setInputProperties(null);
//            changed = inputProperties != null;
//        } else {
//            if (inputProperties == null) {
//                inputProperties = Maps.newHashMap();
//                deploymentSetup.setInputProperties(inputProperties);
//                changed = true;
//            } else {
//                Iterator<Map.Entry<String, String>> inputPropertyEntryIterator = inputProperties.entrySet().iterator();
//                while (inputPropertyEntryIterator.hasNext()) {
//                    Map.Entry<String, String> inputPropertyEntry = inputPropertyEntryIterator.next();
//                    if (!inputDefinitions.containsKey(inputPropertyEntry.getKey())) {
//                        inputPropertyEntryIterator.remove();
//                        changed = true;
//                    } else {
//                        try {
//                            constraintPropertyService.checkSimplePropertyConstraint(inputPropertyEntry.getKey(), inputPropertyEntry.getValue(),
//                                    inputDefinitions.get(inputPropertyEntry.getKey()));
//                        } catch (ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
//                            // Property is not valid anymore for the input, remove the old value
//                            inputPropertyEntryIterator.remove();
//                            changed = true;
//                        }
//                    }
//                }
//            }
//            for (Map.Entry<String, PropertyDefinition> inputDefinitionEntry : inputDefinitions.entrySet()) {
//                String existingValue = inputProperties.get(inputDefinitionEntry.getKey());
//                if (existingValue == null) {
//                    String defaultValue = inputDefinitionEntry.getValue().getDefault();
//                    if (defaultValue != null) {
//                        inputProperties.put(inputDefinitionEntry.getKey(), defaultValue);
//                        changed = true;
//                    }
//                }
//            }
//        }
//        if (changed) {
//            alienDAO.save(deploymentSetup);
//        }
//    }
//
//    /**
//     * Try to generate resources mapping for deployment setup from the topology and the cloud.
//     * If no value has been chosen this method will generate default value.
//     * If existing configuration is no longer valid for the topology and the cloud, this method will correct incompatibility
//     *
//     * @param deploymentSetup the deployment setup to generate configuration for
//     * @param topology the topology
//     * @param cloud the cloud
//     * @param automaticSave automatically save the deployment setup if it has been changed
//     * @return true if the topology's deployment setup is valid (all resources are matchable), false otherwise
//     */
//    public DeploymentSetupMatchInfo generateCloudResourcesMapping(DeploymentSetup deploymentSetup, Topology topology, Cloud cloud, boolean automaticSave)
//            throws OrchestratorDisabledException {
//        CloudResourceMatcherConfig cloudResourceMatcherConfig = cloudService.getCloudResourceMatcherConfig(cloud);
//        Map<String, IndexedNodeType> types = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, true);
//        CloudResourceTopologyMatchResult matchResult = cloudResourceMatcherService.matchTopology(topology, cloud, cloudService.getPaaSProvider(cloud.getId()),
//                cloudResourceMatcherConfig, types);
//
//        // Generate default matching for compute template
//        MappingGenerationResult<ComputeTemplate> computeMapping = generateDefaultMapping(deploymentSetup.getCloudResourcesMapping(),
//                matchResult.getComputeMatchResult(), topology);
//
//        // Generate default matching for network
//        MappingGenerationResult<NetworkTemplate> networkMapping = generateDefaultMapping(deploymentSetup.getNetworkMapping(),
//                matchResult.getNetworkMatchResult(), topology);
//
//        // Generate default matching for storage
//        MappingGenerationResult<StorageTemplate> storageMapping = generateDefaultMapping(deploymentSetup.getStorageMapping(),
//                matchResult.getStorageMatchResult(), topology);
//
//        MappingGenerationResult<Set<AvailabilityZone>> groupMapping = generateDefaultAvailabilityZoneMapping(deploymentSetup.getAvailabilityZoneMapping(),
//                matchResult.getAvailabilityZoneMatchResult(), topology);
//
//        deploymentSetup.setCloudResourcesMapping(computeMapping.mapping);
//        deploymentSetup.setNetworkMapping(networkMapping.mapping);
//        deploymentSetup.setStorageMapping(storageMapping.mapping);
//        deploymentSetup.setAvailabilityZoneMapping(groupMapping.mapping);
//        if ((computeMapping.changed || networkMapping.changed || storageMapping.changed || groupMapping.changed) && automaticSave) {
//            alienDAO.save(deploymentSetup);
//        }
//        DeploymentSetupMatchInfo matchInfo = new DeploymentSetupMatchInfo(deploymentSetup);
//        matchInfo.setValid(computeMapping.valid && networkMapping.valid && storageMapping.valid && groupMapping.valid);
//        matchInfo.setMatchResult(matchResult);
//        return matchInfo;
//    }
//    /**
//     * Inject input artifacts in the corresponding nodes.
//     */
//    private void processInputArtifacts(Topology topology) {
//        if (topology.getInputArtifacts() != null && !topology.getInputArtifacts().isEmpty()) {
//            // we'll build a map inputArtifactId -> List<DeploymentArtifact>
//            Map<String, List<DeploymentArtifact>> artifactMap = Maps.newHashMap();
//            // iterate over nodes in order to remember all nodes referencing an input artifact
//            for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
//                if (nodeTemplate.getArtifacts() != null && !nodeTemplate.getArtifacts().isEmpty()) {
//                    for (DeploymentArtifact da : nodeTemplate.getArtifacts().values()) {
//                        String inputArtifactId = InputArtifactUtil.getInputArtifactId(da);
//                        if (inputArtifactId != null) {
//                            List<DeploymentArtifact> das = artifactMap.get(inputArtifactId);
//                            if (das == null) {
//                                das = Lists.newArrayList();
//                                artifactMap.put(inputArtifactId, das);
//                            }
//                            das.add(da);
//                        }
//                    }
//                }
//            }
//
//            for (Map.Entry<String, DeploymentArtifact> e : topology.getInputArtifacts().entrySet()) {
//                List<DeploymentArtifact> nodeArtifacts = artifactMap.get(e.getKey());
//                if (nodeArtifacts != null) {
//                    for (DeploymentArtifact nodeArtifact : nodeArtifacts) {
//                        nodeArtifact.setArtifactRef(e.getValue().getArtifactRef());
//                        nodeArtifact.setArtifactName(e.getValue().getArtifactName());
//                    }
//                }
//            }
//        }
//    }
//private MappingGenerationResult<Set<AvailabilityZone>> generateDefaultAvailabilityZoneMapping(Map<String, Set<AvailabilityZone>> mapping,
//                                                                                              Map<String, Collection<AvailabilityZone>> matchResult, Topology topology) {
//    boolean valid = true;
//    for (Map.Entry<String, ? extends Collection<AvailabilityZone>> matchResultEntry : matchResult.entrySet()) {
//        valid = valid && (matchResultEntry.getValue() != null && !matchResultEntry.getValue().isEmpty());
//    }
//    boolean changed = false;
//    if (mapping == null) {
//        mapping = Maps.newHashMap();
//        for (Map.Entry<String, Collection<AvailabilityZone>> matchResultEntry : matchResult.entrySet()) {
//            mapping.put(matchResultEntry.getKey(), getFirstZones(matchResultEntry.getValue(), 2));
//        }
//        changed = true;
//    } else {
//        // Try to remove unknown mapping from existing config
//        Iterator<Map.Entry<String, Set<AvailabilityZone>>> mappingEntryIterator = mapping.entrySet().iterator();
//        while (mappingEntryIterator.hasNext()) {
//            Map.Entry<String, Set<AvailabilityZone>> entry = mappingEntryIterator.next();
//            if (topology.getGroups() == null || !topology.getGroups().containsKey(entry.getKey()) || !matchResult.containsKey(entry.getKey())
//                    || !matchResult.get(entry.getKey()).containsAll(entry.getValue())) {
//                // Remove the mapping if topology do not contain the node with that name and of type compute
//                // Or the mapping do not exist anymore in the match result
//                changed = true;
//                mappingEntryIterator.remove();
//            }
//        }
//        for (Map.Entry<String, Collection<AvailabilityZone>> matchResultEntry : matchResult.entrySet()) {
//            if (!mapping.containsKey(matchResultEntry.getKey())) {
//                // Only take the first element as selected if no configuration has been set before
//                changed = true;
//                mapping.put(matchResultEntry.getKey(), getFirstZones(matchResultEntry.getValue(), 2));
//            }
//        }
//    }
//    return new MappingGenerationResult<>(mapping, valid, changed);
//}
//
//    private Set<AvailabilityZone> getFirstZones(Collection<AvailabilityZone> zones, int toBeTaken) {
//        Iterator<AvailabilityZone> matchedZones = zones.iterator();
//        Set<AvailabilityZone> defaultZones = Sets.newLinkedHashSet();
//        while (defaultZones.size() < toBeTaken && matchedZones.hasNext()) {
//            defaultZones.add(matchedZones.next());
//        }
//        return defaultZones;
//    }
//private <T> MappingGenerationResult<T> generateDefaultMapping(Map<String, T> mapping, Map<String, ? extends Collection<T>> matchResult, Topology topology) {
//    boolean valid = true;
//    for (Map.Entry<String, ? extends Collection<T>> matchResultEntry : matchResult.entrySet()) {
//        valid = valid && (matchResultEntry.getValue() != null && !matchResultEntry.getValue().isEmpty());
//    }
//    boolean changed = false;
//    if (mapping == null) {
//        changed = true;
//        mapping = Maps.newHashMap();
//    } else {
//        // Try to remove unknown mapping from existing config
//        Iterator<Map.Entry<String, T>> mappingEntryIterator = mapping.entrySet().iterator();
//        while (mappingEntryIterator.hasNext()) {
//            Map.Entry<String, T> entry = mappingEntryIterator.next();
//            if (topology.getNodeTemplates() == null || !topology.getNodeTemplates().containsKey(entry.getKey()) || !matchResult.containsKey(entry.getKey())
//                    || !matchResult.get(entry.getKey()).contains(entry.getValue())) {
//                // Remove the mapping if topology do not contain the node with that name and of type compute
//                // Or the mapping do not exist anymore in the match result
//                changed = true;
//                mappingEntryIterator.remove();
//            }
//        }
//    }
//    for (Map.Entry<String, ? extends Collection<T>> entry : matchResult.entrySet()) {
//        if (!entry.getValue().isEmpty() && !mapping.containsKey(entry.getKey())) {
//            // Only take the first element as selected if no configuration has been set before
//            changed = true;
//            mapping.put(entry.getKey(), entry.getValue().iterator().next());
//        }
//    }
//    return new MappingGenerationResult<>(mapping, valid, changed);
//}
//    /**
//     * Try to generate a default deployment properties
//     *
//     * @param deploymentSetup the deployment setup to generate configuration for
//     * @param cloud the cloud
//     */
//    public void generatePropertyDefinition(DeploymentSetup deploymentSetup, Cloud cloud) {
//        Map<String, PropertyDefinition> propertyDefinitionMap = cloudService.getDeploymentPropertyDefinitions(cloud.getId());
//        if (propertyDefinitionMap != null) {
//            // Reset deployment properties as it might have changed between cloud
//            Map<String, String> propertyValueMap = Maps.newHashMap();
//            for (Map.Entry<String, PropertyDefinition> propertyDefinitionEntry : propertyDefinitionMap.entrySet()) {
//                propertyValueMap.put(propertyDefinitionEntry.getKey(), propertyDefinitionEntry.getValue().getDefault());
//            }
//            deploymentSetup.setProviderDeploymentProperties(propertyValueMap);
//        }
//    }
}
