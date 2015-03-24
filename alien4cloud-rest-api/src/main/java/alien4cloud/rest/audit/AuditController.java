package alien4cloud.rest.audit;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import alien4cloud.audit.AuditService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.audit.model.AuditConfiguration;
import alien4cloud.audit.model.AuditedMethod;
import alien4cloud.audit.model.Method;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
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
                if (existingMethodEnabled != null && !existingMethodEnabled.equals(methodEntry.getValue())) {
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
            Method auditedMethod = auditService.getAuditedMethod(method.getMethod());
            if (auditedMethod != null) {
                Audit audit = method.getMethodAnnotation(Audit.class);
                boolean enabledByDefault = (audit != null && audit.enabledByDefault());
                log.info("Audit method found {}, enabled by default {}", auditedMethod, enabledByDefault);
                allMethods.put(new Method(auditedMethod.getPath(), auditedMethod.getMethod()), enabledByDefault);
            } else {
                log.info("Audit ignore mapping {} for method {}", handlerMethodEntry.getKey(), method);
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

    @ApiOperation(value = "Enable/Disable audit", notes = "Audit configuration update is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/configuration/enabled", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> enableAudit(@RequestParam boolean enabled) {
        AuditConfiguration auditConfiguration = auditService.getMandatoryAuditConfiguration();
        auditConfiguration.setEnabled(enabled);
        auditService.saveAuditConfiguration(auditConfiguration);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Enable/Disable audit on a particular method", notes = "Audit configuration update is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/configuration/audited-methods", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> enableMethodAudit(@RequestBody AuditedMethod method) {
        AuditConfiguration auditConfiguration = auditService.getMandatoryAuditConfiguration();
        if (method.getMethod() == null || method.getPath() == null) {
            throw new InvalidArgumentException("Method's path or http method is null");
        }
        Map<Method, Boolean> auditedMethodsMap = auditConfiguration.getAuditedMethodsMap();
        Method auditedMethodKey = new Method(method.getPath(), method.getMethod());
        if (!auditedMethodsMap.containsKey(auditedMethodKey)) {
            throw new NotFoundException("Method " + method + " does not exist ");
        }
        auditedMethodsMap.put(auditedMethodKey, method.isEnabled());
        auditConfiguration.setAuditedMethodsMap(auditedMethodsMap);
        auditService.saveAuditConfiguration(auditConfiguration);
        return RestResponseBuilder.<Void> builder().build();
    }
}
