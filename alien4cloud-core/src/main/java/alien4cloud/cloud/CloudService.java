package alien4cloud.cloud;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

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
import alien4cloud.model.cloud.ActivableComputeTemplate;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudConfiguration;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.cloud.ComputeTemplate;
import alien4cloud.model.cloud.MatchedComputeTemplate;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.IConfigurablePaaSProvider;
import alien4cloud.paas.IManualResourceMatcherPaaSProvider;
import alien4cloud.paas.IPaaSProvider;
import alien4cloud.paas.IPaaSProviderFactory;
import alien4cloud.paas.PaaSProviderFactoriesService;
import alien4cloud.paas.PaaSProviderService;
import alien4cloud.paas.exception.CloudDisabledException;
import alien4cloud.paas.exception.PaaSTechnicalException;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.container.model.template.PropertyValue;
import alien4cloud.tosca.container.model.type.PropertyDefinition;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.PropertyUtil;
import alien4cloud.utils.ReflectionUtil;

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
        long totalResult = 0;
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
        IPaaSProvider provider = passProviderFactory.newInstance();
        cloud.setConfigurable(saveDefaultConfiguration(cloud.getId(), provider));
        initializeMatcherConfig(provider, cloud);

        alienDAO.save(cloud);
        return cloud.getId();
    }

    private void initializeMatcherConfig(IPaaSProvider provider, Cloud cloud) {
        // If the provider manually match resources, then we should update it with saved configuration from Alien
        if (provider instanceof IManualResourceMatcherPaaSProvider) {
            log.info("Cloud <{}> needs manual resource matcher configuration", cloud.getName());
            CloudResourceMatcherConfig config = findCloudResourceMatcherConfig(cloud);
            if (config == null) {
                log.info("Publish new manual resource matcher configuration for cloud <{}>", cloud.getName());
                config = new CloudResourceMatcherConfig();
                config.setId(cloud.getId());
                alienDAO.save(config);
            }
            ((IManualResourceMatcherPaaSProvider) provider).updateMatcherConfig(config);
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean saveDefaultConfiguration(String cloudId, IPaaSProvider provider) {
        if (provider instanceof IConfigurablePaaSProvider) {
            Object defaultConfiguration = ((IConfigurablePaaSProvider) provider).getDefaultConfiguration();
            CloudConfiguration configuration = new CloudConfiguration(cloudId, defaultConfiguration);
            alienDAO.save(configuration);
            return true;
        }
        return false;
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
        if (updated.getName() != null) {
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
            alienDAO.delete(Cloud.class, id);
            alienDAO.delete(CloudConfiguration.class, id);
            return true;
        }
        return false;
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
     * @param authorizationFilter
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

        IPaaSProvider paaSProvider = paaSProviderService.getPaaSProvider(id);
        if (paaSProvider == null) {
            // cloud may be disabled, let's get properties from a new instance
            paaSProvider = paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean()).newInstance();
        }

        return paaSProvider.getDeploymentPropertyMap();
    }

    /**
     * Get deployment properties for a cloud.
     *
     * @param id Id of the cloud for which to get properties.
     */
    public Map<String, PropertyValue> getDeploymentProps(String id) {
        return PropertyUtil.getDefaultPropertyValuesFromPropertyDefinitions(getDeploymentPropertyDefinitions(id));
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
        IPaaSProvider paaSProvider = paaSProviderService.getPaaSProvider(cloud.getId());
        if (paaSProvider == null) {
            paaSProvider = paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean()).newInstance();
        }
        if (paaSProvider instanceof IConfigurablePaaSProvider) {
            return ReflectionUtil.getGenericArgumentType(paaSProvider.getClass(), IConfigurablePaaSProvider.class, 0);
        }
        return null;
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
        IPaaSProvider provider = passProviderFactory.newInstance();
        if (provider instanceof IConfigurablePaaSProvider) {
            Object configuration = getConfiguration(cloud.getId());
            if (configuration != null) {
                Object validConfiguration;
                try {
                    validConfiguration = configurationAsValidObject(cloud, configuration);
                } catch (IOException e) {
                    throw new PluginConfigurationException("Failed convert configuration in object.", e);
                }

                ((IConfigurablePaaSProvider) provider).setConfiguration(validConfiguration);
            }
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
        // un-register the IPaaSProvider for the cloud.
        paaSProviderService.unregister(cloud.getId());

        cloud.setEnabled(false);
        alienDAO.save(cloud);
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
        // The field computes cannot be null
        Set<ActivableComputeTemplate> computes = cloud.getComputeTemplates();
        CloudImage cloudImage = cloudImageService.getCloudImageFailIfNotExist(cloudImageId);
        for (CloudImageFlavor flavor : cloud.getFlavors()) {
            if (isFlavorMatchImageRequirement(cloudImage, flavor)) {
                computes.add(new ActivableComputeTemplate(cloudImageId, flavor.getId()));
            }
        }
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
        Map<String, CloudImage> images = cloudImageService.getMultiple(cloud.getImages());
        // The field computes cannot be null
        Set<ActivableComputeTemplate> computes = cloud.getComputeTemplates();
        for (String imageId : cloud.getImages()) {
            if (isFlavorMatchImageRequirement(images.get(imageId), flavor)) {
                computes.add(new ActivableComputeTemplate(imageId, flavor.getId()));
            }
        }
        alienDAO.save(cloud);
    }

    /**
     * Remove an image from the cloud consists of removing available compute template and push update to our persistence layer
     *
     * @param cloud the cloud to update
     * @param cloudImageId the image to remove
     */
    public void removeCloudImage(Cloud cloud, CloudResourceMatcherConfig config, String cloudImageId) {
        cloud.getImages().remove(cloudImageId);
        Iterator<ActivableComputeTemplate> iterator = cloud.getComputeTemplates().iterator();
        while (iterator.hasNext()) {
            ActivableComputeTemplate computeTemplate = iterator.next();
            if (computeTemplate.getCloudImageId().equals(cloudImageId)) {
                iterator.remove();
            }
        }
        if (config != null) {
            List<MatchedComputeTemplate> matchedTemplates = config.getMatchedComputeTemplates();
            Iterator<MatchedComputeTemplate> matchedTemplateIterator = matchedTemplates.iterator();
            while (matchedTemplateIterator.hasNext()) {
                MatchedComputeTemplate matchedTemplate = matchedTemplateIterator.next();
                if (matchedTemplate.getComputeTemplate().getCloudImageId().equals(cloudImageId)) {
                    matchedTemplateIterator.remove();
                }
            }
            alienDAO.save(config);
        }
        alienDAO.save(cloud);
    }

    /**
     * Remove a flavor from the cloud consists of removing available compute template and push update to our persistence layer
     *
     * @param cloud the cloud to update
     * @param flavorId the flavor to remove
     */
    public void removeCloudImageFlavor(Cloud cloud, CloudResourceMatcherConfig config, String flavorId) {
        Iterator<CloudImageFlavor> flavorIterator = cloud.getFlavors().iterator();
        while (flavorIterator.hasNext()) {
            CloudImageFlavor flavor = flavorIterator.next();
            if (flavor.getId().equals(flavorId)) {
                flavorIterator.remove();
            }
        }
        Iterator<ActivableComputeTemplate> templateIterator = cloud.getComputeTemplates().iterator();
        while (templateIterator.hasNext()) {
            ActivableComputeTemplate computeTemplate = templateIterator.next();
            if (computeTemplate.getCloudImageFlavorId().equals(flavorId)) {
                templateIterator.remove();
            }
        }
        if (config != null) {
            List<MatchedComputeTemplate> matchedTemplates = config.getMatchedComputeTemplates();
            Iterator<MatchedComputeTemplate> matchedTemplateIterator = matchedTemplates.iterator();
            while (matchedTemplateIterator.hasNext()) {
                MatchedComputeTemplate matchedTemplate = matchedTemplateIterator.next();
                if (matchedTemplate.getComputeTemplate().getCloudImageFlavorId().equals(flavorId)) {
                    matchedTemplateIterator.remove();
                }
            }
            alienDAO.save(config);
        }
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
        ActivableComputeTemplate computeTemplate = findComputeTemplate(cloud, cloudImageId, flavorId);
        computeTemplate.setEnabled(enabled);
        alienDAO.save(cloud);
    }

    public CloudResourceMatcherConfig findCloudResourceMatcherConfig(Cloud cloud) {
        // A little bit tricky here, the resource matcher has the same id as the cloud
        return alienDAO.findById(CloudResourceMatcherConfig.class, cloud.getId());
    }

    public CloudResourceMatcherConfig getMandatoryCloudResourceMatcherConfig(Cloud cloud) {
        CloudResourceMatcherConfig config = findCloudResourceMatcherConfig(cloud);
        if (config == null) {
            throw new NotFoundException("Could not find resource matcher config for cloud " + cloud.getName() + ", perhaps it's not a manual matcher PaaS");
        }
        return config;
    }

    /**
     * Set the template resource id
     *
     * @param cloud the cloud to update
     * @param cloudImageId the image id
     * @param flavorId the flavor id
     * @param paaSResourceId paaS resource id
     */
    public void setCloudTemplateResourceId(Cloud cloud, String cloudImageId, String flavorId, String paaSResourceId) {
        ActivableComputeTemplate computeTemplate = findComputeTemplate(cloud, cloudImageId, flavorId);
        CloudResourceMatcherConfig matcherConfig = getMandatoryCloudResourceMatcherConfig(cloud);
        if (paaSResourceId == null) {
            getMatchedComputeTemplate(matcherConfig, cloudImageId, flavorId, true);
        } else {
            MatchedComputeTemplate existing = getMatchedComputeTemplate(matcherConfig, cloudImageId, flavorId, false);
            if (existing != null) {
                existing.setPaaSResourceId(paaSResourceId);
            } else {
                matcherConfig.getMatchedComputeTemplates().add(
                        new MatchedComputeTemplate(new ComputeTemplate(computeTemplate.getCloudImageId(), computeTemplate.getCloudImageFlavorId()),
                                paaSResourceId));
            }
        }
        IPaaSProvider paaSProvider = paaSProviderService.getPaaSProvider(cloud.getId());
        if (paaSProvider != null) {
            // Cloud may not be initialized yet
            initializeMatcherConfig(paaSProvider, cloud);
        }
        alienDAO.save(matcherConfig);
    }

    private ActivableComputeTemplate findComputeTemplate(Cloud cloud, String cloudImageId, String flavorId) {
        Iterator<ActivableComputeTemplate> templateIterator = cloud.getComputeTemplates().iterator();
        while (templateIterator.hasNext()) {
            ActivableComputeTemplate computeTemplate = templateIterator.next();
            if (computeTemplate.getCloudImageId().equals(cloudImageId) && computeTemplate.getCloudImageFlavorId().equals(flavorId)) {
                return computeTemplate;
            }
        }
        throw new NotFoundException("Could not find template [" + cloudImageId + "," + flavorId + "]");
    }

    private MatchedComputeTemplate getMatchedComputeTemplate(CloudResourceMatcherConfig config, String cloudImageId, String flavorId, boolean take) {
        Iterator<MatchedComputeTemplate> templateIterator = config.getMatchedComputeTemplates().iterator();
        while (templateIterator.hasNext()) {
            MatchedComputeTemplate matchedTemplate = templateIterator.next();
            if (matchedTemplate.getComputeTemplate().getCloudImageId().equals(cloudImageId)
                    && matchedTemplate.getComputeTemplate().getCloudImageFlavorId().equals(flavorId)) {
                if (take) {
                    templateIterator.remove();
                }
                return matchedTemplate;
            }
        }
        return null;
    }
}
