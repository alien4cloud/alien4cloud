package alien4cloud.webconfiguration;

import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.User;
import alien4cloud.security.users.JwtTokenService;
import com.google.common.collect.Sets;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.server.MaintenanceModeService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfiguration extends AbstractWebSocketMessageBrokerConfigurer {

    @Inject
    private JwtTokenService jwtTokenService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/rest/alienEndPoint").withSockJS().setSessionCookieNeeded(true);
        registry.addEndpoint("/rest/v1/alienEndPoint").withSockJS().setSessionCookieNeeded(true);
        registry.addEndpoint("/rest/latest/alienEndPoint").withSockJS().setSessionCookieNeeded(true);
        registry.addEndpoint("/rest/w4cAlienEndPoint").setAllowedOrigins("*").withSockJS().setInterceptors(new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Map<String, Object> map) throws Exception {
                try {
                    HttpServletRequest servletRequest = ((ServletServerHttpRequest) serverHttpRequest).getServletRequest();
                    String jwtToken = servletRequest.getParameter("jwtToken");
                    Jws<Claims> claims = jwtTokenService.validateJwtToken(jwtToken);
                    Authentication authentication = jwtTokenService.buildAuthenticationFromClaim(claims);
                    if (log.isDebugEnabled()) {
                        log.debug("/w4cAlienEndPoint was called and JWT authentication was success for principal " + authentication.getPrincipal());
                    }
                } catch(Exception e) {
                    log.warn("/w4cAlienEndPoint was called but JWT authentication failed !", e);
                    return false;
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Exception e) {

            }
        });
    }
}
