package alien4cloud.webconfiguration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

import alien4cloud.utils.AlienConstants;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.BuilderDefaults;
import springfox.documentation.spi.service.RequestHandlerProvider;
import springfox.documentation.spi.service.contexts.Orderings;

/**
 * Request handler provider that also manages plugins apis.
 */
@Component
@Profile(AlienConstants.API_DOC_PROFILE_FILTER)
public class RestDocumentationHandlerProvider implements RequestHandlerProvider {
    private final Set<RequestMappingInfoHandlerMapping> handlerMappings;

    @Autowired
    public RestDocumentationHandlerProvider(List<RequestMappingInfoHandlerMapping> handlerMappings) {
        this.handlerMappings = Sets.newHashSet(handlerMappings);
    }

    public void register(RequestMappingInfoHandlerMapping handler) {
        handlerMappings.add(handler);
    }

    public void unregister(RequestMappingInfoHandlerMapping handler) {
        handlerMappings.remove(handler);
    }

    @Override
    public List<RequestHandler> requestHandlers() {
        return Orderings.byPatternsCondition().sortedCopy(FluentIterable.from(BuilderDefaults.nullToEmptyList(this.handlerMappings))
                .transformAndConcat(this.toMappingEntries()).transform(this.toRequestHandler()));
    }

    private Function<? super RequestMappingInfoHandlerMapping, Iterable<Map.Entry<RequestMappingInfo, HandlerMethod>>> toMappingEntries() {
        return input -> input.getHandlerMethods().entrySet();
    }

    private Function<Map.Entry<RequestMappingInfo, HandlerMethod>, RequestHandler> toRequestHandler() {
        return input -> new RequestHandler(input.getKey(), input.getValue());
    }
}