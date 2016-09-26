package org.alien4cloud.tosca.catalog;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.common.AlienConstants;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.rest.application.model.CreateTopologyRequest;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.catalog.index.ITopologyCatalogService;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.validation.Valid;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

/**
 * Controller to access topology catalog features.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/catalog/topologies", "/rest/v1/catalog/topologies", "/rest/latest/catalog/topologies" })
public class TopologyCatalogController {

    @Inject
    private ITopologyCatalogService catalogService;

    /**
     * Search for topologies in the catalog.
     *
     * @param searchRequest The search request.
     * @return A {@link RestResponse} that contains a {@link FacetedSearchResult} of {@link NodeType}.
     */
    @ApiOperation(value = "Search for topologies in the catalog.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER', 'ARCHITECT')")
    public RestResponse<FacetedSearchResult<Topology>> search(@RequestBody FilteredSearchRequest searchRequest) {
        FacetedSearchResult<Topology> searchResult = catalogService.search(Topology.class, searchRequest.getQuery(), searchRequest.getSize(),
                searchRequest.getFilters());
        return RestResponseBuilder.<FacetedSearchResult<Topology>> builder().data(searchResult).build();
    }

    /**
     * Create a topology and register it as a template in the catalog .
     *
     * @param createTopologyRequest The create topology template request.
     * @return A {@link RestResponse} that contains the Id of the newly created topology.
     */
    @ApiOperation(value = "Create a topology and register it in the catalog")
    @RequestMapping(value = "/template", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ARCHITECT')")
    @Audit
    public RestResponse<String> createAsTemplate(@RequestBody @Valid CreateTopologyRequest createTopologyRequest) {
        Topology topology = catalogService.createTopologyAsTemplate(createTopologyRequest.getName(), createTopologyRequest.getDescription(),
                createTopologyRequest.getVersion(), AlienConstants.GLOBAL_WORKSPACE_ID, createTopologyRequest.getFromTopologyId());
        return RestResponseBuilder.<String> builder().data(topology.getId()).build();
    }

    /**
     * Get all versions of the given topology.
     *
     * @param archiveName The name of the archive for which we want to get versions.
     * @return A {@link RestResponse} that contains an array of {@link CatalogVersionResult} .
     */
    @ApiOperation(value = "Get all the versions for a given archive (name)")
    @RequestMapping(value = "/{archiveName:.+}/versions", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER', 'ARCHITECT')")
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

    /**
     * Get a specific topology from it's id.
     *
     * @param id The name of the archive for which we want to get versions.
     * @return A {@link RestResponse} that contains a of {@link Topology} .
     */
    @ApiOperation(value = "Get a specific topology from it's id.")
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER', 'ARCHITECT')")
    public RestResponse<Topology> getTopology(@PathVariable String id) {
        return RestResponseBuilder.<Topology> builder().data(catalogService.getOrFail(id)).build();
    }
}