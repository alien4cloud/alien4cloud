package alien4cloud.webconfiguration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.server.MaintenanceModeService;
import org.springframework.http.HttpStatus;

/**
 * {@link Filter} that checks that maintenance mode is not enabled. If the maintenance mode is enabled the filter throws an exception except on maintenance mode
 * management endpoints and login endpoints.
 */
@Slf4j
public class MaintenanceFilter implements Filter {
    @Setter
    private MaintenanceModeService maintenanceModeService;
    private String maintenanceJsonError;

    private List<String> authorizedPaths = Lists.newArrayList("/maintenance", "/modules", "/auth", "/admin/health");

    @SneakyThrows(JsonProcessingException.class)
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Load the current maintenance state
        RestResponse restResponse = RestResponseBuilder.<Void> builder()
                .error(RestErrorBuilder.builder(RestErrorCode.MAINTENANCE).message("Maintenance mode is enabled.").build()).build();

        ObjectMapper mapper = new ObjectMapper();
        maintenanceJsonError = mapper.writeValueAsString(restResponse);

        // Initialize the list of authorizedPaths
        List<String> fullAuthorizedPaths = Lists.newArrayList();
        authorizedPaths.stream().forEach(s -> {
            fullAuthorizedPaths.add("/rest" + s);
            fullAuthorizedPaths.add("/rest/v1" + s);
            fullAuthorizedPaths.add("/rest/latest" + s);
        });
        authorizedPaths = fullAuthorizedPaths;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // Check if we are in maintenance mode
        if (maintenanceModeService != null && maintenanceModeService.isMaintenanceModeEnabled()) {
            final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
            if (isDeniedPath(httpRequest.getRequestURL().toString())) {
                log.debug("Maintenance mode is enabled and deny request for {}", httpRequest.getRequestURL());

                HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
                httpResponse.getWriter().write(maintenanceJsonError);
                httpResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                return;
            }
        }

        // Proceed with the request.
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean isDeniedPath(String requestPath) {
        for (String authorizedPath : authorizedPaths) {
            if (requestPath.contains(authorizedPath)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void destroy() {
    }
}