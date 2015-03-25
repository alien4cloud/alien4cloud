package alien4cloud.rest.audit;

import java.util.List;
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

import com.google.common.collect.Lists;
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

    private static interface IAuditedMethodFactory<T extends Method> {
        T buildAuditedMethod(Method auditedMethod, HandlerMethod method);
    }

    private Map<Method, Boolean> getAllAvailableMethodsForAudit() {
        return getAllAvailableMethodsForAudit(new IAuditedMethodFactory<Method>() {
            @Override
            public Method buildAuditedMethod(Method auditedMethod, HandlerMethod method) {
                return auditedMethod;
            }
        });
    }

    private <T extends Method> Map<T, Boolean> getAllAvailableMethodsForAudit(IAuditedMethodFactory<T> methodFactory) {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = this.requestMappingHandlerMapping.getHandlerMethods();
        Map<T, Boolean> allMethods = Maps.newHashMap();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> handlerMethodEntry : handlerMethods.entrySet()) {
            HandlerMethod method = handlerMethodEntry.getValue();
            Method auditedMethod = auditService.getAuditedMethod(method);
            if (auditedMethod != null) {
                Audit audit = method.getMethodAnnotation(Audit.class);
                boolean enabledByDefault = (audit != null && audit.enabledByDefault());
                allMethods.put(methodFactory.buildAuditedMethod(auditedMethod, method), enabledByDefault);
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

    @ApiOperation(value = "Reset the audit configuration", notes = "Reset the audit configuration to its default state. Audit search is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/configuration/reset", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<AuditConfigurationDTO> generateDefaultConfiguration() {
        AuditConfiguration auditConfiguration = new AuditConfiguration();
        Map<Method, Boolean> allAvailableMethodsForAudit = getAllAvailableMethodsForAudit();
        auditConfiguration.setAuditedMethodsMap(allAvailableMethodsForAudit);
        auditService.saveAuditConfiguration(auditConfiguration);
        return getAuditConfiguration();
    }

    @ApiOperation(value = "Get audit configuration", notes = "Get the audit configuration object. Audit configuration is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/configuration", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<AuditConfigurationDTO> getAuditConfiguration() {
        AuditConfiguration currentConfiguration = auditService.getMandatoryAuditConfiguration();
        boolean auditEnabled = currentConfiguration.isEnabled();
        Map<String, List<AuditedMethod>> methodsConfigurationDTO = Maps.newHashMap();
        for (AuditedMethod methodDTO : currentConfiguration.getAuditedMethods()) {
            List<AuditedMethod> currentMethodsForCategory = methodsConfigurationDTO.get(methodDTO.getCategory());
            if (currentMethodsForCategory == null) {
                currentMethodsForCategory = Lists.newArrayList();
                methodsConfigurationDTO.put(methodDTO.getCategory(), currentMethodsForCategory);
            }
            currentMethodsForCategory.add(methodDTO);
        }
        AuditConfigurationDTO auditConfigurationDTO = new AuditConfigurationDTO(auditEnabled, methodsConfigurationDTO);
        return RestResponseBuilder.<AuditConfigurationDTO> builder().data(auditConfigurationDTO).build();
    }

    @ApiOperation(value = "Enable/Disable audit", notes = "Audit configuration update is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/configuration/enabled", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> enableAudit(@RequestParam boolean enabled) {
        AuditConfiguration auditConfiguration = auditService.getMandatoryAuditConfiguration();
        auditConfiguration.setEnabled(enabled);
        auditService.saveAuditConfiguration(auditConfiguration);
        return RestResponseBuilder.<Void> builder().build();
    }

    private void enableMethodAudit(Map<Method, Boolean> auditedMethodsMap, AuditedMethod method) {
        if (method.getMethod() == null || method.getPath() == null) {
            throw new InvalidArgumentException("Method's path or http method is null");
        }
        Method auditedMethodKey = new Method(method.getPath(), method.getMethod(), method.getCategory(), method.getAction());
        if (!auditedMethodsMap.containsKey(auditedMethodKey)) {
            throw new NotFoundException("Method " + method + " does not exist ");
        }
        auditedMethodsMap.put(auditedMethodKey, method.isEnabled());
    }

    @ApiOperation(value = "Enable/Disable audit on a list of methods", notes = "Audit configuration update is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/configuration/audited-methods", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> enableMethodAudit(@RequestBody AuditedMethod[] methods) {
        AuditConfiguration auditConfiguration = auditService.getMandatoryAuditConfiguration();
        Map<Method, Boolean> auditedMethodsMap = auditConfiguration.getAuditedMethodsMap();
        for (AuditedMethod method : methods) {
            enableMethodAudit(auditedMethodsMap, method);
        }
        auditConfiguration.setAuditedMethodsMap(auditedMethodsMap);
        auditService.saveAuditConfiguration(auditConfiguration);
        return RestResponseBuilder.<Void> builder().build();
    }
}
