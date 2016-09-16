package org.alien4cloud.tosca.catalog;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.TopologyCatalogService;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller to access topology catalog features.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/catalog/topologies", "/rest/v1/catalog/topologies", "/rest/latest/catalog/topologies" })
public class TopologyCatalogController {

    @Inject
    private TopologyCatalogService catalogService;

    /**
     * Search for topologies in the catalog.
     *
     * @param searchRequest The search request.
     * @return A {@link RestResponse} that contains a {@link FacetedSearchResult} of {@link NodeType}.
     */
    @ApiOperation(value = "Search for components (tosca types) in alien.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<FacetedSearchResult<Topology>> search(@RequestBody FilteredSearchRequest searchRequest) {
        FacetedSearchResult<Topology> searchResult = catalogService.search(Topology.class, searchRequest.getQuery(), searchRequest.getSize(),
                searchRequest.getFilters());
        return RestResponseBuilder.<FacetedSearchResult<Topology>> builder().data(searchResult).build();
    }

    /**
     * Get all versions of the given topology.
     *
     * @param archiveName The name of the archive for which we want to get versions.
     * @return A {@link RestResponse} that contains an array of {@link CatalogVersionResult} .
     */
    @ApiOperation(value = "Get all the versions for a given archive (name)")
    @RequestMapping(value = "/{archiveName:.+}/versions", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<CatalogVersionResult[]> getVersions(@PathVariable String archiveName) {
        Topology[] topologies = catalogService.getAll(fromKeyValueCouples(), archiveName);
        if (topologies != null) {
            CatalogVersionResult[] versions = new CatalogVersionResult[topologies.length];
            for (int i = 0; i < topologies.length; i++) {
                Topology topology = topologies[i];
                versions[i] = new CatalogVersionResult(topology.getId(), topology.getArchiveVersion());
            }
            return RestResponseBuilder.<CatalogVersionResult[]> builder().data(versions).build();
        }
        return RestResponseBuilder.<CatalogVersionResult[]> builder().data(new CatalogVersionResult[0]).build();
    }
}