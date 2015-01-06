package alien4cloud.rest.cloud;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.index.query.FilterBuilder;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.cloud.CloudImageService;
import alien4cloud.cloud.CloudService;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.cloud.CloudResourceType;
import alien4cloud.model.cloud.Network;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.Role;
import alien4cloud.tosca.model.PropertyDefinition;
import alien4cloud.utils.services.ResourceRoleService;

import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;

@Slf4j
@RestController
@RequestMapping("/rest/clouds")
public class CloudController {

    @Resource
    private CloudService cloudService;
    @Resource
    private CloudImageService cloudImageService;
    @Resource
    private ResourceRoleService resourceRoleService;

    /**
     * Create a new cloud.
     *
     * @param cloud The cloud to add.
     */
    @ApiOperation(value = "Create a new cloud.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<String> create(@ApiParam(value = "The instance of cloud to add.", required = true) @Valid @RequestBody Cloud cloud) {
        String cloudId = cloudService.create(cloud);
        return RestResponseBuilder.<String> builder().data(cloudId).build();
    }

    /**
     * Update a cloud.
     *
     * @param cloud The cloud to update.
     */
    @ApiOperation(value = "Update an existing cloud.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> update(@ApiParam(value = "The instance of cloud to update.", required = true) @RequestBody Cloud cloud) {
        cloudService.update(cloud);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Delete an instance of a cloud.
     *
     * @param id Id of the cloud to delete.
     */
    @ApiOperation(value = "Delete an existing cloud. The operation fails in case an application is still deployed on this cloud.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Boolean> delete(@ApiParam(value = "Id of the cloud to delete.", required = true) @Valid @NotBlank @PathVariable String id) {
        Boolean deleted = cloudService.delete(id);
        return RestResponseBuilder.<Boolean> builder().data(deleted).build();
    }

    /**
     * Get details for a cloud.
     *
     * @param id Id of the cloud to delete.
     */
    @ApiOperation(value = "Get details of a cloud.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<CloudDTO> get(@ApiParam(value = "Id of the cloud for which to get details.", required = true) @Valid @NotBlank @PathVariable String id) {
        Cloud cloud = cloudService.getMandatoryCloud(id);
        Map<String, CloudImage> images = cloudImageService.getMultiple(cloud.getImages());
        Map<String, CloudImageFlavor> flavors = Maps.newHashMap();
        for (CloudImageFlavor flavor : cloud.getFlavors()) {
            flavors.put(flavor.getId(), flavor);
        }
        Map<String, Network> networks = Maps.newHashMap();
        for (Network network : cloud.getNetworks()) {
            networks.put(network.getNetworkName(), network);
        }
        CloudDTO cloudDTO = new CloudDTO(cloud, new CloudResourceMatcherDTO(cloudService.findCloudResourceMatcherConfig(cloud),
                cloudService.getCloudResourceIds(cloud, CloudResourceType.COMPUTE)), images, flavors, networks);
        return RestResponseBuilder.<CloudDTO> builder().data(cloudDTO).build();
    }

    /**
     * Get deployment properties for a cloud.
     *
     * @param id Id of the cloud for which to get properties.
     */
    @ApiOperation(value = "Get deployment properties for a cloud.", notes = "Deployments properties are properties that can be set by the Application Deployer before deployment. They depends on the IPaaSProvider plugin associated with a cloud.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/deploymentpropertydefinitions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Map<String, PropertyDefinition>> getDeploymentPropertyDefinitions(
            @ApiParam(value = "Id of the cloud for which to get details.", required = true) @Valid @NotBlank @PathVariable String id) {
        return RestResponseBuilder.<Map<String, PropertyDefinition>> builder().data(cloudService.getDeploymentPropertyDefinitions(id)).build();
    }

    /**
     * Search for clouds.
     *
     * @param query Query to find the cloud.
     * @param from Query from the given index.
     * @param size Maximum number of results to retrieve.
     * @return A {@link RestResponse} that contains a {@link GetMultipleDataResult} that contains the clouds.
     */
    @ApiOperation(value = "Search for clouds.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<GetMultipleDataResult> search(@ApiParam(value = "Query to find the cloud.") @RequestParam(required = false) String query,
            @ApiParam(value = "If true only enabled plugins will be retrieved.") @RequestParam(required = false, defaultValue = "false") boolean enabledOnly,
            @ApiParam(value = "Query from the given index.") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Maximum number of results to retrieve.") @RequestParam(required = false, defaultValue = "20") int size) {
        FilterBuilder authorizationFilter = AuthorizationUtil.getResourceAuthorizationFilters();
        GetMultipleDataResult result = cloudService.get(query, enabledOnly, from, size, authorizationFilter);
        return RestResponseBuilder.<GetMultipleDataResult> builder().data(result).build();
    }

    @ApiOperation(value = "Enable a cloud.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id:.+}/enable", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> enableCloud(@ApiParam(value = "Id of the cloud to enable.", required = true) @PathVariable String id) {
        try {
            cloudService.enableCloud(id);
        } catch (PluginConfigurationException e) {
            log.error("Failed to enable cloud. PaaS provider plugin rejects the configuration of the plugin.", e);
            return RestResponseBuilder
                    .<Void> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.INVALID_PLUGIN_CONFIGURATION)
                            .message("The cloud configuration is not considered as valid by the plugin. cause: \n" + e.getMessage()).build()).build();
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Disable a cloud.", notes = "Note that if the method returns false as the RestResponse data, this means that disable is not possible because the cloud is used for some deployments.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id:.+}/disable", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Boolean> disableCloud(@ApiParam(value = "Id of the cloud to disable.", required = true) @PathVariable String id) {
        boolean disabledSuccess = cloudService.disableCloud(id);
        return RestResponseBuilder.<Boolean> builder().data(disabledSuccess).build();
    }

    @ApiOperation(value = "Get the current configuration for a cloud.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id:.+}/configuration", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Object> getConfiguration(
            @ApiParam(value = "Id of the cloud for which to get the configuration.", required = true) @PathVariable String id) {
        return RestResponseBuilder.builder().data(cloudService.getConfiguration(id)).build();
    }

    @ApiOperation(value = "Update the configuration for a cloud.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id:.+}/configuration", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> updateConfiguration(
            @ApiParam(value = "Id of the cloud for which to update the configuration.", required = true) @PathVariable String id,
            @ApiParam(value = "The configuration object for the cloud - Type depends of the selected PaaSProvider.", required = true) @RequestBody Object configuration) {
        try {
            Object configurationObject = cloudService.configurationAsValidObject(id, configuration);
            cloudService.updateConfiguration(id, configurationObject);
        } catch (IOException e) {
            log.error("Failed to update cloud configuration. Specified json cannot be processed.", e);
            return RestResponseBuilder
                    .<Void> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.INVALID_PLUGIN_CONFIGURATION).message("Fail to parse the provided plugin configuration.")
                            .build()).build();
        } catch (PluginConfigurationException e) {
            log.error("Failed to update cloud configuration.", e);
            return RestResponseBuilder
                    .<Void> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.INVALID_PLUGIN_CONFIGURATION)
                            .message("Fail to update cloud configuration because Plugin used is not valid.").build()).build();
        }

        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Get details for a cloud.
     *
     * @param cloudName name of the cloud to get.
     */
    @ApiOperation(value = "Get details of a cloud.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/getByName", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Cloud> getByName(
            @ApiParam(value = "Name of the cloud for which to get details.") @Valid @NotBlank @RequestParam(required = true) String cloudName) {
        return RestResponseBuilder.<Cloud> builder().data(cloudService.getByName(cloudName)).build();
    }

    /**
     * Add a role to a user on a specific cloud
     *
     * @param cloudId The cloud id.
     * @param username The username of the user to update roles.
     * @param role The role to add to the user on the cloud.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a user on a specific cloud", notes = "Only user with ADMIN role can assign any role to another user.")
    @RequestMapping(value = "/{cloudId}/userRoles/{username}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> addUserRole(@PathVariable String cloudId, @PathVariable String username, @PathVariable String role) {

        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        resourceRoleService.addUserRole(cloud, username, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Add a role to a group on a specific cloud
     *
     * @param cloudId The id of the cloud.
     * @param groupId The id of the group to update roles.
     * @param role The role to add to the group on the cloud.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Add a role to a group on a specific cloud", notes = "Only user with ADMIN role can assign any role to a group of users.")
    @RequestMapping(value = "/{cloudId}/groupRoles/{groupId}/{role}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> addGroupRole(@PathVariable String cloudId, @PathVariable String groupId, @PathVariable String role) {

        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        resourceRoleService.addGroupRole(cloud, groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on a specific cloud
     *
     * @param cloudId The id of the cloud.
     * @param username The username of the user to update roles.
     * @param role The role to add to the user on the cloud.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role to a user on a specific cloud", notes = "Only user with ADMIN role can unassign any role to another user.")
    @RequestMapping(value = "/{cloudId}/userRoles/{username}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> removeUserRole(@PathVariable String cloudId, @PathVariable String username, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        resourceRoleService.removeUserRole(cloud, username, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Remove a role from a user on a specific cloud
     *
     * @param cloudId The id of the cloud.
     * @param groupId The id of the group to update roles.
     * @param role The role to add to the user on the cloud.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Remove a role of a group on a specific cloud", notes = "Only user with ADMIN role can unassign any role to a group.")
    @RequestMapping(value = "/{cloudId}/groupRoles/{groupId}/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> removeGroupRole(@PathVariable String cloudId, @PathVariable String groupId, @PathVariable String role) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        resourceRoleService.removeGroupRole(cloud, groupId, role);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Add a cloud image to the given cloud", notes = "Only user with ADMIN role can add a cloud image.")
    @RequestMapping(value = "/{cloudId}/images", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<CloudComputeResourcesDTO> addCloudImage(@PathVariable String cloudId, @RequestBody String[] cloudImageIds) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        for (String cloudImageId : cloudImageIds) {
            cloudImageService.getCloudImageFailIfNotExist(cloudImageId);
            cloudService.addCloudImage(cloud, cloudImageId);
        }
        CloudComputeResourcesDTO cloudResourcesDTO = new CloudComputeResourcesDTO();
        cloudResourcesDTO.setComputeTemplates(cloud.getComputeTemplates());
        return RestResponseBuilder.<CloudComputeResourcesDTO> builder().data(cloudResourcesDTO).build();
    }

    @ApiOperation(value = "Remove a cloud image from the given cloud", notes = "Only user with ADMIN role can remove a cloud image.")
    @RequestMapping(value = "/{cloudId}/images/{cloudImageId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<CloudComputeResourcesDTO> removeCloudImage(@PathVariable String cloudId, @PathVariable String cloudImageId) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        CloudResourceMatcherConfig config = cloudService.findCloudResourceMatcherConfig(cloud);
        cloudService.removeCloudImage(cloud, config, cloudImageId);
        return RestResponseBuilder.<CloudComputeResourcesDTO> builder()
                .data(new CloudComputeResourcesDTO(cloud.getComputeTemplates(), config.getMatchedComputeTemplates())).build();
    }

    @ApiOperation(value = "Add a flavor to the given cloud", notes = "Only user with ADMIN role can add a cloud image.")
    @RequestMapping(value = "/{cloudId}/flavors", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<CloudComputeResourcesDTO> addCloudImageFlavor(@PathVariable String cloudId, @RequestBody CloudImageFlavor flavor) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        cloudService.addCloudImageFlavor(cloud, flavor);
        CloudComputeResourcesDTO cloudResourcesDTO = new CloudComputeResourcesDTO();
        cloudResourcesDTO.setComputeTemplates(cloud.getComputeTemplates());
        return RestResponseBuilder.<CloudComputeResourcesDTO> builder().data(cloudResourcesDTO).build();
    }

    @ApiOperation(value = "Remove a flavor from the given cloud", notes = "Only user with ADMIN role can add a cloud image.")
    @RequestMapping(value = "/{cloudId}/flavors/{flavorId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<CloudComputeResourcesDTO> removeCloudImageFlavor(@PathVariable String cloudId, @PathVariable String flavorId) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        CloudResourceMatcherConfig config = cloudService.findCloudResourceMatcherConfig(cloud);
        cloudService.removeCloudImageFlavor(cloud, config, flavorId);
        return RestResponseBuilder.<CloudComputeResourcesDTO> builder()
                .data(new CloudComputeResourcesDTO(cloud.getComputeTemplates(), config.getMatchedComputeTemplates())).build();
    }

    @ApiOperation(value = "Enable or disable a cloud template", notes = "Only user with ADMIN role can enable a cloud template.")
    @RequestMapping(value = "/{cloudId}/templates/{imageId}/{flavorId}/status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> setCloudComputeTemplateStatus(@PathVariable String cloudId, @PathVariable String imageId, @PathVariable String flavorId,
            @RequestParam Boolean enabled) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        cloudService.setCloudTemplateStatus(cloud, imageId, flavorId, enabled);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Set the corresponding paaS resource id for the cloud compute template", notes = "Only user with ADMIN role can set the resource id to a cloud compute template.")
    @RequestMapping(value = "/{cloudId}/templates/{imageId}/{flavorId}/resource", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> setCloudComputeTemplateResourceId(@PathVariable String cloudId, @PathVariable String imageId, @PathVariable String flavorId,
            @RequestParam(required = false) String resourceId) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        cloudService.setCloudTemplateResourceId(cloud, imageId, flavorId, resourceId);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Add a network to the given cloud", notes = "Only user with ADMIN role can add a network.")
    @RequestMapping(value = "/{cloudId}/networks", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> addNetwork(@PathVariable String cloudId, @RequestBody Network network) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        cloudService.addNetwork(cloud, network);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Remove a network from the given cloud", notes = "Only user with ADMIN role can remove a network.")
    @RequestMapping(value = "/{cloudId}/networks/{networkName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> removeNetwork(@PathVariable String cloudId, @PathVariable String networkName) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        CloudResourceMatcherConfig config = cloudService.findCloudResourceMatcherConfig(cloud);
        cloudService.removeNetwork(cloud, config, networkName);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Set the corresponding paaS resource id for the network", notes = "Only user with ADMIN role can set the resource id for a network.")
    @RequestMapping(value = "/{cloudId}/networks/{networkName}/resource", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> setNetworkResourceId(@PathVariable String cloudId, @PathVariable String networkName,
            @RequestParam(required = false) String resourceId) {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Cloud cloud = cloudService.getMandatoryCloud(cloudId);
        cloudService.setNetworkResourceId(cloud, networkName, resourceId);
        return RestResponseBuilder.<Void> builder().build();
    }
}
