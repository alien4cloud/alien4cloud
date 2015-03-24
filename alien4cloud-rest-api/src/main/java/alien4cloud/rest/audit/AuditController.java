package alien4cloud.rest.audit;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import alien4cloud.audit.AuditService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.audit.model.AuditConfiguration;
import alien4cloud.audit.model.Method;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/audit")
@Slf4j
public class AuditController {

    @Resource
    private AuditService auditService;

    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @PostConstruct
    private void postConstruct() {
        AuditConfiguration auditConfiguration = auditService.getAuditConfiguration();
        Map<Method, Boolean> allAvailableMethodsForAudit = getAllAvailableMethodsForAudit();
        if (auditConfiguration == null) {
            log.info("Generate default configuration for audit");
            auditConfiguration = new AuditConfiguration();
        } else {
            log.info("Try to merge with existing audit configuration");
            Map<Method, Boolean> existingMethodsMap = auditConfiguration.getAuditedMethodsMap();
            for (Map.Entry<Method, Boolean> methodEntry : allAvailableMethodsForAudit.entrySet()) {
                Boolean existingMethodEnabled = existingMethodsMap.get(methodEntry.getKey());
                if (existingMethodEnabled != null) {
                    methodEntry.setValue(existingMethodEnabled);
                }
            }
        }
        auditConfiguration.setAuditedMethodsMap(allAvailableMethodsForAudit);
        auditService.saveAuditConfiguration(auditConfiguration);
    }

    private Map<Method, Boolean> getAllAvailableMethodsForAudit() {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = this.requestMappingHandlerMapping.getHandlerMethods();
        Map<Method, Boolean> allMethods = Maps.newHashMap();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> handlerMethodEntry : handlerMethods.entrySet()) {
            HandlerMethod method = handlerMethodEntry.getValue();
            RequestMapping requestMapping = method.getMethodAnnotation(RequestMapping.class);
            Method auditedMethod = auditService.getAuditedMethod(requestMapping);
            if (auditedMethod != null) {
                Audit audit = method.getMethodAnnotation(Audit.class);
                allMethods.put(new Method(auditedMethod.getPath(), auditedMethod.getMethod()), audit != null && audit.enabledByDefault());
            }
        }
        return allMethods;
    }

    /**
     * Search for audit trace
     *
     * @param searchRequest The element that contains criterias for search operation.
     * @return A rest response that contains a {@link FacetedSearchResult} containing audit trace.
     */
    @ApiOperation(value = "Search for audit trace", notes = "Returns a search result with that contains auti traces matching the request. Audit search is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<FacetedSearchResult> search(@RequestBody SearchRequest searchRequest) {
        FacetedSearchResult searchResult = auditService.searchAuditTrace(searchRequest.getQuery(), searchRequest.getFilters(), searchRequest.getFrom(),
                searchRequest.getSize());
        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }

    @ApiOperation(value = "Get audit configuration", notes = "Get the audit configuration object. Audit configuration is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/configuration", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<AuditConfiguration> getAuditConfiguration() {
        AuditConfiguration auditConfiguration = auditService.getMandatoryAuditConfiguration();
        return RestResponseBuilder.<AuditConfiguration> builder().data(auditConfiguration).build();
    }

    @ApiOperation(value = "Update audit configuration", notes = "Update the audit configuration object. Audit configuration is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/configuration", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> updateAuditConfiguration(@RequestBody AuditConfiguration auditConfiguration) {
        if (auditConfiguration == null) {
            throw new InvalidArgumentException("Cannot save null audit configuration");
        }
        auditService.saveAuditConfiguration(auditConfiguration);
        return RestResponseBuilder.<Void> builder().build();
    }
}
