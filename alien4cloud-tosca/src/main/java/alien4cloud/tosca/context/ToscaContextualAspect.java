package alien4cloud.tosca.context;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.topology.Topology;

/**
 * This aspect executes for ToscaContextual methods and ensure that a ToscaContext is defined (or creates one if not).
 */
@Slf4j
@Aspect
@Component
public class ToscaContextualAspect {

    @Around("@annotation(alien4cloud.tosca.context.ToscaContextual)")
    public Object ensureContext(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean initContext = false;
        if (ToscaContext.get() == null) {
            initContext = true;
        }
        try {
            // try to find dependencies from parameters
            joinPoint.getArgs();
            if (initContext) {
                Set<CSARDependency> dependencies = findDependencies(joinPoint.getArgs());
                log.info("Initializing Tosca Context with dependencies {}", dependencies);
                ToscaContext.init(dependencies);
            }
            return joinPoint.proceed();
        } finally {
            if (initContext) {
                log.info("Destroying Tosca Context");
                ToscaContext.destroy();
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
