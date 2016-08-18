package alien4cloud.dao;

import alien4cloud.exception.IndexingServiceException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.elasticsearch.ElasticsearchException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Aspect to handles technical exceptions from Elastic Search.
 *
 * @author luc boutier
 */
@Aspect
@Component
public class ESExceptionHandlingAspect {

    @Around("target(alien4cloud.dao.ESIndexMapper)")
    public Object handleException(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (IOException | ExecutionException | ElasticsearchException e) {
            String message = logAndGetMessage(pjp, e);
            throw new IndexingServiceException(message, e);
        }
    }

    private String logAndGetMessage(ProceedingJoinPoint pjp, Throwable t) {
        String message = "Error when calling <" + pjp.getSignature().getDeclaringTypeName() + "> <" + pjp.getSignature().getName() + ">";
        ESIndexMapper.getLog().error(message, t);
        return message;
    }
}
