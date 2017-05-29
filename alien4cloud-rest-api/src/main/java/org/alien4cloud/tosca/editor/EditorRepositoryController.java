package org.alien4cloud.tosca.editor;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.repository.Repository;
import alien4cloud.repository.model.AvailableTopologyRepositories;
import alien4cloud.repository.model.AvailableTopologyRepository;
import alien4cloud.repository.model.RepositoryPluginComponent;
import alien4cloud.repository.services.RepositoryService;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.context.ToscaContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * This controller provides helper operations to find repositories available in topology archive or configured in A4C.
 *
 * This is not intended to be a user API.
 */
@RestController
@RequestMapping({ "/rest/v2/editor/{topologyId}/availableRepositories", "/rest/latest/editor/{topologyId}/availableRepositories" })
@Api
public class EditorRepositoryController {

    @Inject
    private EditionContextManager editionContextManager;

    @Inject
    private RepositoryService repositoryService;

    /**
     * Get the available repositories regarding archive repositories and A4C repositories.
     */
    @ApiOperation(value = "Get the available repositories regarding archive repositories and A4C repositories", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<AvailableTopologyRepositories> getAvailableRepositories(
            @ApiParam(value = "The topology id.", required = true) @NotBlank @PathVariable final String topologyId) {

        try {
            editionContextManager.init(topologyId);
            Topology topology = EditionContextManager.getTopology();

            List<AvailableTopologyRepository> archiveRepositories = Lists.newArrayList();
            List<AvailableTopologyRepository> alienRepositories = Lists.newArrayList();
            List<String> repositoryTypes = Lists.newArrayList();

            AvailableTopologyRepositories result = new AvailableTopologyRepositories(archiveRepositories, alienRepositories, repositoryTypes);

            Set<String> repositoriesName = Sets.newHashSet();
            Set<String> repositoriesUrl = Sets.newHashSet();
            for (NodeTemplate node : topology.getNodeTemplates().values()) {
                if (node.getArtifacts() != null && CollectionUtils.isNotEmpty(node.getArtifacts().values())) {
                    for (DeploymentArtifact artifact : node.getArtifacts().values()) {
                        if (artifact.getRepositoryURL() != null && !repositoriesName.contains(artifact.getRepositoryName())) {
                            AvailableTopologyRepository atr = new AvailableTopologyRepository(artifact.getRepositoryName(), artifact.getArtifactRepository(), artifact.getRepositoryURL());
                            archiveRepositories.add(atr);
                            repositoriesName.add(artifact.getRepositoryName());
                            repositoriesUrl.add(artifact.getRepositoryURL());
                        }
                    }
                }
            }

            FilteredSearchRequest searchRequest = new FilteredSearchRequest();
            searchRequest.setFrom(0);
            searchRequest.setSize(Integer.MAX_VALUE);
            FacetedSearchResult<Repository> searchResult = repositoryService.search(searchRequest);
            for (Repository repository : searchResult.getData()) {
                String url = repositoryService.getRepositoryUrl(repository);
                if (!repositoriesUrl.contains(url)) {
                    // only return this repository if it's url doesn't not match existing archive repository url
                    AvailableTopologyRepository atr = new AvailableTopologyRepository(repository.getName(), repository.getRepositoryType(), url);
                    alienRepositories.add(atr);
                }
            }

            List<RepositoryPluginComponent> plugins = repositoryService.listPluginComponents();
            for (RepositoryPluginComponent repositoryPluginComponent : plugins) {
                repositoryTypes.add(repositoryPluginComponent.getRepositoryType());
            }

            return RestResponseBuilder.<AvailableTopologyRepositories>builder().data(result).build();
        }
        finally {
            ToscaContext.destroy();
        }
    }

}