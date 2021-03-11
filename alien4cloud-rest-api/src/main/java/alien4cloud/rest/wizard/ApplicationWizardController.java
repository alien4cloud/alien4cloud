package alien4cloud.rest.wizard;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.images.IImageDAO;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.rest.wizard.model.WizardAddon;
import alien4cloud.rest.wizard.model.WizardFeature;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.rest.component.ComponentSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.wizard.model.ApplicationModule;
import alien4cloud.rest.wizard.model.ApplicationOverview;
import alien4cloud.rest.wizard.model.MetaProperty;
import alien4cloud.rest.wizard.model.TopologyOverview;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.webconfiguration.WizardAddonsScanner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.catalog.index.ArchiveIndexer;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.topology.TopologyDTOBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service that allows managing applications.
 */
@Slf4j
@RestController
//@ConfigurationProperties(prefix = "wizard.overview")
@RequestMapping({"/rest/wizard", "/rest/v1/wizard", "/rest/latest/wizard"})
@Api(value = "", description = "Operations on Applications")
public class ApplicationWizardController {

    @Resource
    private ApplicationWizardConfiguration applicationWizardConfiguration;

    @Resource
    private WizardAddonsScanner wizardAddonsScanner;

    @Resource
    private IImageDAO imageDAO;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    ArchiveIndexer archiveIndexer;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private TopologyService topologyService;
    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;
    @Inject
    private TopologyDTOBuilder topologyDTOBuilder;
    @Resource
    private IToscaTypeSearchService toscaTypeSearchService;

    private static final Pattern NUMBER_DETECTION_PATTERN = Pattern.compile("(.*\\D+)(\\d+)");

    @RequestMapping(value = "/applications/suggestion/{applicationName:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<String> getSuggestedName(@PathVariable String applicationName) {
        String generatedName = generateUniqueApplicationName(applicationName);
        return RestResponseBuilder.<String> builder().data(generatedName).build();
    }

    private String generateUniqueApplicationName(String suggestedName) {
        try {
            applicationService.checkApplicationName(suggestedName);
            return suggestedName;
        } catch(AlreadyExistException e) {
            log.debug("Application name already used: " + suggestedName);
        }

        String generatedName = suggestedName;
        Matcher m = NUMBER_DETECTION_PATTERN.matcher(suggestedName);
        if (m.matches()) {
            String prefixeName = m.group(1);
            Integer suffixeNumber = Integer.parseInt(m.group(2));
            generatedName = prefixeName + (++suffixeNumber);
        } else {
            generatedName += "2";
        }

        return generateUniqueApplicationName(generatedName);
    }

        /**
         * Get an application from it's id.
         *
         * @param applicationId The application id.
         */
    @ApiOperation(value = "Get an application based from its id.", notes = "Returns the application details. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/applications/overview/{applicationId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<ApplicationOverview> get(@PathVariable String applicationId) {
        return _getApplicationEnvironmentOverview(applicationId, null);
    }

    /**
     * Get an application from it's id.
     *
     * @param applicationId The application id.
     */
    @ApiOperation(value = "Get an application based from its id.", notes = "Returns the application details. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/applications/overview/{applicationId:.+}/{environmentId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<ApplicationOverview> getApplicationEnvironmentOverview(@PathVariable String applicationId, @PathVariable String environmentId) {
        return _getApplicationEnvironmentOverview(applicationId, environmentId);
    }

    private RestResponse<ApplicationOverview> _getApplicationEnvironmentOverview(String applicationId, String environmentId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationOverview overview = new ApplicationOverview();
        overview.setComponentCategories(applicationWizardConfiguration.getComponentCategories());

        overview.setNamedMetaProperties(getNamedMetaProperties(application.getMetaProperties(), applicationWizardConfiguration.getApplicationOverviewMetapropertiesSet()));

        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, environmentId);
        overview.setApplicationEnvironment(applicationEnvironment);
        DeploymentStatus status = null;
        try {
            overview.setDeploymentStatus(applicationEnvironmentService.getStatus(applicationEnvironment));
        } catch (Exception e) {
            overview.setDeploymentStatus(DeploymentStatus.UNKNOWN);
        }

        overview.setDescription(application.getDescription());
        overview.setApplication(application);

        Topology topology = topologyServiceCore.getOrFail(applicationEnvironment.getApplicationId() + ":" + applicationEnvironment.getTopologyVersion());
//        overview.setTopologyId(applicationEnvironment.getApplicationId());
//        overview.setTopologyVersion(applicationEnvironment.getTopologyVersion());
        overview.setComponentsPerCategory(getModulesPerCatgory(topology));

        TopologyDTO topologyDTO = topologyDTOBuilder.initTopologyDTO(topology, new TopologyDTO());
        overview.setTopologyDTO(topologyDTO);

        return RestResponseBuilder.<ApplicationOverview>builder().data(overview).build();
    }

    @ApiOperation(value = "Get an application based from its id.", notes = "Returns the application details. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/topologies/overview/{topologyId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<TopologyOverview> getTopologyOverview(@PathVariable String topologyId) {
        TopologyOverview overview = new TopologyOverview();
        overview.setComponentCategories(applicationWizardConfiguration.getComponentCategories());
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        overview.setNamedMetaProperties(getNamedMetaProperties(topology.getMetaProperties(), applicationWizardConfiguration.getTopologyOverviewMetapropertiesSet()));
        overview.setDescription(topology.getDescription());
        TopologyDTO topologyDTO = topologyDTOBuilder.initTopologyDTO(topology, new TopologyDTO());
        overview.setTopologyDTO(topologyDTO);
//        overview.setTopologyId(topology.getArchiveName());
//        overview.setTopologyVersion(topology.getArchiveVersion());
        overview.setComponentsPerCategory(getModulesPerCatgory(topology));
        return RestResponseBuilder.<TopologyOverview>builder().data(overview).build();
    }

    @ApiOperation(value = "Get the list of available wizard addons.", notes = "An addon can be available but not allowed for a given user, depending on the roles")
    @RequestMapping(value = "/addons", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<List<WizardFeature>> getAddons() {
        List<WizardFeature> result = Lists.newArrayList();
        for (WizardAddon addon : wizardAddonsScanner.getAddons().values()) {

            WizardFeature feature = new WizardFeature();
            feature.setId(addon.getId());
            feature.setIconName(addon.getIconName());
            feature.setActivationLink(addon.getContextPath());
            feature.setAllowed(AuthorizationUtil.hasOneRoleIn(addon.getAuthorizedRoles()));
            feature.setEnabled(true);

            result.add(feature);
        }
        return RestResponseBuilder.<List<WizardFeature>>builder().data(result).build();
    }

    private Map<String, List<ApplicationModule>> getModulesPerCatgory(Topology topology) {
        Map<String, List<ApplicationModule>> modulesPerCategory = Maps.newHashMap();

        Map<String, NodeType> indexedNodeTypesFromTopology = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, true, false);
        if (topology.getNodeTemplates() != null) {

            topology.getNodeTemplates().forEach((name, nodeTemplate) -> {

                NodeType nodeType = indexedNodeTypesFromTopology.get(name);
                List<MetaProperty> namedMetaProperties = getNamedMetaProperties(nodeType.getMetaProperties(), null);
                Map<String, String> metaPropertyValues = Maps.newHashMap();
                namedMetaProperties.forEach(metaProperty -> metaPropertyValues.put(metaProperty.getConfiguration().getName(), metaProperty.getValue()));

                applicationWizardConfiguration.getComponentFilterByCategorySet().forEach((categoryName, filterConfig) -> {
                    boolean moduleAdded = false;

                    List<ApplicationModule> modules = modulesPerCategory.get(categoryName);
                    if (modules == null) {
                        modules = Lists.newArrayList();
                        modulesPerCategory.put(categoryName, modules);
                    }

                    if (filterConfig.isEmpty()) {
                        moduleAdded = true;
                    } else {
                        Iterator<Map.Entry<String, Set<String>>> entryIterator = filterConfig.entrySet().iterator();
                        boolean passFilter = true;
                        while (entryIterator.hasNext() && passFilter) {
                            Map.Entry<String, Set<String>> filterEntry = entryIterator.next();
                            if (!metaPropertyValues.containsKey(filterEntry.getKey()) || !filterEntry.getValue().contains(metaPropertyValues.get(filterEntry.getKey()))) {
                                passFilter = false;
                            }
                        }
                        moduleAdded = passFilter;
                    }

                    if (moduleAdded) {
                        ApplicationModule applicationModule = new ApplicationModule();
                        applicationModule.setNodeName(name);
                        applicationModule.setNodeType(nodeType);
                        applicationModule.setNamedMetaProperties(getNamedMetaProperties(nodeType.getMetaProperties(), applicationWizardConfiguration.getComponentOverviewMetapropertiesSet()));
                        modules.add(applicationModule);
                    }

                });

            });
        }
        return modulesPerCategory;
    }

    private List<MetaProperty> getNamedMetaProperties(Map<String, String> metaProperties, Set<String> metaPropertiesFilter) {
        List<MetaProperty> namedMetaProperties = Lists.newArrayList();
        if (metaProperties != null) {
            metaProperties.forEach((id, value) -> {
                MetaPropConfiguration configuration = dao.findById(MetaPropConfiguration.class, id);
                // filter returned meta properties for applications
                if (metaPropertiesFilter == null || metaPropertiesFilter.isEmpty() || metaPropertiesFilter.contains(configuration.getName())) {
                    namedMetaProperties.add(new MetaProperty(configuration, value));
                }
            });
        }
        return namedMetaProperties;
    }

    /**
     * Search for TOSCA elements.
     *
     * @param searchRequest The search request.
     * @return A {@link RestResponse} that contains a {@link FacetedSearchResult} of {@link NodeType}.
     */
    @ApiOperation(value = "Search for components (tosca types) in alien.")
    @RequestMapping(value = "/components/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<FacetedSearchResult<? extends AbstractToscaType>> search(@RequestBody ComponentSearchRequest searchRequest) {
        final Map<String, String[]> filters = (searchRequest.getFilters() == null) ? Maps.newHashMap() : searchRequest.getFilters();
        applicationWizardConfiguration.getAllComponentFiltersSet().forEach((metaPropertyKey, values) -> {
            String[] filetred = filters.get("metaProperties." + metaPropertyKey);
            if (filetred == null) {
                filetred = new String[]{};
                filters.put("metaProperties." + metaPropertyKey, values.toArray(filetred));
            } else {
                Set<String> filetredSet = Sets.newHashSet(filetred);
                filetredSet.addAll(values);
                String[] merged = new String[]{};
                filters.put("metaProperties." + metaPropertyKey, filetredSet.toArray(merged));
            }
        });
        Class<? extends AbstractToscaType> queryClass = searchRequest.getType() == null ? AbstractToscaType.class
                : searchRequest.getType().getIndexedToscaElementClass();
        FacetedSearchResult<? extends AbstractToscaType> searchResult = toscaTypeSearchService.search(queryClass, searchRequest.getQuery(),
                Integer.MAX_VALUE, filters);
        return RestResponseBuilder.<FacetedSearchResult<? extends AbstractToscaType>> builder().data(searchResult).build();
    }



}
