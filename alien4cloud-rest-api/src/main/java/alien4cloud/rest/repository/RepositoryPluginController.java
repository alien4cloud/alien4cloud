package alien4cloud.rest.repository;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.repository.model.RepositoryPluginComponent;
import alien4cloud.repository.services.RepositoryService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RestController
@RequestMapping(value = { "/rest/repository-plugins", "/rest/v1/repository-plugins",
        "/rest/latest/repository-plugins" }, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "List Repository Plugins", description = "Allow to list all repository plugins (artifact resolver).", authorizations = {
        @Authorization("COMPONENTS_MANAGER") })
public class RepositoryPluginController {

    @Resource
    private RepositoryService repositoryService;

    @ApiOperation(value = "Search for repository resolver plugins.", authorizations = { @Authorization("COMPONENTS_MANAGER") })
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    public RestResponse<List<RepositoryPluginComponent>> listRepositoryResolverPlugins() {
        return RestResponseBuilder.<List<RepositoryPluginComponent>> builder().data(repositoryService.listPluginComponents()).build();
    }
}
