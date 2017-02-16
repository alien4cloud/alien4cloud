package alien4cloud.webconfiguration;

import alien4cloud.rest.utils.RestMapper;
import alien4cloud.utils.ReflectionUtil;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 */
@ControllerAdvice
public class RequestBodyOperationContextAdvice extends RequestBodyAdviceAdapter {
    @Override
    public boolean supports(MethodParameter methodParameter, Type type, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        try {
            Field field = ReflectionUtil.getDeclaredField(inputMessage.getClass(), "method");
            field.setAccessible(true);
            HttpMethod httpMethod = null;
            httpMethod = (HttpMethod) field.get(inputMessage);
            RestMapper.REQUEST_OPERATION.set(httpMethod.name());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("Bad luck...");
        }
        return super.beforeBodyRead(inputMessage, parameter, targetType, converterType);
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        // Note that this may not be called in case of exception so the variable may still be in thread until next request.
        RestMapper.REQUEST_OPERATION.remove();
        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
    }
}
