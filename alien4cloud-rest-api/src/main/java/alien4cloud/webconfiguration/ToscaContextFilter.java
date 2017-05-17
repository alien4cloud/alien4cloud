package alien4cloud.webconfiguration;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import alien4cloud.tosca.context.ToscaContext;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link Filter} ensure the ToscaContext is empty at the beginning of each request and clean after
 */
@Slf4j
public class ToscaContextFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        destroyToscaContextIfNeeded("ToscaContext is not empty. Another request forget to destroy the ToscaContext attached to it !", request);
        try {
            chain.doFilter(request, response);
        } finally {
            destroyToscaContextIfNeeded("ToscaContext was NOT destroyed properly by this request !", request);
        }
    }

    private void destroyToscaContextIfNeeded(String message, ServletRequest request) {
        if (ToscaContext.get() != null) {
            ToscaContext.destroy();

            String url = "unknown";
            if (request instanceof HttpServletRequest) {
                url = ((HttpServletRequest) request).getRequestURI();
            }
            log.error("============================================================");
            log.error(url + " -> " + message);
            log.error("Fix me", new FixMeException());
            log.error("============================================================");
        }
    }

    public static class FixMeException extends RuntimeException{
    }

}
