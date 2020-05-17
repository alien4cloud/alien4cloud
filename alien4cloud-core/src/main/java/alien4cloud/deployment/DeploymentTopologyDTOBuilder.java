package alien4cloud.deployment;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutor;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.OrchestratorDeploymentProperties;
import org.alien4cloud.alm.deployment.configuration.model.PreconfiguredInputsConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.SecretCredentialInfo;
import org.alien4cloud.alm.service.ServiceResourceService;
import org.alien4cloud.tosca.catalog.index.ICsarDependencyLoader;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.topology.TopologyDTOBuilder;
import org.springframework.stereotype.Service;

import alien4cloud.deployment.model.DeploymentSubstitutionConfiguration;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationTopologyVersion;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.PolicyLocationResourceTemplate;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationResourceTypes;
import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.utils.ReflectionUtil;

/**
 * Construct a deployment topology dto for rest api and ui consumption.
 */
@Service
public class DeploymentTopologyDTOBuilder implements IDeploymentTopologyBuilder {
    @Inject
    private TopologyDTOBuilder topologyDTOBuilder;
    @Inject
    private FlowExecutor flowExecutor;
    @Inject
    private ILocationResourceService locationResourceService;
    @Inject
    private ServiceResourceService serviceResourceService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Inject
    private ICsarDependencyLoader csarDependencyLoader;

    @Override
    @ToscaContextual
    public DeploymentTopologyDTO prepareDeployment(Topology topology, Application application, ApplicationEnvironment environment) {
        FlowExecutionContext executionContext = flowExecutor.executeDeploymentFlow(topology, application, environment);
        return build(executionContext);
    }

    @Override
    @ToscaContextual
    public DeploymentTopologyDTO prepareDeployment(Topology topology, Application application, ApplicationEnvironment environment,
            ApplicationTopologyVersion topologyVersion, IDeploymentConfigAction deploymentConfigAction) {
        // Execute the update
        deploymentConfigAction.execute(application, environment, topologyVersion, topology);

        FlowExecutionContext executionContext = flowExecutor.executeDeploymentFlow(topology, application, environment);
        return build(executionContext);
    }

    @Override
    @ToscaContextual
    public DeploymentTopologyDTO prepareDeployment(Topology topology, Supplier<FlowExecutionContext> contextSupplier) {
        return build(contextSupplier.get());
    }

    /**
     * Create a deployment topology dto from the context of the execution of a deployment flow.
     *
     * @param executionContext The deployment flow execution context.
     * @return The deployment topology.
     */
    private DeploymentTopologyDTO build(FlowExecutionContext executionContext) {
        // re-create the deployment topology object for api compatibility purpose
        DeploymentTopology deploymentTopology = new DeploymentTopology();
        ReflectionUtil.mergeObject(executionContext.getTopology(), deploymentTopology);
        deploymentTopology.setInitialTopologyId(executionContext.getTopology().getId());
        deploymentTopology.setEnvironmentId(executionContext.getEnvironmentContext().get().getEnvironment().getId());
        deploymentTopology.setVersionId(executionContext.getEnvironmentContext().get().getEnvironment().getTopologyVersion());

        DeploymentTopologyDTO deploymentTopologyDTO = new DeploymentTopologyDTO();
        topologyDTOBuilder.initTopologyDTO(deploymentTopology, deploymentTopologyDTO);

        // Convert log result to validation result.
        TopologyValidationResult validationResult = new TopologyValidationResult();
        for (AbstractTask task : executionContext.getLog().getInfos()) {
            validationResult.addInfo(task);
        }
        for (AbstractTask task : executionContext.getLog().getWarnings()) {
            validationResult.addWarning(task);
        }
        for (AbstractTask task : executionContext.getLog().getErrors()) {
            validationResult.addTask(task);
        }
        validationResult.setValid(validationResult.getTaskList() == null || validationResult.getTaskList().isEmpty());
        deploymentTopologyDTO.setValidation(validationResult);

        Optional<PreconfiguredInputsConfiguration> preconfiguredInputsConfiguration = executionContext.getConfiguration(PreconfiguredInputsConfiguration.class,
                DeploymentTopologyDTOBuilder.class.getSimpleName());
        if (!preconfiguredInputsConfiguration.isPresent()) {
            deploymentTopology.setPreconfiguredInputProperties(Maps.newHashMap());
        } else {
            deploymentTopology.setPreconfiguredInputProperties(preconfiguredInputsConfiguration.get().getInputs());
        }

        Optional<DeploymentInputs> inputsOptional = executionContext.getConfiguration(DeploymentInputs.class,
                DeploymentTopologyDTOBuilder.class.getSimpleName());
        if (!inputsOptional.isPresent()) {
            deploymentTopology.setDeployerInputProperties(Maps.newHashMap());
            deploymentTopology.setUploadedInputArtifacts(Maps.newHashMap());
        } else {
            deploymentTopology.setDeployerInputProperties(inputsOptional.get().getInputs());
            deploymentTopology.setUploadedInputArtifacts(inputsOptional.get().getInputArtifacts());
        }

        Optional<DeploymentMatchingConfiguration> matchingConfigurationOptional = executionContext.getConfiguration(DeploymentMatchingConfiguration.class,
                DeploymentTopologyDTOBuilder.class.getSimpleName());
        if (!matchingConfigurationOptional.isPresent()) {
            return deploymentTopologyDTO;
        }
        DeploymentMatchingConfiguration matchingConfiguration = matchingConfigurationOptional.get();

        deploymentTopology.setOrchestratorId(matchingConfiguration.getOrchestratorId());
        deploymentTopology.setLocationGroups(matchingConfiguration.getLocationGroups());
        deploymentTopologyDTO.setLocationPolicies(matchingConfiguration.getLocationIds());
        // Good enough approximation as it doesn't contains just the location dependencies.
        deploymentTopology.setLocationDependencies(executionContext.getTopology().getDependencies());

        DeploymentSubstitutionConfiguration substitutionConfiguration = new DeploymentSubstitutionConfiguration();
        substitutionConfiguration.setSubstitutionTypes(new LocationResourceTypes());
        // fill DTO with policies substitution stuffs
        fillDTOWithPoliciesSubstitutionConfiguration(executionContext, deploymentTopology, deploymentTopologyDTO, matchingConfiguration,
                substitutionConfiguration);

        // fill DTO with nodes substitution stuffs
        fillDTOWithNodesSubstitutionConfiguration(executionContext, deploymentTopology, deploymentTopologyDTO, matchingConfiguration,
                substitutionConfiguration);

        deploymentTopologyDTO.setAvailableSubstitutions(substitutionConfiguration);

        ApplicationEnvironment environment = executionContext.getEnvironmentContext().get().getEnvironment();
        OrchestratorDeploymentProperties orchestratorDeploymentProperties = executionContext
                .getConfiguration(OrchestratorDeploymentProperties.class, this.getClass().getSimpleName())
                .orElse(new OrchestratorDeploymentProperties(environment.getTopologyVersion(), environment.getId(), matchingConfiguration.getOrchestratorId()));
        deploymentTopology.setProviderDeploymentProperties(orchestratorDeploymentProperties.getProviderDeploymentProperties());

        deploymentTopologyDTO
                .setSecretCredentialInfos((List<SecretCredentialInfo>) executionContext.getExecutionCache().get(FlowExecutionContext.SECRET_CREDENTIAL));

        deploymentTopologyDTO.setUnprocessedTopology((Topology) executionContext.getExecutionCache().get(FlowExecutionContext.INITIAL_TOPOLOGY));

        return deploymentTopologyDTO;
    }

    private void fillDTOWithNodesSubstitutionConfiguration(FlowExecutionContext executionContext, DeploymentTopology deploymentTopology,
            DeploymentTopologyDTO deploymentTopologyDTO, DeploymentMatchingConfiguration matchingConfiguration,
            DeploymentSubstitutionConfiguration substitutionConfiguration) {
        // used by ui to know if a property is editable. This should however be done differently with a better v2 api.
        deploymentTopology.setOriginalNodes((Map<String, NodeTemplate>) executionContext.getExecutionCache().get(FlowExecutionContext.MATCHING_ORIGINAL_NODES));
        deploymentTopology.setSubstitutedNodes(matchingConfiguration.getMatchedLocationResources().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e-> e.getValue())));
        deploymentTopology
                .setMatchReplacedNodes((Map<String, NodeTemplate>) executionContext.getExecutionCache().get(FlowExecutionContext.MATCHING_REPLACED_NODES));

        // Restrict the map of LocationResourceTemplate to the ones that are actually substituted after matching.
        Map<String, LocationResourceTemplate> allLocationResourcesTemplates = (Map<String, LocationResourceTemplate>) executionContext.getExecutionCache()
                .get(FlowExecutionContext.MATCHED_NODE_LOCATION_TEMPLATES_BY_ID_MAP);
        Map<String, LocationResourceTemplate> substitutedLocationResourceTemplate = Maps.newHashMap(); //
        matchingConfiguration.getMatchedLocationResources().values().forEach((resourceMapping) -> substitutedLocationResourceTemplate.put(resourceMapping,
                safe(allLocationResourcesTemplates).get(resourceMapping)));
        deploymentTopologyDTO.setLocationResourceTemplates(substitutedLocationResourceTemplate);

        substitutionConfiguration.setAvailableSubstitutions(
                (Map<String, Set<String>>) executionContext.getExecutionCache().get(FlowExecutionContext.SELECTED_MATCH_NODE_LOCATION_TEMPLATE_BY_NODE_ID_MAP));
        substitutionConfiguration.setSubstitutionsTemplates(allLocationResourcesTemplates);
        // Fetch all required types associated with the location substitution templates.
        substitutionConfiguration.getSubstitutionTypes()
                .addFrom(locationResourceService.getLocationResourceTypes(safe(substitutionConfiguration.getSubstitutionsTemplates()).values()));
        enrichSubstitutionTypesWithServicesDependencies(safe(substitutionConfiguration.getSubstitutionsTemplates()).values(),
                substitutionConfiguration.getSubstitutionTypes());
    }

    private void fillDTOWithPoliciesSubstitutionConfiguration(FlowExecutionContext executionContext, DeploymentTopology deploymentTopology,
            DeploymentTopologyDTO deploymentTopologyDTO, DeploymentMatchingConfiguration matchingConfiguration,
            DeploymentSubstitutionConfiguration substitutionConfiguration) {
        // used by ui to know if a property is editable. This should however be done differently with a better v2 api.
        deploymentTopology
                .setOriginalPolicies((Map<String, PolicyTemplate>) executionContext.getExecutionCache().get(FlowExecutionContext.MATCHING_ORIGINAL_POLICIES));
        deploymentTopology.setSubstitutedPolicies(matchingConfiguration.getMatchedPolicies().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e-> e.getValue())));

        // Restrict the map of PolicyLocationResourceTemplate to the ones that are actually substituted after matching.
        Map<String, PolicyLocationResourceTemplate> allResourcesTemplates = (Map<String, PolicyLocationResourceTemplate>) executionContext.getExecutionCache()
                .get(FlowExecutionContext.MATCHED_POLICY_LOCATION_TEMPLATES_BY_ID_MAP);
        Map<String, PolicyLocationResourceTemplate> substitutedResourceTemplates = Maps.newHashMap(); //
        safe(matchingConfiguration.getMatchedPolicies()).values()
                .forEach((resourceMapping) -> substitutedResourceTemplates.put(resourceMapping, safe(allResourcesTemplates).get(resourceMapping)));
        deploymentTopologyDTO.setPolicyLocationResourceTemplates(substitutedResourceTemplates);

        substitutionConfiguration.setAvailablePoliciesSubstitutions((Map<String, Set<String>>) executionContext.getExecutionCache()
                .get(FlowExecutionContext.SELECTED_MATCH_POLICY_LOCATION_TEMPLATE_BY_NODE_ID_MAP));
        substitutionConfiguration.setSubstitutionsPoliciesTemplates(allResourcesTemplates);
        // Fetch all required types associated with the location substitution templates.
        substitutionConfiguration.getSubstitutionTypes().addFrom(
                locationResourceService.getPoliciesLocationResourceTypes(safe(substitutionConfiguration.getSubstitutionsPoliciesTemplates()).values()));
    }

    /**
     * Enrich {@link LocationResourceTypes} adding types coming from on demand service resources.
     */
    private void enrichSubstitutionTypesWithServicesDependencies(Collection<LocationResourceTemplate> resourceTemplates,
            LocationResourceTypes locationResourceTypes) {
        Set<String> serviceTypes = Sets.newHashSet();
        Set<CSARDependency> dependencies = Sets.newHashSet();
        for (LocationResourceTemplate resourceTemplate : resourceTemplates) {
            if (resourceTemplate.isService()) {
                String serviceId = resourceTemplate.getId();
                ServiceResource serviceResource = serviceResourceService.getOrFail(serviceId);

                NodeType serviceType = ToscaContext.get(NodeType.class, serviceResource.getNodeInstance().getNodeTemplate().getType());
                if (serviceType == null || !serviceType.getArchiveVersion().equals(serviceResource.getNodeInstance().getTypeVersion())) {
                    serviceType = toscaTypeSearchService.findOrFail(NodeType.class, serviceResource.getNodeInstance().getNodeTemplate().getType(),
                            serviceResource.getNodeInstance().getTypeVersion());
                }
                dependencies.addAll(csarDependencyLoader.getDependencies(serviceType.getArchiveName(), serviceType.getArchiveVersion()));
                dependencies.add(new CSARDependency(serviceType.getArchiveName(), serviceType.getArchiveVersion()));
                serviceTypes.add(serviceResource.getNodeInstance().getNodeTemplate().getType());
            }
        }
        locationResourceService.fillLocationResourceTypes(serviceTypes, locationResourceTypes, dependencies);
    }
}