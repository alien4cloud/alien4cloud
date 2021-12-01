package alien4cloud.rest.component;

import java.lang.reflect.Array;
import java.util.*;

import javax.annotation.Resource;

import alien4cloud.utils.VersionUtil;
import org.alien4cloud.tosca.catalog.CatalogVersionResult;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.common.TagService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.utils.AlienConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle components
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/components", "/rest/v1/components", "/rest/latest/components" })
@Api(value = "", description = "Operations on Components")
public class ComponentController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    @Resource
    private IToscaTypeSearchService toscaTypeSearchService;

    @Resource
    private TagService tagService;

    /**
     * Get details for a component.
     *
     * @param id unique id of the component for which to get details.
     * @return A {@link RestResponse} that contains an {@link AbstractToscaType} .
     */
    @ApiOperation(value = "Get details for a component (tosca type) from it's id (including archive hash).")
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<AbstractToscaType> getComponent(@PathVariable String id, @RequestParam(required = false) QueryComponentType toscaType) {
        Class<? extends AbstractToscaType> queryClass = toscaType == null ? AbstractToscaType.class : toscaType.getIndexedToscaElementClass();
        AbstractToscaType component = dao.findById(queryClass, id);
        return RestResponseBuilder.<AbstractToscaType> builder().data(component).build();
    }

    /**
     * Get details for a component based on it's name and version.
     *
     * @param elementId unique id of the component for which to get details.
     * @param version unique id of the component for which to get details.
     * @return A {@link RestResponse} that contains an {@link AbstractToscaType} .
     */
    @ApiOperation(value = "Get details for a component (tosca type) from it's id (including archive hash).")
    @RequestMapping(value = "/element/{elementId:.+}/version/{version:.+}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<AbstractToscaType> getComponent(@PathVariable String elementId, @PathVariable String version,
            @RequestParam(required = false) QueryComponentType toscaType) {
        Class<? extends AbstractToscaType> queryClass = toscaType == null ? AbstractToscaType.class : toscaType.getIndexedToscaElementClass();
        AbstractToscaType component = toscaTypeSearchService.find(queryClass, elementId, version);
        return RestResponseBuilder.<AbstractToscaType> builder().data(component).build();
    }

    /**
     * Get all versions of a given component.
     *
     * @param elementId unique id of the component for which to get all other versions.
     * @return A {@link RestResponse} that contains an {@link AbstractToscaType} .
     */
    @ApiOperation(value = "Get details for a component (tosca type).")
    @RequestMapping(value = "/element/{elementId:.+}/versions", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<CatalogVersionResult[]> getComponentVersions(@PathVariable String elementId,
            @RequestParam(required = false) QueryComponentType toscaType) {
        Class<? extends AbstractToscaType> queryClass = toscaType == null ? AbstractToscaType.class : toscaType.getIndexedToscaElementClass();
        Object array = toscaTypeSearchService.findAll(queryClass, elementId);
        if (array != null) {
            int length = Array.getLength(array);
            CatalogVersionResult[] versions = new CatalogVersionResult[length];
            for (int i = 0; i < length; i++) {
                AbstractToscaType element = ((AbstractToscaType) Array.get(array, i));
                versions[i] = new CatalogVersionResult(element.getId(), element.getArchiveVersion());
            }
            Arrays.sort(versions, new Comparator<CatalogVersionResult>() {
                @Override
                public int compare(CatalogVersionResult o1, CatalogVersionResult o2) {
                    // reverse sort versions (most recent first)
                    return VersionUtil.compare(o2.getVersion(), o1.getVersion());
                }
            });
            return RestResponseBuilder.<CatalogVersionResult[]> builder().data(versions).build();
        }
        return RestResponseBuilder.<CatalogVersionResult[]> builder().data(new CatalogVersionResult[0]).build();
    }

    @ApiOperation(value = "Get details for a component (tosca type).")
    @RequestMapping(value = "/getInArchives", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<AbstractToscaType> getComponent(@RequestBody ElementFromArchiveRequest checkElementExistRequest) throws ClassNotFoundException {
        Class<? extends AbstractToscaType> elementClass = checkElementExistRequest.getComponentType().getIndexedToscaElementClass();
        AbstractToscaType element = toscaTypeSearchService.getElementInDependencies(elementClass, checkElementExistRequest.getElementName(),
                checkElementExistRequest.getDependencies());
        return RestResponseBuilder.<AbstractToscaType> builder().data(element).build();
    }

    /**
     * Check if an element exist in alien repository
     *
     * @param checkElementExistRequest request
     * @return A rest response that contains true if the element has been found in Alien repository, and false if not.
     * @throws ClassNotFoundException if the class of element is not available on server
     */
    @ApiOperation(value = "Verify that a component (tosca element) exists in alien's repository.")
    @RequestMapping(value = "/exist", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<Boolean> checkElementExist(@RequestBody ElementFromArchiveRequest checkElementExistRequest) throws ClassNotFoundException {
        Class<? extends AbstractToscaType> elementClass = checkElementExistRequest.getComponentType().getIndexedToscaElementClass();
        Boolean found = toscaTypeSearchService.isElementExistInDependencies(elementClass, checkElementExistRequest.getElementName(),
                checkElementExistRequest.getDependencies());
        return RestResponseBuilder.<Boolean> builder().data(found).build();
    }

    /**
     * Search for TOSCA elements.
     *
     * @param searchRequest The search request.
     * @return A {@link RestResponse} that contains a {@link FacetedSearchResult} of {@link NodeType}.
     */
    @ApiOperation(value = "Search for components (tosca types) in alien.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<FacetedSearchResult<? extends AbstractToscaType>> search(@RequestBody ComponentSearchRequest searchRequest) {
        Class<? extends AbstractToscaType> queryClass = searchRequest.getType() == null ? AbstractToscaType.class
                : searchRequest.getType().getIndexedToscaElementClass();
        FacetedSearchResult<? extends AbstractToscaType> searchResult = toscaTypeSearchService.search(queryClass, searchRequest.getQuery(),
                searchRequest.getSize(), searchRequest.getFilters());
        return RestResponseBuilder.<FacetedSearchResult<? extends AbstractToscaType>> builder().data(searchResult).build();
    }

    /**
     * Get the component recommended as default for a capability
     *
     * @param capability
     * @return A {@link RestResponse} that contains an {@link NodeType} .
     */
    @ApiOperation(value = "Get details for an indexed node type..")
    @RequestMapping(value = "/recommendation/{capability:.+}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<NodeType> getRecommendedForCapability(@PathVariable String capability) {
        NodeType component = getDefaultNodeForCapability(capability);
        return RestResponseBuilder.<NodeType> builder().data(component).build();
    }

    /**
     * define a component as default recommended for a specific capability.
     * Only one component can be recommended as default for a capability.
     *
     * @param recommendationRequest : {@link RecommendationRequest} object mapping the request body of the REST call.
     * @return A {@link RestResponse} that contains the component {@link NodeType} that has just been recommended.
     */
    @ApiOperation(value = "Set the given node type as default for the given capability.")
    @RequestMapping(value = "/recommendation", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    @Audit
    public RestResponse<NodeType> recommendComponentForCapability(@RequestBody RecommendationRequest recommendationRequest) {
        removeFromDefaultCapabilities(recommendationRequest.getCapability());

        NodeType component = dao.findById(NodeType.class, recommendationRequest.getComponentId());
        if (component != null) {
            if (component.getDefaultCapabilities() == null) {
                component.setDefaultCapabilities(new ArrayList<String>());
            }
            component.getDefaultCapabilities().add(recommendationRequest.getCapability());
            log.info("Defining the component <" + component.getId() + "> as default for the capability <" + recommendationRequest.getCapability() + ">.");
            dao.save(component);
        }
        return RestResponseBuilder.<NodeType> builder().data(component).build();
    }

    /**
     * un-define a component as default recommended for a specific capability.
     *
     * @param recommendationRequest : {@link RecommendationRequest} object mapping the request body of the REST call.
     * @return A {@link RestResponse} that contains the component {@link NodeType} that has just been undefined as default.
     */
    @ApiOperation(value = "Remove a recommendation for a node type.", notes = "If a node type is set as default for a given capability, you can remove this setting by calling this operation with the right request parameters.")
    @RequestMapping(value = "/unflag", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    @Audit
    public RestResponse<NodeType> unflagAsDefaultForCapability(@RequestBody RecommendationRequest recommendationRequest) {
        NodeType component = dao.findById(NodeType.class, recommendationRequest.getComponentId());
        if (component != null && component.getDefaultCapabilities() != null) {
            component.getDefaultCapabilities().remove(recommendationRequest.getCapability());
            log.info("Undefining the component <" + component.getId() + "> as default for the capability <" + recommendationRequest.getCapability() + ">.");
            dao.save(component);
        }
        return RestResponseBuilder.<NodeType> builder().data(component).build();
    }

    /**
     * Update or insert one tag for a given component
     *
     * @param componentId The if of the component for which to insert a tag.
     * @param updateTagRequest The request that contains the key and value for the tag to update.
     * @return a void rest response that contains no data if successful and an error if something goes wrong.
     */
    @ApiOperation(value = "Update or insert a tag for a component (tosca element).")
    @RequestMapping(value = "/{componentId:.+}/tags", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    @Audit
    public RestResponse<Void> upsertTag(@PathVariable String componentId, @RequestBody UpdateTagRequest updateTagRequest) {
        RestError updateComponantTagError = null;
        NodeType component = dao.findById(NodeType.class, componentId);
        if (component != null) {
            tagService.upsertTag(component, updateTagRequest.getTagKey(), updateTagRequest.getTagValue());
        } else {
            updateComponantTagError = RestErrorBuilder.builder(RestErrorCode.COMPONENT_MISSING_ERROR)
                    .message("Tag update operation failed. Could not find component with id <" + componentId + ">.").build();
        }

        return RestResponseBuilder.<Void> builder().error(updateComponantTagError).build();
    }

    @ApiOperation(value = "Delete a tag for a component (tosca element).")
    @RequestMapping(value = "/{componentId:.+}/tags/{tagId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    @Audit
    public RestResponse<Void> deleteTag(@PathVariable String componentId, @PathVariable String tagId) {
        RestError deleteComponentTagError = null;
        NodeType component = dao.findById(NodeType.class, componentId);
        if (component != null) {
            tagService.removeTag(component, tagId);
        } else {
            deleteComponentTagError = RestErrorBuilder.builder(RestErrorCode.COMPONENT_MISSING_ERROR)
                    .message("Tag delete operation failed. Could not find component with id <" + componentId + ">.").build();
        }
        return RestResponseBuilder.<Void> builder().error(deleteComponentTagError).build();
    }

    private void removeFromDefaultCapabilities(String capability) {
        NodeType component = getDefaultNodeForCapability(capability);
        if (component != null) {
            component.getDefaultCapabilities().remove(capability);
            dao.save(component);
        }
    }

    private NodeType getDefaultNodeForCapability(String capability) {
        Map<String, String[]> filters = new HashMap<>();
        filters.put(AlienConstants.DEFAULT_CAPABILITY_FIELD_NAME, new String[] { capability.toLowerCase() });
        GetMultipleDataResult result = dao.find(NodeType.class, filters, 1);
        if (result == null || result.getData() == null || result.getData().length == 0) {
            return null;
        }

        return (NodeType) result.getData()[0];
    }
}
