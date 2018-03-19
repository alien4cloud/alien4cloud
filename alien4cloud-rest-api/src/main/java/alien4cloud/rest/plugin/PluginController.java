package alien4cloud.rest.plugin;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.plugin.IPluginConfigurator;
import alien4cloud.plugin.Plugin;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.exception.MissingPlugingDescriptorFileException;
import alien4cloud.plugin.exception.PluginConfigurationException;
import alien4cloud.plugin.exception.PluginLoadingException;
import alien4cloud.plugin.model.PluginConfiguration;
import alien4cloud.plugin.model.PluginUsage;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.FileUploadUtil;
import alien4cloud.utils.FileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Controller for plugins.
 */
@RestController
@RequestMapping(value = { "/rest/plugins", "/rest/v1/plugins", "/rest/latest/plugins" }, produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@Api(value = "Plugins", description = "Manages plugins.", authorizations = { @Authorization("ADMIN") })
public class PluginController {
    @Resource
    private PluginManager pluginManager;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    private Path tempDirPath;

    @ApiOperation(value = "Upload a plugin archive.", notes = "Content of the zip file must be compliant with the expected alien 4 cloud plugin structure.", authorizations = {
            @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> upload(
            @ApiParam(value = "Zip file that contains the plugin.", required = true) @RequestParam("file") MultipartFile pluginArchive) {
        Path pluginPath = null;
        try {
            // save the plugin archive in the temp directory
            pluginPath = Files.createTempFile(tempDirPath, null, ".zip");
            FileUploadUtil.safeTransferTo(pluginPath, pluginArchive);
            // upload the plugin archive
            Plugin plugin = pluginManager.uploadPlugin(pluginPath);
            // if the plugin is configurable, then try reuse if existing a previous version
            // TODO as we do not manage many version of a same plugin, is this still relevant ?
            if (plugin.isConfigurable()) {
                tryReusePreviousVersionConf(plugin);
            }
        } catch (MissingPlugingDescriptorFileException e) {
            log.error("Your plugin don't have the META-INF/plugin.yml file.", e);
            return RestResponseBuilder.<Void> builder().error(
                    new RestError(RestErrorCode.MISSING_PLUGIN_DESCRIPTOR_FILE_EXCEPTION.getCode(), "Your plugin don't have the META-INF/plugin.yml file."))
                    .build();
        } catch (IOException e) {
            log.error("Unexpected IO error on plugin upload.", e);
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.INDEXING_SERVICE_ERROR.getCode(),
                    "A technical issue occurred during the plugin upload <" + e.getMessage() + ">.")).build();
        } catch (PluginLoadingException e) {
            log.error("Fail to enable and load the plugin. The plugin will remain disabled", e);
            return RestResponseBuilder.<Void> builder().error(new RestError(RestErrorCode.ENABLE_PLUGIN_ERROR.getCode(), e.getMessage())).build();
        } finally {
            if (pluginPath != null) {
                try {
                    FileUtil.delete(pluginPath);
                } catch (IOException e) {
                    log.error("Failed to cleanup temporary file <" + pluginPath.toString() + ">", e);
                }
            }
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void tryReusePreviousVersionConf(Plugin plugin) {
        // search for a previous version
        // TODO manage the case there are many previous versions
        QueryBuilder macthNameQuerybuilder = QueryBuilders.matchQuery("descriptor.id", plugin.getDescriptor().getId());
        QueryBuilder idQueryBuilder = QueryBuilders.idsQuery(MappingBuilder.indexTypeFromClass(Plugin.class)).ids(plugin.getId());
        QueryBuilder boolQuery = QueryBuilders.boolQuery().must(macthNameQuerybuilder).mustNot(idQueryBuilder);
        Plugin oldVersionPlugin = alienDAO.customFind(Plugin.class, boolQuery);
        if (oldVersionPlugin != null && oldVersionPlugin.isConfigurable()) {
            // get the configuration type
            Class<?> configType = pluginManager.getConfigurationType(plugin.getId());
            PluginConfiguration oldPluginConf = alienDAO.findById(PluginConfiguration.class, oldVersionPlugin.getId());
            // try to de-serialize it into the config of the new version
            if (oldPluginConf != null && oldPluginConf.getConfiguration() != null) {
                try {
                    Object configObject = JsonUtil.readObject(JsonUtil.toString(oldPluginConf.getConfiguration()), configType);
                    IPluginConfigurator configurator = pluginManager.getConfiguratorFor(plugin.getId());
                    configurator.setConfiguration(configObject);
                    PluginConfiguration pluginConf = new PluginConfiguration(plugin.getId(), configObject);
                    alienDAO.save(pluginConf);
                } catch (IOException e) {
                    log.warn("Plugin [" + plugin.getId() + "]: Failed to re-use the configuration of the previous version " + oldVersionPlugin.getId()
                            + ". The configuration beans are not comptible", e);
                } catch (PluginConfigurationException e) {
                    log.warn("Plugin [" + plugin.getId() + "]: Failed to re-use the configuration of the previous version " + oldVersionPlugin.getId()
                            + ". Error while applying the configuration", e);
                }
            }
        }
    }

    @ApiOperation(value = "Search for plugins registered in ALIEN.")
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<GetMultipleDataResult<Plugin>> search(
            @ApiParam(value = "Query text.") @RequestParam(required = false) String query,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "20") int size) {
        GetMultipleDataResult<Plugin> result = this.alienDAO.search(Plugin.class, query, null, from, size);
        return RestResponseBuilder.<GetMultipleDataResult<Plugin>> builder().data(result).build();
    }

    @ApiOperation(value = "Enable a plugin.", notes = "Enable and load a plugin. Role required [ ADMIN ]")
    @RequestMapping(value = "/{pluginId:.+}/enable", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> enablePlugin(@PathVariable String pluginId) {
        try {
            this.pluginManager.enablePlugin(pluginId);
        } catch (PluginLoadingException e) {
            return RestResponseBuilder.<Void> builder()
                    .error(new RestError(RestErrorCode.UNCATEGORIZED_ERROR.getCode(), e.getMessage() + " Cause: " + e.getCause().getMessage())).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Disable a plugin.", notes = "Disable a plugin (and unloads it if enabled). Note that if the plugin is used (deployment plugin for example) it won't be disabled but will be marked as deprecated. In such situation an error code 350 is returned as part of the error and a list of plugin usages will be returned as part of the returned data. Role required [ ADMIN ]")
    @RequestMapping(value = "/{pluginId:.+}/disable", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<List<PluginUsage>> disablePlugin(@PathVariable String pluginId) {
        List<PluginUsage> usages = this.pluginManager.disablePlugin(pluginId, false);
        if (usages == null || usages.isEmpty()) {
            return RestResponseBuilder.<List<PluginUsage>> builder().build();
        }
        return RestResponseBuilder.<List<PluginUsage>> builder().data(usages)
                .error(new RestError(RestErrorCode.PLUGIN_USED_ERROR.getCode(), "The plugin is used and cannot be disabled. It has been marked as deprecated."))
                .build();
    }

    @ApiOperation(value = "Remove a plugin.", notes = "Remove a plugin (and unloads it if enabled). Note that if the plugin is used (deployment plugin for example) it won't be disabled but will be marked as deprecated. In such situation an error code 350 is returned as part of the error and a list of plugin usages will be returned as part of the returned data. Role required [ ADMIN ]")
    @RequestMapping(value = "/{pluginId:.+}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<List<PluginUsage>> removePlugin(@PathVariable String pluginId) {
        List<PluginUsage> usages = this.pluginManager.disablePlugin(pluginId, true);
        if (usages == null || usages.isEmpty()) {
            return RestResponseBuilder.<List<PluginUsage>> builder().build();
        }
        return RestResponseBuilder.<List<PluginUsage>> builder().data(usages)
                .error(new RestError(RestErrorCode.PLUGIN_USED_ERROR.getCode(), "The plugin is used and cannot be disabled. It has been marked as deprecated."))
                .build();
    }

    @ApiOperation(value = "Get a plugin configuration object.", notes = "Retrieve a plugin configuration object.  Role required [ ADMIN ]")
    @RequestMapping(value = "/{pluginId:.+}/config", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<Object> getPluginConfiguration(@PathVariable String pluginId) throws PluginLoadingException {
        RestResponse<Object> response = RestResponseBuilder.<Object> builder().build();

        // check if a config object already exist in the repository
        PluginConfiguration pluginConf = alienDAO.findById(PluginConfiguration.class, pluginId);
        if (pluginConf != null && pluginConf.getConfiguration() != null) {
            response.setData(pluginConf.getConfiguration());
            return response;
        }

        if (pluginManager.isPluginConfigurable(pluginId)) {
            Object configObject = pluginManager.getConfiguratorFor(pluginId).getDefaultConfiguration();
            response.setData(configObject);
        }
        return response;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @ApiOperation(value = "Save a configuration object for a plugin.", notes = "Save a configuration object for a plugin. Returns the newly saved configuration.  Role required [ ADMIN ]")
    @RequestMapping(value = "/{pluginId:.+}/config", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Object> savePluginConfiguration(@PathVariable String pluginId, @RequestBody Object configObjectRequest) throws PluginLoadingException {
        RestResponse<Object> response = RestResponseBuilder.<Object> builder().build();

        if (pluginManager.isPluginConfigurable(pluginId)) {
            Class<?> configType = pluginManager.getConfigurationType(pluginId);
            try {
                Object configObject = JsonUtil.readObject(JsonUtil.toString(configObjectRequest), configType);
                IPluginConfigurator configurator = pluginManager.getConfiguratorFor(pluginId);
                configurator.setConfiguration(configObject);
                PluginConfiguration pluginConf = new PluginConfiguration(pluginId, configObject);
                alienDAO.save(pluginConf);
                response.setData(configObject);
            } catch (IOException e) {
                response.setError(RestErrorBuilder.builder(RestErrorCode.INVALID_PLUGIN_CONFIGURATION)
                        .message("The posted configuration is not of type <" + configType.getName() + ">").build());
                log.error("The posted configuration is not of type <" + configType.getName() + ">", e);
            } catch (PluginConfigurationException e) {
                response.setError(RestErrorBuilder.builder(RestErrorCode.INVALID_PLUGIN_CONFIGURATION)
                        .message("The plugin configuration failed for plugin <" + pluginId + ">: configuration parameters are invalid.").build());
            }
        }
        return response;
    }

    @Required
    @Value("${directories.alien}/${directories.upload_temp}")
    public void setTempDirPath(String tempDirPath) throws IOException {
        this.tempDirPath = FileUtil.createDirectoryIfNotExists(tempDirPath);
    }
}
