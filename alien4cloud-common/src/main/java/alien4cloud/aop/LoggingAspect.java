package alien4cloud.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("@annotation(LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = 0;

        if (log.isDebugEnabled()) {
            start = System.currentTimeMillis();
        }

        Object proceed = joinPoint.proceed();

        if (log.isDebugEnabled()) {
            log.info("{} took {} ms",joinPoint.getSignature(),System.currentTimeMillis()-start);
        }

        return proceed;
    }
}
