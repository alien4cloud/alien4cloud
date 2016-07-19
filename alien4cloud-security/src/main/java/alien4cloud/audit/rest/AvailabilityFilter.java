package alien4cloud.audit.rest;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import alien4cloud.events.HALeaderElectionEvent;

/**
 * A filter to avoid requests while not available.
 * 
 * TODO: should not be activated if HA is not available !!
 * TODO: add filtering on consul server IPs to avoid DoS
 */
@Component
@Slf4j
public class AvailabilityFilter extends OncePerRequestFilter implements Ordered, ApplicationListener<HALeaderElectionEvent> {

    private static final ThreadLocal<Pattern> ELECT_DETECTION_PATTERN = new ThreadLocal<Pattern> () {
        @Override
        protected Pattern initialValue() {
            // TODO remove elect when consul will be used
            return Pattern.compile("/rest/(latest|[v|V]\\d+)/health/(elect|check)");
        }
    };

    @Value("${ha.ha_enabled:#{false}}")
    private boolean haEnabled;

    // TODO use http://projects.spring.io/spring-statemachine/#quick-start
    private volatile boolean init = true;

    private volatile boolean available;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 9;
    }

    private boolean isFiltered(String uri) {
        Matcher matcher = ELECT_DETECTION_PATTERN.get().matcher(uri);
        if (log.isTraceEnabled()) {
            log.trace("Matcher matches ? {}", matcher.matches());
        }
        if (matcher.matches()) {
            boolean isCheck = matcher.group(2).equals("check");
            if (isCheck && !init && !available) {
                // TODO use http://projects.spring.io/spring-statemachine/#quick-start
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!haEnabled) {
            filterChain.doFilter(request, response);
            return;
        }
        if (isFiltered(request.getRequestURI()) && !available) {
            if (log.isTraceEnabled()) {
                log.trace("Application not availaible (not elected), refusing requests");
            }
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            return;
        }
        filterChain.doFilter(request, response);
        return;
    }

    @Override
    public void onApplicationEvent(HALeaderElectionEvent event) {
        log.info("Election event received. Am I availaible ? => {}", event.isLeader());
        available = event.isLeader();
        if (available && init) {
            // TODO use http://projects.spring.io/spring-statemachine/#quick-start
            init = false;
        }
    }
}
