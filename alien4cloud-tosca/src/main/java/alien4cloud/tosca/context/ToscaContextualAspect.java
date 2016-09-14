package alien4cloud.tosca.context;

import java.lang.reflect.Method;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.templates.Topology;

/**
 * This aspect executes for ToscaContextual methods and ensure that a ToscaContext is defined (or creates one if not).
 */
@Slf4j
@Aspect
@Component
public class ToscaContextualAspect {

    @Around("@annotation(alien4cloud.tosca.context.ToscaContextual)")
    public Object ensureContext(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method m = ms.getMethod();
        boolean requireNew = m.getAnnotation(ToscaContextual.class).requiresNew();

        boolean initContext = false;
        ToscaContext.Context existingContext = ToscaContext.get();
        if (requireNew || existingContext == null) {
            initContext = true;
        }
        try {
            // try to find dependencies from parameters
            joinPoint.getArgs();
            if (initContext) {
                Set<CSARDependency> dependencies = findDependencies(joinPoint.getArgs());
                log.debug("Initializing Tosca Context with dependencies {}", dependencies);
                ToscaContext.init(dependencies);
            }
            return joinPoint.proceed();
        } finally {
            if (initContext) {
                log.debug("Destroying Tosca Context");
                ToscaContext.destroy();
            }
            if (existingContext != null) {
                log.debug("Set back the existing context");
                ToscaContext.set(existingContext);
            }
        }
    }

    private Set<CSARDependency> findDependencies(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Topology) {
                return ((Topology) arg).getDependencies();
            }
            if (arg instanceof Set) {
                Set set = (Set) arg;
                if (set.size() > 0 && set.iterator().next() instanceof CSARDependency) {
                    return (Set<CSARDependency>) arg;
                }
            }
        }
        return Sets.<CSARDependency> newHashSet();
    }
}
