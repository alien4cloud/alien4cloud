package alien4cloud.rest.component;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.utils.ToscaWithDependenciesBuilder;
import org.alien4cloud.tosca.utils.TypeWithDependenciesResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import springfox.documentation.annotations.ApiIgnore;

/**
 * This is currently an ui oriented controller that provides types information and dependencies.
 *
 * It should evolve to replace the component controller and provide type catalog REST API.
 */
@ApiIgnore
@RestController
@RequestMapping({ "/rest/catalog/types", "/rest/v1/catalog/types", "/rest/latest/catalog/types" })
public class ToscaTypeController {
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Inject
    private ToscaWithDependenciesBuilder toscaWithDependenciesBuilder;

    /**
     * Get a Tosca Type and all related dependency types.
     * 
     * @param typeId The id (element id) of the tosca type.
     * @param typeVersion The version of the tosca type.
     * @return A {@link RestResponse} that contains an {@link TypeWithDependenciesResult} .
     */
    @RequestMapping(value = "adv/typewithdependencies/{typeId:.+}/{typeVersion:.+}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<TypeWithDependenciesResult> getTypeWithDependencies(@PathVariable String typeId, @PathVariable String typeVersion) {
        AbstractToscaType type = toscaTypeSearchService.findOrFail(AbstractToscaType.class, typeId, typeVersion);
        return RestResponseBuilder.<TypeWithDependenciesResult> builder().data(toscaWithDependenciesBuilder.buildTypeWithDependencies(type)).build();
    }
}
