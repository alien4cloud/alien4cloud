package alien4cloud.rest.application;

import javax.annotation.Resource;

import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static alien4cloud.dao.ESIndexMapper.TYPE_NAME;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.ResponseUtil;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.rest.model.JsonRawRestResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin oriented bulk operations on application environments.
 */
@RestController
@RequestMapping({ "/rest/applications/environments", "/rest/v1/applications/environments", "/rest/latest/applications/environments" })
@Api(value = "", description = "Bulk API for application environments.")
public class EnvironmentBulkController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /** Bulk id request API. */
    @ApiOperation(value = "Get a list of environment from their ids.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/bulk/ids", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public JsonRawRestResponse getByIds(@RequestBody String[] deploymentIds) {
        // Check topology status for this deployment object
        MultiGetResponse response = alienDAO.getClient().prepareMultiGet()
                //.add(alienDAO.getIndexForType(ApplicationEnvironment.class), MappingBuilder.indexTypeFromClass(ApplicationEnvironment.class), deploymentIds)
                .add(MappingBuilder.indexTypeFromClass(ApplicationEnvironment.class), TYPE_NAME, deploymentIds)
                .get();
        JsonRawRestResponse restResponse = new JsonRawRestResponse();
        restResponse.setData(ResponseUtil.rawMultipleData(response));
        return restResponse;
    }
}
