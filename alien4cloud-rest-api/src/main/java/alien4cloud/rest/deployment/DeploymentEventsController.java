package alien4cloud.rest.deployment;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.ResponseUtil;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.rest.deployment.model.GetMultipleJsonResult;
import alien4cloud.rest.deployment.model.ScrollJsonResult;
import alien4cloud.rest.deployment.model.ScrollTimedRequest;
import alien4cloud.rest.deployment.model.TimedRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

/**
 * API to access deployments events.
 */
@RestController
@RequestMapping({ "/rest/deployments/events", "/rest/v1/deployments/events", "/rest/latest/deployments/events" })
@Api(value = "Deployment events query API", description = "This api allows to perfom admin oriented requests on deployment events.")
public class DeploymentEventsController {
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO monitorDao;

    /**
     * Batch processing oriented API to retrieve deployment status events. This API is not intended for frequent requests but can retrieve lot of data.
     *
     * @return
     */
    @ApiOperation(value = "Get deployment status events from a given date.", notes = "Batch processing oriented API to retrieve deployment status events. This API is not intended for frequent requests but can retrieve lot of data.", authorizations = {
            @Authorization("ADMIN"), @Authorization("APPLICATION_MANAGER") })
    @RequestMapping(value = "/status", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public GetMultipleJsonResult get(@RequestBody @Valid TimedRequest timedRequest) {
        RangeQueryBuilder dateFilter = QueryBuilders.rangeQuery("date").gte(timedRequest.getIntervalStart());
        if (timedRequest.getIntervalEnd() != null) {
            dateFilter.lt(timedRequest.getIntervalEnd());
        }

        SearchResponse response = monitorDao.getClient().prepareSearch(monitorDao.getIndexForType(PaaSDeploymentStatusMonitorEvent.class))
                .setTypes(MappingBuilder.indexTypeFromClass(PaaSDeploymentStatusMonitorEvent.class)).setQuery(QueryBuilders.constantScoreQuery(dateFilter))
                .setFrom(timedRequest.getFrom()).setSize(timedRequest.getSize()).get();

        GetMultipleJsonResult result = new GetMultipleJsonResult();
        result.setData(ResponseUtil.rawMultipleData(response));
        result.setTotalResults(response.getHits().getTotalHits());
        result.setQueryDuration(response.getTook().getMillis());
        return result;
    }

    /**
     * Batch processing oriented API to retrieve deployment status events. This API is not intended for frequent requests but can retrieve lot of data.
     * 
     * @return
     */
    @ApiOperation(value = "Get deployment status events from a given date.", notes = "Batch processing oriented API to retrieve deployment status events. This API is not intended for frequent requests but can retrieve lot of data.", authorizations = {
            @Authorization("ADMIN"), @Authorization("APPLICATION_MANAGER") })
    @RequestMapping(value = "/status/scroll", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ScrollJsonResult get(@RequestBody @Valid ScrollTimedRequest timedRequest) {
        RangeQueryBuilder dateFilter = QueryBuilders.rangeQuery("date").gte(timedRequest.getIntervalStart());
        if (timedRequest.getIntervalEnd() != null) {
            dateFilter.lt(timedRequest.getIntervalEnd());
        }

        SearchResponse response = monitorDao.getClient().prepareSearch(monitorDao.getIndexForType(PaaSDeploymentStatusMonitorEvent.class))
                .setTypes(MappingBuilder.indexTypeFromClass(PaaSDeploymentStatusMonitorEvent.class)).setQuery(QueryBuilders.constantScoreQuery(dateFilter))
                .setScroll(TimeValue.timeValueMinutes(5)).setSize(timedRequest.getSize()).get();

        return convert(response);
    }

    /**
     * Batch processing oriented API to retrieve deployment status events. This API is not intended for frequent requests but can retrieve lot of data.
     *
     * @return
     */
    @ApiOperation(value = "Get deployment status events from a given date.", notes = "Batch processing oriented API to retrieve deployment status events. This API is not intended for frequent requests but can retrieve lot of data.", authorizations = {
            @Authorization("ADMIN"), @Authorization("APPLICATION_MANAGER") })
    @RequestMapping(value = "/status/scroll", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ScrollJsonResult get(@RequestParam String scrollId) {
        SearchResponse response = monitorDao.getClient().prepareSearchScroll(scrollId).setScroll(TimeValue.timeValueMinutes(5)).get();
        return convert(response);
    }

    private ScrollJsonResult convert(SearchResponse response) {
        ScrollJsonResult result = new ScrollJsonResult();
        result.setData(ResponseUtil.rawMultipleData(response));
        result.setScrollId(response.getScrollId());
        result.setTotalResults(response.getHits().getTotalHits());
        result.setQueryDuration(response.getTook().getMillis());
        return result;
    }
}
