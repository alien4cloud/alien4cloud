package alien4cloud.utils;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

@Slf4j
public final class TypeScanner {

    private TypeScanner() {
    }

    public static Set<Class<?>> scanTypes(String basePackage, Class<?> targetType) throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        AssignableTypeFilter filter = new AssignableTypeFilter(targetType);
        scanner.addIncludeFilter(filter);
        Set<BeanDefinition> beanSet = scanner.findCandidateComponents(basePackage);

        Set<Class<?>> classSet = new HashSet<Class<?>>();
        for (BeanDefinition beanDef : beanSet) {
            log.debug("found candidate bean = {}", beanDef.getBeanClassName());

            Class<?> clazz;

            clazz = Class.forName(beanDef.getBeanClassName(), true, Thread.currentThread().getContextClassLoader());
            classSet.add(clazz);
        }

        return classSet;
    }
}
