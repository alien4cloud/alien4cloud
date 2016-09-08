package alien4cloud.audit.rest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.google.common.collect.Sets;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.audit.AuditService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.audit.model.AuditConfiguration;
import alien4cloud.audit.model.AuditedMethod;
import alien4cloud.audit.model.Method;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping({ "/rest/audit", "/rest/v1/audit", "/rest/latest/audit" })
@Slf4j
public class AuditController {

    @Resource
    private AuditService auditService;

    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    // Some mapping are registered from child context in a dynamic fashion (Alien main context, plugins).
    private Set<RequestMappingHandlerMapping> registeredRequestMappingHandlerMapping = Sets.newHashSet();

    @PostConstruct
    private void postConstruct() {
        AuditConfiguration auditConfiguration = auditService.getAuditConfiguration();
        Map<Method, Boolean> allAvailableMethodsForAudit = getAllAvailableMethodsForAudit(requestMappingHandlerMapping);
        if (auditConfiguration == null) {
            log.info("Generate default configuration for audit");
            auditConfiguration = new AuditConfiguration();
        } else {
            log.info("Try to merge with existing audit configuration");
            Map<Method, Boolean> existingMethodsMap = auditConfiguration.getAuditedMethodsMap();
            allAvailableMethodsForAudit.putAll(existingMethodsMap);
        }
        auditConfiguration.setAuditedMethodsMap(allAvailableMethodsForAudit);
        auditService.saveAuditConfiguration(auditConfiguration);
    }

    private interface IAuditedMethodFactory<T extends Method> {
        T buildAuditedMethod(Method auditedMethod, HandlerMethod method);
    }

    private Map<Method, Boolean> getAllAvailableMethodsForAudit(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        return getAllAvailableMethodsForAudit(requestMappingHandlerMapping, (auditedMethod, method) -> auditedMethod);
    }

    private <T extends Method> Map<T, Boolean> getAllAvailableMethodsForAudit(RequestMappingHandlerMapping requestMappingHandlerMapping,
            IAuditedMethodFactory<T> methodFactory) {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
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
     * Register a dynamic RequestMappingHandlerMapping in the audit management system.
     * 
     * @param requestMappingHandlerMapping The dynamic RequestMappingHandlerMapping to handle for audit.
     */
    public void register(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        registeredRequestMappingHandlerMapping.add(requestMappingHandlerMapping);
        // update configuration to inclure methods from plugin or context
        Map<Method, Boolean> allAvailableMethodsForAudit = getAllAvailableMethodsForAudit(requestMappingHandlerMapping);
        AuditConfiguration configuration = auditService.getAuditConfiguration();
        // Put all in new map to not override existing user settings if some methods are already defined.
        allAvailableMethodsForAudit.putAll(configuration.getAuditedMethodsMap());
        configuration.setAuditedMethodsMap(allAvailableMethodsForAudit);
        auditService.saveAuditConfiguration(configuration);
    }

    /**
     * Unregister a dynamic RequestMappingHandlerMapping
     * 
     * @param requestMappingHandlerMapping the dynamic RequestMappingHandlerMapping to unregister.
     */
    public void unRegister(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        registeredRequestMappingHandlerMapping.remove(requestMappingHandlerMapping);
        // TODO we should cleanup configuration when a plugin is removed.
    }

    /**
     * Search for audit trace
     *
     * @param searchRequest The element that contains criterias for search operation.
     * @return A rest response that contains a {@link FacetedSearchResult} containing audit trace.
     */
    @ApiOperation(value = "Search for audit trace", notes = "Returns a search result with that contains auti traces matching the request. Audit search is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<FacetedSearchResult> search(@RequestBody FilteredSearchRequest searchRequest) {
        FacetedSearchResult searchResult = auditService.searchAuditTrace(searchRequest.getQuery(), searchRequest.getFilters(), searchRequest.getFrom(),
                searchRequest.getSize());
        if (searchRequest.getFilters() == null || !searchRequest.getFilters().containsKey("category")) {
            searchResult.getFacets().remove("action");
        }
        return RestResponseBuilder.<FacetedSearchResult> builder().data(searchResult).build();
    }

    @ApiOperation(value = "Reset the audit configuration", notes = "Reset the audit configuration to its default state. Audit search is only accessible to user with role [ ADMIN ]")
    @RequestMapping(value = "/configuration/reset", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<AuditConfigurationDTO> generateDefaultConfiguration() {
        AuditConfiguration auditConfiguration = new AuditConfiguration();
        Map<Method, Boolean> allAvailableMethodsForAudit = getAllAvailableMethodsForAudit(requestMappingHandlerMapping);
        for (RequestMappingHandlerMapping registeredHandlerMapping : this.registeredRequestMappingHandlerMapping) {
            allAvailableMethodsForAudit.putAll(getAllAvailableMethodsForAudit(registeredHandlerMapping));
        }
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
        if (method.getMethod() == null) {
            throw new InvalidArgumentException("Method's path or http method is null");
        }
        Method auditedMethodKey = new Method(method.getSignature(), method.getMethod(), method.getCategory(), method.getAction());
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
