package alien4cloud.cloud;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.QueryHelper;
import org.elasticsearch.mapping.QueryHelper.SearchQueryHelperBuilder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.cloud.ActivableComputeTemplate;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudConfiguration;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.cloud.CloudResourceType;
import alien4cloud.model.cloud.ICloudResourceTemplate;
import alien4cloud.model.cloud.NetworkTemplate;
import alien4cloud.model.cloud.StorageTemplate;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.IConfigurablePaaSProvider;
import alien4cloud.paas.IConfigurablePaaSProviderFactory;
import alien4cloud.paas.IDeploymentParameterizablePaaSProviderFactory;
import alien4cloud.paas.IPaaSProvider;
import alien4cloud.paas.IPaaSProviderFactory;
import alien4cloud.paas.ITemplateManagedPaaSProvider;
import alien4cloud.paas.PaaSProviderFactoriesService;
import alien4cloud.paas.PaaSProviderService;
import alien4cloud.paas.exception.CloudDisabledException;
import alien4cloud.paas.exception.PaaSTechnicalException;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.paas.model.PaaSComputeTemplate;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.MappingUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Slf4j
@Component
@DependsOn("plugin-manager")
public class CloudService {
    @Resource
    private QueryHelper queryHelper;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private PaaSProviderFactoriesService paaSProviderFactoriesService;
    @Resource
    private PaaSProviderService paaSProviderService;
    @Resource
    private CloudImageService cloudImageService;

    public void initialize() {
        int from = 0;
        long totalResult;
        do {
            GetMultipleDataResult<Cloud> enabledCloudResult = get(null, true, from, 20, null);
            if (enabledCloudResult.getData() == null) {
                return;
            }

            for (Cloud cloud : enabledCloudResult.getData()) {
                try {
                    initCloud(cloud);
                } catch (Throwable t) {
                    // we have to catch everything as we don't know what a plugin can do here and cannot interrupt startup.
                    disableOnInitFailure(cloud, t);
                }
            }
            from += enabledCloudResult.getData().length;
            totalResult = enabledCloudResult.getTotalResults();
        } while (from < totalResult);
    }

    private void disableOnInitFailure(Cloud cloud, Throwable t) {
        log.error("Failed to start cloud - will switch it to disabled", t);
        disableCloud(cloud);
    }

    /**
     * Create a new cloud instance.
     *
     * @param cloud The cloud to create.
     */
    public synchronized String create(Cloud cloud) {
        // check that the cloud doesn't already exists
        if (alienDAO.count(Cloud.class, QueryBuilders.termQuery("name", cloud.getName())) > 0) {
            throw new AlreadyExistException("a cloud with the given name already exists.");
        }

        // generate an unique id
        cloud.setId(UUID.randomUUID().toString());
        // by default clouds are disabled as it should be configured before being enabled.
        cloud.setEnabled(false);

        // save default configuration
        IPaaSProviderFactory passProviderFactory = paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean());
        if (passProviderFactory instanceof IConfigurablePaaSProviderFactory) {
            saveDefaultConfiguration(cloud.getId(), ((IConfigurablePaaSProviderFactory) passProviderFactory).getDefaultConfiguration());
            cloud.setConfigurable(true);
        } else {
            cloud.setConfigurable(false);
        }

        alienDAO.save(cloud);
        return cloud.getId();
    }

    private void initializeMatcherConfig(IPaaSProvider provider, Cloud cloud) {
        // If the provider manually match resources, then we should update it with saved configuration from Alien
        log.info("Loading matching configuration for cloud <{}>.", cloud.getName());
        CloudResourceMatcherConfig config = getCloudResourceMatcherConfig(cloud);
        provider.updateMatcherConfig(config);
    }

    @SuppressWarnings("rawtypes")
    private void saveDefaultConfiguration(String cloudId, Object defaultConfiguration) {
        CloudConfiguration configuration = new CloudConfiguration(cloudId, defaultConfiguration);
        alienDAO.save(configuration);
    }

    /**
     * Update an exising cloud.
     *
     * @param updated The cloud to update.
     */
    public synchronized void update(Cloud updated) {
        // ensure the current cloud exists.
        Cloud current = getMandatoryCloud(updated.getId());

        // Some data cannot be updated so we just update the authorized fields.
        if (updated.getName() != null && !updated.getName().equals(current.getName())) {
            if (alienDAO.count(Cloud.class, QueryBuilders.termQuery("name", updated.getName())) > 0) {
                throw new AlreadyExistException("a cloud with the given name already exists.");
            }
            current.setName(updated.getName());
        }
        if (updated.getGroupRoles() != null) {
            current.setGroupRoles(updated.getGroupRoles());
        }
        if (updated.getUserRoles() != null) {
            current.setUserRoles(updated.getUserRoles());
        }
        if (updated.getEnvironmentType() != null) {
            current.setEnvironmentType(updated.getEnvironmentType());
        }
        if (updated.getIaaSType() != null) {
            current.setIaaSType(updated.getIaaSType());
        }

        alienDAO.save(current);
    }

    /**
     * Delete an existing cloud. Note: the cloud must be unused in order to be deleted.
     *
     * @param id The cloud to delete.
     * @return true if the plugin has been successfully deleted, false if deletion could not be done because plugin wasn't disabled.
     */
    public synchronized boolean delete(String id) {
        // checks that the cloud is not used anymore
        if (disableCloud(id)) {
            removeCloudIdInApplicationEnvironment(id);
            alienDAO.delete(Cloud.class, id);
            alienDAO.delete(CloudConfiguration.class, id);
            return true;
        }
        return false;
    }

    /**
     * Remove the reference to the deleted cloud id in {ApplicationEnvironment}.
     *
     * @param id
     */
    private void removeCloudIdInApplicationEnvironment(String id) {
        GetMultipleDataResult<ApplicationEnvironment> result = alienDAO.find(ApplicationEnvironment.class,
                MapUtil.newHashMap(new String[] { "cloudId" }, new String[][] { new String[] { id } }), Integer.MAX_VALUE);
        for (ApplicationEnvironment env : result.getData()) {
            env.setCloudId(null);
            log.debug("The reference of cloud " + id + " has been deleted in application environment " + id);
            alienDAO.save(env);
        }
    }

    /**
     * Get a specific cloud based on it's id.
     *
     * @param id The id of the cloud to get.
     * @return The actual cloud object.
     */
    public Cloud get(String id) {
        return alienDAO.findById(Cloud.class, id);
    }

    /**
     * Get a specific cloud based on it's name.
     *
     * @param name The name of the cloud to get.
     * @return The actual cloud object.
     */
    public Cloud getByName(String name) {
        return alienDAO.customFind(Cloud.class, QueryBuilders.termQuery("name", name));
    }

    /**
     * Get multiple clouds.
     *
     * @param query The query to apply to filter clouds.
     * @param from The start index of the query.
     * @param size The maximum number of elements to return.
     * @param authorizationFilter authorization filter
     * @return A {@link GetMultipleDataResult} that contains cloud objects.
     */
    public GetMultipleDataResult<Cloud> get(String query, boolean enabledOnly, int from, int size, FilterBuilder authorizationFilter) {
        Map<String, String[]> filters = null;
        if (enabledOnly) {
            filters = MapUtil.newHashMap(new String[] { "enabled" }, new String[][] { new String[] { "true" } });
        }
        return alienDAO.search(Cloud.class, query, filters, authorizationFilter, null, from, size);
    }

    /**
     * Get the map of deployment property definitions for the given cloud.
     *
     * @param id Id of the cloud for which to get deployment property definitions.
     * @return The map of property definitions for the given cloud.
     */
    public Map<String, PropertyDefinition> getDeploymentPropertyDefinitions(String id) {
        Cloud cloud = getMandatoryCloud(id);
        IPaaSProviderFactory<IPaaSProvider> providerFactory = paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean());
        if (providerFactory instanceof IDeploymentParameterizablePaaSProviderFactory) {
            return ((IDeploymentParameterizablePaaSProviderFactory) providerFactory).getDeploymentPropertyDefinitions();
        } else {
            return null;
        }
    }

    /**
     * Get the current configuration for a given cloud.
     *
     * @param id Id of the cloud for which to get the configuration.
     * @return The current configuration object for the given cloud.
     */
    public Object getConfiguration(String id) {
        CloudConfiguration configuration = alienDAO.findById(CloudConfiguration.class, id);
        if (configuration == null) {
            return null;
        }
        return configuration.getConfiguration();
    }

    /**
     * Get the type of the configuration object for a given cloud.
     *
     * @param id Id of the cloud for which to get the configuration object's type.
     * @return The type (class) of the configuration object for the given cloud or null if the cloud doesn't have configuration.
     */
    public Class<?> getConfigurationType(String id) {
        Cloud cloud = getMandatoryCloud(id);
        if (!cloud.isConfigurable()) {
            return null;
        }

        return getConfigurationType(cloud);
    }

    private Class<?> getConfigurationType(Cloud cloud) {
        return ((IConfigurablePaaSProviderFactory) paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean()))
                .getConfigurationType();
    }

    /**
     * Update the configuration for the given cloud.
     *
     * @param id Id of the cloud for which to update the configuration.
     * @param newConfiguration The new configuration.
     */
    public synchronized void updateConfiguration(String id, Object newConfiguration) {
        CloudConfiguration configuration = alienDAO.findById(CloudConfiguration.class, id);
        if (configuration == null) {
            throw new NotFoundException("No configuration exists for cloud [" + id + "].");
        }
        configuration.setConfiguration(newConfiguration);
        alienDAO.save(configuration);
    }

    /**
     * Enable a cloud, this will configure an instance of a {@link IPaaSProvider} and register it to the PaaSProviderService that manages monitoring.
     *
     * @param id Id of the cloud to enable.
     * @throws PluginConfigurationException In case the configuration is not valid for the plugin.
     */
    public synchronized void enableCloud(String id) throws PluginConfigurationException {
        Cloud cloud = getMandatoryCloud(id);

        if (cloud.isEnabled()) {
            // cloud is already enabled.
            return;
        }

        initCloud(cloud);

        // set the cloud as enabled.
        cloud.setEnabled(true);
        alienDAO.save(cloud);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initCloud(Cloud cloud) throws PluginConfigurationException {
        log.info("Enable cloud <{}> <{}>", cloud.getId(), cloud.getName());

        // get a PaaSProvider bean and configure it.
        IPaaSProviderFactory passProviderFactory = paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean());
        // create and configure a IPaaSProvider instance.
        IPaaSProvider provider;
        if (passProviderFactory instanceof IConfigurablePaaSProviderFactory) {
            IConfigurablePaaSProvider<Object> cProvider = ((IConfigurablePaaSProviderFactory<Object>) passProviderFactory).newInstance();
            provider = cProvider;
            Object configuration = getConfiguration(cloud.getId());
            if (configuration != null) {
                Object validConfiguration;
                try {
                    validConfiguration = configurationAsValidObject(cloud, configuration);
                } catch (IOException e) {
                    throw new PluginConfigurationException("Failed convert configuration in object.", e);
                }
                cProvider.setConfiguration(validConfiguration);
            }
        } else {
            provider = passProviderFactory.newInstance();
        }
        initializeMatcherConfig(provider, cloud);
        // register the IPaaSProvider for the cloud.
        paaSProviderService.register(cloud.getId(), provider);
    }

    /**
     * Convert a configuration object as a Map of key/values into a valid object that has the type expected by the Cloud PaaSProvider.
     *
     * @param id The id of the cloud.
     * @param configuration The configuration as a Map of key/values.
     * @return An instance of the configuration object matching the PaaSProvider requested type.
     * @throws IOException If the configuration cannot be parsed.
     * @throws PluginConfigurationException In case the paas provider configuration type cannot be found.
     */
    public Object configurationAsValidObject(String id, Object configuration) throws IOException, PluginConfigurationException {
        Cloud cloud = getMandatoryCloud(id);
        return configurationAsValidObject(cloud, configuration);
    }

    private Object configurationAsValidObject(Cloud cloud, Object configuration) throws IOException, PluginConfigurationException {
        Class<?> configurationType = getConfigurationType(cloud);
        if (configurationType == null) {
            log.error("Cloud <" + cloud.getId() + "> using paas provider <" + cloud.getPaasProviderName()
                    + "> cannot have configuration set (paas provider plugin is not valid).");
            throw new PluginConfigurationException("Cloud <" + cloud.getId() + "> using paas provider <" + cloud.getPaasProviderName()
                    + "> cannot have configuration set (paas provider plugin is not valid).");
        }

        return JsonUtil.readObject(JsonUtil.toString(configuration), configurationType);
    }

    /**
     * Disable an existing cloud. Note that this can be done only when no more deployments are using the cloud to disable.
     *
     * @param id Id of the cloud to disable.
     * @return true if the cloud has been disabled, false if not (some deployments are using the cloud).
     */
    public synchronized boolean disableCloud(String id) {

        String index = alienDAO.getIndexForType(Deployment.class);
        SearchQueryHelperBuilder searchQueryHelperBuilder = queryHelper.buildSearchQuery(index).types(Deployment.class)
                .filters(MapUtil.newHashMap(new String[] { "cloudId", "endDate" }, new String[][] { new String[] { id }, new String[] { null } }))
                .fieldSort("_timestamp", true);

        GetMultipleDataResult<Object> result = alienDAO.search(searchQueryHelperBuilder, 0, 1);

        // TODO place a lock to avoid deployments during disablement of the cloud.
        if (result.getData().length > 0) {
            return false;
        }
        Cloud cloud = getMandatoryCloud(id);
        disableCloud(cloud);
        return true;
    }

    private void disableCloud(Cloud cloud) {
        try {
            // un-register the IPaaSProvider for the cloud.
            IPaaSProvider paaSProvider = paaSProviderService.unregister(cloud.getId());
            IPaaSProviderFactory passProviderFactory = paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean());
            if (paaSProvider != null) {
                passProviderFactory.destroy(paaSProvider);
            }
        } catch (Exception e) {
            log.info("Unable to destroy paaS provider, it may not be created yet", e);
        } finally {
            // Mark the cloud as disabled
            cloud.setEnabled(false);
            alienDAO.save(cloud);
        }
    }

    /**
     * Get a {@link IPaaSProvider} for a given cloud.
     *
     * @param cloudId Id of the cloud for which to get the {@link IPaaSProvider}.
     * @return The {@link IPaaSProvider} associated to the enabled cloud.
     * @throws CloudDisabledException In case the cloud is not enabled.
     */
    public IPaaSProvider getPaaSProvider(String cloudId) throws CloudDisabledException {
        IPaaSProvider paaSProvider = paaSProviderService.getPaaSProvider(cloudId);
        if (paaSProvider == null) {
            Cloud cloud = getMandatoryCloud(cloudId);
            if (cloud.isEnabled()) {
                throw new PaaSTechnicalException("Failed to find an active PaaSProvider for enabled cloud.");
            } else {
                throw new CloudDisabledException("Cloud is not enabled and no PaaSProvider instance has been created.");
            }
        }
        return paaSProvider;
    }

    /**
     * Get a cloud, fail if not exist
     *
     * @param id id of the cloud
     * @return the cloud with given id
     * @throws alien4cloud.exception.NotFoundException if the cloud does not exist
     */
    public Cloud getMandatoryCloud(String id) {
        Cloud cloud = alienDAO.findById(Cloud.class, id);
        if (cloud == null) {
            throw new NotFoundException("Cloud [" + id + "] doesn't exists.");
        }
        return cloud;
    }

    /**
     * Check if a flavor matches image's requirement
     *
     * @param image the image to verify compatibility
     * @param flavor the flavor to verify compatibility
     * @return true if flavor matches image's requirement, false otherwise
     */
    private boolean isFlavorMatchImageRequirement(CloudImage image, CloudImageFlavor flavor) {
        if (image.getRequirement() != null) {
            if (image.getRequirement().getNumCPUs() != null && image.getRequirement().getNumCPUs() > flavor.getNumCPUs()) {
                return false;
            }
            if (image.getRequirement().getDiskSize() != null && image.getRequirement().getDiskSize() > flavor.getDiskSize()) {
                return false;
            }
            if (image.getRequirement().getMemSize() != null && image.getRequirement().getMemSize() > flavor.getMemSize()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Add an image to the cloud consists of adding new available compute template and push update to our persistence layer
     *
     * @param cloud the cloud to update
     * @param cloudImageId the image to add
     */
    public void addCloudImage(Cloud cloud, String cloudImageId) {
        // The field images cannot be null
        if (cloud.getImages().contains(cloudImageId)) {
            throw new AlreadyExistException("Cloud image [" + cloudImageId + "] already exists");
        }
        cloud.getImages().add(cloudImageId);
        alienDAO.save(cloud);
    }

    /**
     * Add a flavor to the cloud consists of adding new available compute template and push update to our persistence layer
     *
     * @param cloud the cloud to update
     * @param flavor the flavor to add
     */
    public void addCloudImageFlavor(Cloud cloud, CloudImageFlavor flavor) {
        // The field images cannot be null
        Set<CloudImageFlavor> flavors = cloud.getFlavors();
        if (!flavors.add(flavor)) {
            throw new AlreadyExistException("Cloud image flavor [" + flavor.getId() + "] already exists");
        }
        alienDAO.save(cloud);
    }

    /**
     * Remove an image from the cloud consists of removing available compute template and push update to our persistence layer
     *
     * @param cloud the cloud to update
     * @param cloudImageId the image to remove
     */
    public void removeCloudImage(Cloud cloud, String cloudImageId) {
        cloud.getImages().remove(cloudImageId);
        cloud.getImageMapping().remove(cloudImageId);
        getComputeTemplates(cloud, cloudImageId, null, true);
        alienDAO.save(cloud);
    }

    /**
     * Remove a flavor from the cloud consists of removing available compute template and push update to our persistence layer
     *
     * @param cloud the cloud to update
     * @param flavorId the flavor to remove
     */
    public void removeCloudImageFlavor(Cloud cloud, String flavorId) {
        getResource(cloud.getFlavors(), flavorId, true);
        cloud.getFlavorMapping().remove(flavorId);
        getComputeTemplates(cloud, null, flavorId, true);
        alienDAO.save(cloud);
    }

    /**
     * Set the template status (enable / disable)
     *
     * @param cloud the cloud to update
     * @param cloudImageId the image id
     * @param flavorId the flavor id
     * @param enabled enable or disable
     */
    public void setCloudTemplateStatus(Cloud cloud, String cloudImageId, String flavorId, boolean enabled) {
        List<ActivableComputeTemplate> computeTemplates = getComputeTemplates(cloud, cloudImageId, flavorId, false);
        if (computeTemplates.isEmpty()) {
            throw new NotFoundException("No compute template found for [" + cloudImageId + "," + flavorId + "]");
        }
        computeTemplates.iterator().next().setEnabled(enabled);
        alienDAO.save(cloud);
    }

    public String[] getCloudResourceIds(Cloud cloud, CloudResourceType type) {
        if (cloud.isEnabled()) {
            IPaaSProvider paaSProvider = paaSProviderService.getPaaSProvider(cloud.getId());
            return paaSProvider.getAvailableResourceIds(type);
        } else {
            return null;
        }
    }

    public CloudResourceMatcherConfig getCloudResourceMatcherConfig(Cloud cloud) {
        CloudResourceMatcherConfig config = new CloudResourceMatcherConfig();
        Map<String, CloudImage> images = cloudImageService.getMultiple(cloud.getImages());
        config.setImageMapping(MappingUtil.getMapping(images, cloud.getImageMapping()));
        config.setFlavorMapping(MappingUtil.getMapping(buildResourcesMap(cloud.getFlavors()), cloud.getFlavorMapping()));
        config.setNetworkMapping(MappingUtil.getMapping(buildResourcesMap(cloud.getNetworks()), cloud.getNetworkMapping()));
        config.setStorageMapping(MappingUtil.getMapping(buildResourcesMap(cloud.getStorages()), cloud.getStorageMapping()));
        return config;
    }

    private <U extends ICloudResourceTemplate> Map<String, U> buildResourcesMap(Set<U> resources) {
        Map<String, U> resourcesMap = Maps.newHashMap();
        for (U resource : resources) {
            resourcesMap.put(resource.getId(), resource);
        }
        return resourcesMap;
    }

    /**
     * Set the template resource id
     *
     * @param cloud the cloud to update
     * @param cloudImageId the image id
     * @param paaSResourceId paaS resource id
     */
    public void setCloudImageResourceId(Cloud cloud, String cloudImageId, String paaSResourceId) throws CloudDisabledException {
        IPaaSProvider paaSProvider = getPaaSProvider(cloud.getId());
        getComputeTemplates(cloud, cloudImageId, null, true);
        if (StringUtils.isEmpty(paaSResourceId)) {
            cloud.getImageMapping().remove(cloudImageId);
        } else {
            cloud.getImageMapping().put(cloudImageId, paaSResourceId);
            addComputeTemplatesForImage(cloud, paaSProvider, getCloudResourceMatcherConfig(cloud), cloudImageId);
        }
        initializeMatcherConfig(paaSProvider, cloud);
        alienDAO.save(cloud);
    }

    private void addComputeTemplatesForImage(Cloud cloud, IPaaSProvider paaSProvider, CloudResourceMatcherConfig matcherConfig, String cloudImageId) {
        Set<ActivableComputeTemplate> computes = cloud.getComputeTemplates();
        CloudImage cloudImage = cloudImageService.getCloudImageFailIfNotExist(cloudImageId);
        String paaSImageId = matcherConfig.getImageMapping().get(cloudImage);
        if (paaSImageId == null) {
            throw new NotFoundException("Cannot add compute template as mapping not found for image [" + cloudImageId + "]");
        }
        String[] compatibleFlavorIdsArray = paaSProvider.getAvailableResourceIds(CloudResourceType.FLAVOR, paaSImageId);
        Set<String> compatibleFlavorIds = compatibleFlavorIdsArray != null ? Sets.newHashSet(compatibleFlavorIdsArray) : null;
        for (CloudImageFlavor flavor : cloud.getFlavors()) {
            // Only add compute templates if it is matching image requirement and it exists mapping for the flavor
            String paaSFlavorId = matcherConfig.getFlavorMapping().get(flavor);
            if (paaSProvider instanceof ITemplateManagedPaaSProvider) {
                PaaSComputeTemplate[] paaSManagedTemplates = ((ITemplateManagedPaaSProvider) paaSProvider).getAvailablePaaSComputeTemplates();
                if (paaSManagedTemplates != null) {
                    for (PaaSComputeTemplate paaSManagedTemplate : paaSManagedTemplates) {
                        if (isFlavorMatchImageRequirement(cloudImage, flavor) && paaSFlavorId != null && paaSManagedTemplate.getImageId().equals(paaSImageId)
                                && paaSManagedTemplate.getFlavorId().equals(paaSFlavorId)) {
                            computes.add(new ActivableComputeTemplate(cloudImageId, flavor.getId(), paaSManagedTemplate.getDescription()));
                        }
                    }
                }
            } else {
                if (isFlavorMatchImageRequirement(cloudImage, flavor) && paaSFlavorId != null
                        && (compatibleFlavorIds == null || compatibleFlavorIds.contains(paaSFlavorId))) {
                    computes.add(new ActivableComputeTemplate(cloudImageId, flavor.getId(), "IaaS Image : [" + paaSImageId + "], IaaS Flavor : ["
                            + paaSFlavorId + "]"));
                }
            }
        }
    }

    /**
     * Set the template resource id
     *
     * @param cloud the cloud to update
     * @param flavorId the image id
     * @param paaSResourceId paaS resource id
     */
    public void setCloudImageFlavorResourceId(Cloud cloud, String flavorId, String paaSResourceId) throws CloudDisabledException {
        IPaaSProvider paaSProvider = getPaaSProvider(cloud.getId());
        CloudImageFlavor cloudImageFlavor = getResource(cloud.getFlavors(), flavorId, false);
        if (cloudImageFlavor == null) {
            throw new NotFoundException("Cloud image flavor [" + flavorId + "] do not exist");
        }
        getComputeTemplates(cloud, null, flavorId, true);
        if (StringUtils.isEmpty(paaSResourceId)) {
            cloud.getFlavorMapping().remove(flavorId);
        } else {
            cloud.getFlavorMapping().put(flavorId, paaSResourceId);
            addComputeTemplatesForFlavor(cloud, paaSProvider, getCloudResourceMatcherConfig(cloud), cloudImageFlavor);
        }
        initializeMatcherConfig(paaSProvider, cloud);
        alienDAO.save(cloud);
    }

    private void addComputeTemplatesForFlavor(Cloud cloud, IPaaSProvider paaSProvider, CloudResourceMatcherConfig matcherConfig, CloudImageFlavor flavor) {
        Set<ActivableComputeTemplate> computes = cloud.getComputeTemplates();
        Map<String, CloudImage> images = cloudImageService.getMultiple(cloud.getImages());
        String paaSFlavorId = matcherConfig.getFlavorMapping().get(flavor);
        if (paaSFlavorId == null) {
            throw new NotFoundException("Cannot add compute template as mapping not found for flavor [" + flavor.getId() + "]");
        }
        for (String imageId : cloud.getImages()) {
            // Only add compute templates if it is matching image requirement and it exists mapping for the image
            CloudImage image = images.get(imageId);
            String paaSImageId = matcherConfig.getImageMapping().get(image);
            if (paaSProvider instanceof ITemplateManagedPaaSProvider) {
                PaaSComputeTemplate[] paaSManagedTemplates = ((ITemplateManagedPaaSProvider) paaSProvider).getAvailablePaaSComputeTemplates();
                if (paaSManagedTemplates != null) {
                    for (PaaSComputeTemplate paaSManagedTemplate : paaSManagedTemplates) {
                        if (isFlavorMatchImageRequirement(image, flavor) && paaSImageId != null && paaSManagedTemplate.getImageId().equals(paaSImageId)
                                && paaSManagedTemplate.getFlavorId().equals(paaSFlavorId)) {
                            computes.add(new ActivableComputeTemplate(imageId, flavor.getId(), paaSManagedTemplate.getDescription()));
                        }
                    }
                }
            } else {
                String[] compatibleFlavorIdsArray = paaSProvider.getAvailableResourceIds(CloudResourceType.FLAVOR, paaSImageId);
                Set<String> compatibleFlavorIds = compatibleFlavorIdsArray != null ? Sets.newHashSet(compatibleFlavorIdsArray) : null;
                if (isFlavorMatchImageRequirement(image, flavor) && paaSImageId != null
                        && (compatibleFlavorIds == null || compatibleFlavorIds.contains(paaSFlavorId))) {
                    computes.add(new ActivableComputeTemplate(imageId, flavor.getId(), "IaaS Image : [" + paaSImageId + "], IaaS Flavor : [" + paaSFlavorId
                            + "]"));
                }
            }
        }
    }

    public List<ActivableComputeTemplate> getComputeTemplates(Cloud cloud, String cloudImageId, String flavorId, boolean take) {
        Iterator<ActivableComputeTemplate> templateIterator = cloud.getComputeTemplates().iterator();
        List<ActivableComputeTemplate> foundTemplates = Lists.newArrayList();
        if (cloudImageId == null && flavorId == null) {
            throw new NullPointerException("No criteria selected to search for compute template");
        }
        while (templateIterator.hasNext()) {
            ActivableComputeTemplate computeTemplate = templateIterator.next();
            boolean matched = true;
            if (cloudImageId != null && !cloudImageId.equals(computeTemplate.getCloudImageId())) {
                matched = false;
            }
            if (flavorId != null && !flavorId.equals(computeTemplate.getCloudImageFlavorId())) {
                matched = false;
            }
            if (matched) {
                foundTemplates.add(computeTemplate);
                if (take) {
                    templateIterator.remove();
                }
            }
        }
        return foundTemplates;
    }

    private <T extends ICloudResourceTemplate> T getResource(Collection<T> resources, String id, boolean take) {
        Iterator<T> templateIterator = resources.iterator();
        while (templateIterator.hasNext()) {
            T template = templateIterator.next();
            if (template.getId().equals(id)) {
                if (take) {
                    templateIterator.remove();
                }
                return template;
            }
        }
        return null;
    }

    public void addNetwork(Cloud cloud, NetworkTemplate network) {
        Set<NetworkTemplate> existingNetworks = cloud.getNetworks();
        if (getResource(existingNetworks, network.getId(), false) != null) {
            throw new AlreadyExistException("Network template " + network.getId() + " already exist");
        }
        existingNetworks.add(network);
        alienDAO.save(cloud);
    }

    public void removeNetwork(Cloud cloud, String networkName) {
        Set<NetworkTemplate> existingNetworks = cloud.getNetworks();
        // Remove network
        getResource(existingNetworks, networkName, true);
        // Remove matched network
        cloud.getNetworkMapping().remove(networkName);
        // Save
        alienDAO.save(cloud);
    }

    /**
     * Set the network resource id
     *
     * @param cloud the cloud to update
     * @param networkName the network's name
     * @param paaSResourceId paaS resource id
     */
    public void setNetworkResourceId(Cloud cloud, String networkName, String paaSResourceId) throws CloudDisabledException {
        NetworkTemplate foundNetwork = getResource(cloud.getNetworks(), networkName, false);
        if (foundNetwork == null) {
            throw new NotFoundException("Network [" + networkName + "] not found");
        }
        if (StringUtils.isEmpty(paaSResourceId)) {
            cloud.getNetworkMapping().remove(networkName);
        } else {
            cloud.getNetworkMapping().put(networkName, paaSResourceId);
        }
        initializeMatcherConfig(getPaaSProvider(cloud.getId()), cloud);
        alienDAO.save(cloud);
    }

    public void addStorageTemplate(Cloud cloud, StorageTemplate storageTemplate) {
        Set<StorageTemplate> storageTemplates = cloud.getStorages();
        if (getResource(storageTemplates, storageTemplate.getId(), false) != null) {
            throw new AlreadyExistException("Network template " + storageTemplate.getId() + " already exist");
        }
        storageTemplates.add(storageTemplate);
        alienDAO.save(cloud);
    }

    public void removeStorageTemplate(Cloud cloud, String storageId) {
        Set<StorageTemplate> storageTemplates = cloud.getStorages();
        // Remove storage
        getResource(storageTemplates, storageId, true);
        // Remove matched storage
        cloud.getStorageMapping().remove(storageId);
        // Save
        alienDAO.save(cloud);
    }

    /**
     * Set the storage resource id
     *
     * @param cloud the cloud to update
     * @param storageId the storage's name
     * @param paaSResourceId paaS resource id
     */
    public void setStorageResourceId(Cloud cloud, String storageId, String paaSResourceId) throws CloudDisabledException {
        StorageTemplate foundStorage = getResource(cloud.getStorages(), storageId, false);
        if (foundStorage == null) {
            throw new NotFoundException("Storage [" + storageId + "] not found");
        }
        if (StringUtils.isEmpty(paaSResourceId)) {
            cloud.getStorageMapping().remove(storageId);
        } else {
            cloud.getStorageMapping().put(storageId, paaSResourceId);
        }
        initializeMatcherConfig(getPaaSProvider(cloud.getId()), cloud);
        alienDAO.save(cloud);
    }
}
