package alien4cloud.rest.paas;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.rest.application.model.SearchLogRequest;
import alien4cloud.rest.paas.services.LogService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping({ "/rest/deployment/logs", "/rest/v1/deployment/logs", "/rest/latest/deployment/logs" })
@Slf4j
@Api(value = "", description = "Administration api for deployment logs access.")
public class LogController {
    @Inject
    private LogService logService;

    private final static String LOG_FILTER = "LOG_FILTER";
    /**
     * Search logs for a given deployment.
     *
     * @param searchRequest The element that contains criterias for search operation.
     * @return A rest response that contains a {@link FacetedSearchResult} containing applications.
     */
    @ApiOperation(value = "Search for logs of a given deployment", notes = "Returns a search result with that contains logs matching the request. ")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult<PaaSDeploymentLog>> search(@RequestBody SearchLogRequest searchRequest) {
        return logService.doSearch(searchRequest);
    }

    @ApiOperation(value = "Download logs of a given deployment", notes = "Returns a file containing the logs which matches the filter. ")
    @RequestMapping(value = "/download", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public void download(@RequestParam String req, HttpServletResponse response) throws IOException {
        SearchLogRequest logRequest = JsonUtil.readObject(req, SearchLogRequest.class);
        String fName = logService.getDownloadFileName(logRequest);
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fName +"\"");
        logService.downloadLogs(logRequest, response.getOutputStream());
    }
    
}