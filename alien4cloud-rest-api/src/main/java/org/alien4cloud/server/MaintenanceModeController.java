package org.alien4cloud.server;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller to manage maintenance mode, enable, disable or get current maintenance task state.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/maintenance", "/rest/v1/maintenance", "/rest/latest/maintenance" })
@Api(value = "", description = "Maintenance mode operations.")
public class MaintenanceModeController {
    @Inject
    private MaintenanceModeService maintenanceModeService;

    /**
     * Enable maintenance mode.
     * 
     * @return A void rest response.
     * 
     */
    @ApiOperation(value = "Enable maintenance mode.", notes = "All requests but disable maintenance mode and get maintenance mode state will be denied.", authorizations = {
            @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> enable() {
        AuthorizationUtil.checkHasOneRoleIn(Role.ADMIN);

        maintenanceModeService.enable(AuthorizationUtil.getCurrentUser().getName());

        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Update maintenance state.
     *
     * @return A void rest response.
     *
     */
    @ApiOperation(value = "Update maintenance state.", notes = "Allow to add a message and eventually update progress of the maintenance.", authorizations = {
            @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> update(@RequestBody MaintenanceUpdateDTO updateDTO) {
        AuthorizationUtil.checkHasOneRoleIn(Role.ADMIN);

        maintenanceModeService.update(AuthorizationUtil.getCurrentUser().getName(), updateDTO.getMessage(), updateDTO.getProgressPercentage());

        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Get state on current maintenance task.
     * 
     * @return A rest response that contains the current maintenance mode task if any or null.
     */
    @ApiOperation(value = "Get state on current maintenance task.", notes = "Maintenance mode task contains an optional percentage element to provide state on maintenance progress.")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<MaintenanceModeState> get() {
        return RestResponseBuilder.<MaintenanceModeState> builder().data(maintenanceModeService.getMaintenanceModeState()).build();
    }

    /**
     * Disable maintenance mode.
     *
     * @return A void rest response.
     */
    @ApiOperation(value = "Disable maintenance mode.", notes = "Maintenance mode can be disabled only when it has been enabled by a user. Automatic maintenance (migrations etc) cannot be disabled through rest api.")
    @RequestMapping(method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> disable() {
        AuthorizationUtil.checkHasOneRoleIn(Role.ADMIN);

        maintenanceModeService.disable();

        return RestResponseBuilder.<Void> builder().build();
    }
}