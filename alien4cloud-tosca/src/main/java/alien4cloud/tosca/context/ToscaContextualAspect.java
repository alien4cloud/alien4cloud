package alien4cloud.tosca.context;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.exception.NotFoundException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * This aspect executes for ToscaContextual methods and ensure that a ToscaContext is defined (or creates one if not).
 */
@Slf4j
@Aspect
@Component
public class ToscaContextualAspect {
    @Resource
    private ICSARRepositorySearchService csarRepositorySearchService;

    @Around("@annotation(alien4cloud.tosca.context.ToscaContextual)")
    public Object ensureContext(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method m = ms.getMethod();
        boolean requireNew = m.getAnnotation(ToscaContextual.class).requiresNew();

        return execInToscaContext(new Supplier<Object>() {
            @Override
            @SneakyThrows
            public Object get() {
                return joinPoint.proceed();
            }
        }, requireNew, joinPoint.getArgs());
        // boolean initContext = false;
        // ToscaContext.Context existingContext = ToscaContext.get();
        // if (requireNew || existingContext == null) {
        // initContext = true;
        // }
        // try {
        // // try to find dependencies from parameters
        // if (initContext) {
        // Set<CSARDependency> dependencies = findDependencies(joinPoint.getArgs());
        // log.debug("Initializing Tosca Context with dependencies {}", dependencies);
        // ToscaContext.init(dependencies);
        // }
        // return joinPoint.proceed();
        // } finally {
        // if (initContext) {
        // log.debug("Destroying Tosca Context");
        // ToscaContext.destroy();
        // }
        // if (existingContext != null) {
        // log.debug("Set back the existing context");
        // ToscaContext.set(existingContext);
        // }
        // }
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
            if (arg instanceof AbstractToscaType) {
                AbstractToscaType type = ((AbstractToscaType) arg);
                Csar csar = csarRepositorySearchService.getArchive(type.getArchiveName(), type.getArchiveVersion());
                if (csar == null) {
                    throw new NotFoundException("Unable to find dependencies from type as it's archive cannot be found in the repository.");
                }
                Set<CSARDependency> dependencies = csar.getDependencies() == null ? Sets.newHashSet() : csar.getDependencies();
                dependencies.add(new CSARDependency(type.getArchiveName(), type.getArchiveVersion()));
                return dependencies;
            }
        }
        return Sets.<CSARDependency> newHashSet();
    }

    public <T> T execInToscaContext(Supplier<T> supplier, boolean requireNew, Object... dependenciesSource) {
        boolean initContext = false;
        ToscaContext.Context existingContext = ToscaContext.get();
        if (requireNew || existingContext == null) {
            initContext = true;
        }
        try {
            // try to find dependencies from parameters
            if (initContext) {
                Set<CSARDependency> dependencies = findDependencies(dependenciesSource);
                log.debug("Initializing Tosca Context with dependencies {}", dependencies);
                ToscaContext.init(dependencies);
            }
            return supplier.get();
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
}
