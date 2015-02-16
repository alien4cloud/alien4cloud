package alien4cloud.rest.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import alien4cloud.Constants;
import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.common.Tag;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.rest.model.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * Handle components
 *
 * @author mourouvi
 */
@Slf4j
@RestController
@RequestMapping("/rest/components")
public class ComponentController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    @Resource
    private ICSARRepositorySearchService searchService;

    /**
     * Get details for a component.
     *
     * @param id unique id of the component for which to get details.
     * @return A {@link RestResponse} that contains an {@link IndexedToscaElement} .
     */
    @ApiOperation(value = "Get details for a component (tosca type).")
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET)
    public RestResponse<IndexedToscaElement> getComponent(@PathVariable String id) {
        IndexedToscaElement component = dao.findById(IndexedToscaElement.class, id.trim());
        return RestResponseBuilder.<IndexedToscaElement> builder().data(component).build();
    }

    @ApiOperation(value = "Get details for a component (tosca type).")
    @RequestMapping(value = "/getInArchives", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<IndexedToscaElement> getComponent(@RequestBody ElementFromArchiveRequest checkElementExistRequest) throws ClassNotFoundException {
        Class<? extends IndexedToscaElement> elementClass = checkElementExistRequest.getComponentType().getIndexedToscaElementClass();
        IndexedToscaElement element = searchService.getElementInDependencies(elementClass, checkElementExistRequest.getElementName(),
                checkElementExistRequest.getDependencies());
        return RestResponseBuilder.<IndexedToscaElement> builder().data(element).build();
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
    public RestResponse<Boolean> checkElementExist(@RequestBody ElementFromArchiveRequest checkElementExistRequest) throws ClassNotFoundException {
        Class<? extends IndexedToscaElement> elementClass = checkElementExistRequest.getComponentType().getIndexedToscaElementClass();
        Boolean found = searchService.isElementExistInDependencies(elementClass, checkElementExistRequest.getElementName(),
                checkElementExistRequest.getDependencies());
        return RestResponseBuilder.<Boolean> builder().data(found).build();
    }

    /**
     * Search for TOSCA elements.
     *
     * @param searchRequest The search request.
     * @param queryAllVersions Retrieve all versions of the component, by default set to false which means only retrieve the last recent version of the
     *            component
     * @return A {@link RestResponse} that contains a {@link FacetedSearchResult} of {@link IndexedNodeType}.
     */
    @ApiOperation(value = "Search for components (tosca types) in alien.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<FacetedSearchResult> search(@RequestBody SearchRequest searchRequest, @RequestParam(defaultValue = "false") boolean queryAllVersions) {
        Class<? extends IndexedToscaElement> classNameToQuery = searchRequest.getType() == null ? IndexedToscaElement.class : searchRequest.getType()
                .getIndexedToscaElementClass();
        if (!queryAllVersions) {
            Map<String, String[]> filters = searchRequest.getFilters();
            if (filters == null) {
                filters = Maps.newHashMap();
                searchRequest.setFilters(filters);
            }
            filters.put("highestVersion", new String[] { "true" });
        }
        FacetedSearchResult searchResult = dao.facetedSearch(classNameToQuery, searchRequest.getQuery(), searchRequest.getFilters(), "component_summary",
                searchRequest.getFrom(), searchRequest.getSize());
        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }

    /**
     * Get the component recommended as default for a capability
     *
     * @param capability
     * @return A {@link RestResponse} that contains an {@link IndexedNodeType} .
     */
    @ApiOperation(value = "Get details for an indexed node type..")
    @RequestMapping(value = "/recommendation/{capability:.+}", method = RequestMethod.GET)
    public RestResponse<IndexedNodeType> getRecommendedForCapability(@PathVariable String capability) {
        IndexedNodeType component = getDefaultNodeForCapability(capability);
        return RestResponseBuilder.<IndexedNodeType> builder().data(component).build();
    }

    /**
     * define a component as default recommended for a specific capability.
     * Only one component can be recommended as default for a capability.
     *
     * @param recommendationRequest : {@link RecommendationRequest} object mapping the request body of the REST call.
     * @return A {@link RestResponse} that contains the component {@link IndexedNodeType} that has just been recommended.
     */
    @ApiOperation(value = "Set the given node type as default for the given capability.")
    @RequestMapping(value = "/recommendation", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<IndexedNodeType> recommendComponentForCapability(@RequestBody RecommendationRequest recommendationRequest) {

        removeFromDefaultCapabilities(recommendationRequest.getCapability());

        IndexedNodeType component = dao.findById(IndexedNodeType.class, recommendationRequest.getComponentId());
        if (component != null) {
            if (component.getDefaultCapabilities() == null) {
                component.setDefaultCapabilities(new ArrayList<String>());
            }
            component.getDefaultCapabilities().add(recommendationRequest.getCapability());
            log.info("Defining the component <" + component.getId() + "> as default for the capability <" + recommendationRequest.getCapability() + ">.");
            dao.save(component);
        }
        return RestResponseBuilder.<IndexedNodeType> builder().data(component).build();
    }

    /**
     * un-define a component as default recommended for a specific capability.
     *
     * @param recommendationRequest : {@link RecommendationRequest} object mapping the request body of the REST call.
     * @return A {@link RestResponse} that contains the component {@link IndexedNodeType} that has just been undefined as default.
     */
    @ApiOperation(value = "Remove a recommendation for a node type.", notes = "If a node type is set as default for a given capability, you can remove this setting by calling this operation with the right request parameters.")
    @RequestMapping(value = "/unflag", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<IndexedNodeType> unflagAsDefaultForCapability(@RequestBody RecommendationRequest recommendationRequest) {
        IndexedNodeType component = dao.findById(IndexedNodeType.class, recommendationRequest.getComponentId());
        if (component != null && component.getDefaultCapabilities() != null) {
            component.getDefaultCapabilities().remove(recommendationRequest.getCapability());
            log.info("Undefining the component <" + component.getId() + "> as default for the capability <" + recommendationRequest.getCapability() + ">.");
            dao.save(component);
        }
        return RestResponseBuilder.<IndexedNodeType> builder().data(component).build();
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
    public RestResponse<Void> upsertTag(@PathVariable String componentId, @RequestBody UpdateTagRequest updateTagRequest) {
        RestError updateComponantTagError = null;
        IndexedNodeType component = dao.findById(IndexedNodeType.class, componentId);
        if (component != null) {
            if (!updateTagRequest.getTagKey().equals(Constants.ALIEN_INTERNAL_TAG)) {
                // Put the updated tag (will override the old tag or add it to the tag map)
                if (component.getTags() == null) {
                    component.setTags(Lists.<Tag> newArrayList());
                }
                Tag newTag = new Tag(updateTagRequest.getTagKey(), updateTagRequest.getTagValue());
                if (component.getTags().contains(newTag)) {
                    component.getTags().remove(newTag);
                }
                component.getTags().add(newTag);
                dao.save(component);
            } else {
                updateComponantTagError = RestErrorBuilder.builder(RestErrorCode.COMPONENT_INTERNALTAG_ERROR)
                        .message("Tag update operation failed. Could not update internal alien tag  <" + Constants.ALIEN_INTERNAL_TAG + ">.").build();
            }
        } else {
            updateComponantTagError = RestErrorBuilder.builder(RestErrorCode.COMPONENT_MISSING_ERROR)
                    .message("Tag update operation failed. Could not find component with id <" + componentId + ">.").build();
        }

        return RestResponseBuilder.<Void> builder().error(updateComponantTagError).build();
    }

    @ApiOperation(value = "Delete a tag for a component (tosca element).")
    @RequestMapping(value = "/{componentId:.+}/tags/{tagId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> deleteTag(@PathVariable String componentId, @PathVariable String tagId) {

        RestError deleteComponantTagError = null;
        IndexedNodeType component = dao.findById(IndexedNodeType.class, componentId);
        if (component != null) {

            if (!tagId.equals(Constants.ALIEN_INTERNAL_TAG)) {
                if (component.getTags() == null) {
                    return RestResponseBuilder.<Void> builder().error(deleteComponantTagError).build();
                }
                component.getTags().remove(new Tag(tagId, null));
                dao.save(component);
            } else {
                deleteComponantTagError = RestErrorBuilder.builder(RestErrorCode.COMPONENT_INTERNALTAG_ERROR)
                        .message("Tag delete operation failed. Could not delete internal alien tag  <" + Constants.ALIEN_INTERNAL_TAG + ">.").build();
            }
        } else {
            deleteComponantTagError = RestErrorBuilder.builder(RestErrorCode.COMPONENT_MISSING_ERROR)
                    .message("Tag delete operation failed. Could not find component with id <" + componentId + ">.").build();
        }

        return RestResponseBuilder.<Void> builder().error(deleteComponantTagError).build();
    }

    private void removeFromDefaultCapabilities(String capability) {
        IndexedNodeType component = getDefaultNodeForCapability(capability);
        if (component != null) {
            component.getDefaultCapabilities().remove(capability);
            dao.save(component);
        }
    }

    private IndexedNodeType getDefaultNodeForCapability(String capability) {
        Map<String, String[]> filters = new HashMap<>();
        filters.put(Constants.DEFAULT_CAPABILITY_FIELD_NAME, new String[] { capability.toLowerCase() });
        GetMultipleDataResult result = dao.find(IndexedNodeType.class, filters, 1);
        if (result == null || result.getData() == null || result.getData().length == 0) {
            return null;
        }

        return (IndexedNodeType) result.getData()[0];
    }
}
