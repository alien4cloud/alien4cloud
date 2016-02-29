package alien4cloud.rest.internal;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import springfox.documentation.annotations.ApiIgnore;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.configuration.ApplicationBootstrap;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import io.swagger.annotations.ApiOperation;

/**
 * Controllers that handle maintenance operations of the Alien platform.
 * For ex: disable/unload or enable/load all plugins and orchestrators in a migration/test scenario (hot swap data then reload).
 * These operations should only be used with care, the user must know what they are doing, so for the moment it's not exposed in API doc.
 */
@Slf4j
@ApiIgnore
@RestController
@RequestMapping({"/rest/maintenance", "/rest/v1/maintenance", "/rest/latest/maintenance"})
public class AlienMaintenanceController {

    @Resource
    private ApplicationBootstrap bootstrap;

    @ApiOperation(value = "Initialize the platform.", notes = "Initialize the platform, load all enabled plugins and orchestrator, should only be used for testing or maintenance purpose. Role required [ ADMIN ]")
    @RequestMapping(value = "/init-platform", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public DeferredResult<RestResponse<Void>> initializePlatform() {
        final DeferredResult<RestResponse<Void>> initResult = new DeferredResult<>(15L * 60L * 1000L);
        Futures.addCallback(bootstrap.bootstrap(), new FutureCallback<Object>() {

            @Override
            public void onSuccess(Object result) {
                initResult.setResult(RestResponseBuilder.<Void> builder().build());
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Unable to initialize platform", t);
                initResult.setErrorResult(RestResponseBuilder
                        .<Void> builder()
                        .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_STATE_OPERATION).message("Unable to bootstrap the platform " + t.getMessage())
                                .build()).build());
            }
        });
        return initResult;
    }

    @ApiOperation(value = "Teardown the platform.", notes = "Teardown the platform, unload all enabled plugins and orchestrator, should only be used for testing or maintenance purpose. Role required [ ADMIN ]")
    @RequestMapping(value = "/teardown-platform", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> teardownPlatform() {
        try {
            bootstrap.teardown();
            return RestResponseBuilder.<Void> builder().build();
        } catch (Exception e) {
            log.error("Unable to teardown platform", e);
            return RestResponseBuilder.<Void> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.ILLEGAL_STATE_OPERATION).message("Unable to teardown the platform").build()).build();
        }
    }
}
