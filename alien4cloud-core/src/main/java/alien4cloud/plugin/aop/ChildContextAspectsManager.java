package alien4cloud.plugin.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.GenericApplicationListenerAdapter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;

import alien4cloud.events.AlienEvent;

import com.google.common.collect.Maps;

/**
 * Manage inter context proxies : thanks to it, we can define aspects upon main context beans in child contexts. Also broadcast {@link AlienEvent}s to child
 * contexts.
 * <p>
 * This {@link BeanPostProcessor} will search for {@link Overridable} annotation in any bean of the context (at type level, or method level). Each bean
 * containing this annotation is a candidate to be proxied and is in fact proxied by an internal {@link InvocationHandler}.
 * <p>
 * When a child context is started, it looks if some advice can apply to any of these candidates. In such case, the invocation handler will invoke methods on
 * proxy created using these advices.
 * <p>
 * Few notes:
 * <ul>
 * <li>candidate beans must have interface (we use JDK proxy feature).
 * <li>the annotation can be used on the methods, type, interface or interface method.
 * <li>you can use several advices for the same bean in child context.
 * <li>the bean can be already proxied in the main context: in this case, the annotation should be present at interface level.
 * <li>proxies are applied in the order child contexts are started.
 * </ul>
 */
@Component
@Slf4j
public class ChildContextAspectsManager implements ApplicationListener<ApplicationEvent>, BeanPostProcessor {

    /** All the candidates to be overriden by plugin child contexts. */
    private Map<Object, ProxyRegistry> overridableCandidates = Maps.newHashMap();

    /** All the referenced plugin child contexts. */
    private Map<String, ApplicationContext> childContexts = Maps.newLinkedHashMap();

    /** We store all the names of beans that implements {@link ApplicationListener} per child context. */
    private Map<String, GenericApplicationListenerAdapter[]> childApplicationListeners = Maps.newHashMap();

    private Lock lock = new ReentrantLock();

    @Resource
    private ApplicationContext context;

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String id) throws BeansException {
        if (log.isTraceEnabled()) {
            log.trace("post processing bean with id <{}> of type <{}>", id, bean.getClass().toString());
        }
        if (AnnotationUtils.findAnnotation(bean.getClass(), Overridable.class) != null) {
            // the bean is annotated as Overridable candidate
            log.info("The bean with id <{}> of type <{}> is candidate to be overridden by plugin child contexts", id, bean.getClass().toString());
            registerProxyCandidate(bean, id);
        } else {
            // let's look for annotation in methods
            ReflectionUtils.doWithMethods(bean.getClass(), new MethodCallback() {
                @Override
                public void doWith(Method m) throws IllegalArgumentException, IllegalAccessException {
                    log.info("The method <{}> of bean <{}> is candidate to be overridden by plugin child contexts", m.toString(), id);
                    registerProxyCandidate(bean, id);
                }
            }, new MethodFilter() {
                @Override
                public boolean matches(Method m) {
                    // we search for public methods annotated as Overridable
                    return (m.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC && AnnotationUtils.findAnnotation(m, Overridable.class) != null;
                }
            });
        }
        // if the bean is a candidate, then return the proxy
        ProxyRegistry proxyRegistry = overridableCandidates.get(bean);
        if (proxyRegistry != null) {
            return proxyRegistry.proxy;
        } else {
            return bean;
        }

    }

    private void registerProxyCandidate(final Object bean, final String id) {
        ProxyRegistry proxyRegistry = overridableCandidates.get(bean);
        if (proxyRegistry == null) {
            proxyRegistry = new ProxyRegistry();
            Object proxy = Proxy.newProxyInstance(bean.getClass().getClassLoader(), bean.getClass().getInterfaces(), new DynamicProxyInvocationHandler(bean));
            proxyRegistry.proxy = proxy;
            proxyRegistry.target = bean;
            proxyRegistry.original = bean;
            proxyRegistry.beanName = id;
            overridableCandidates.put(bean, proxyRegistry);
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String id) throws BeansException {
        return bean;
    }

    private void onContextStarted(ApplicationContext ctx) {
        if (ctx == context) {
            return;
        }
        lock.lock();
        try {
            childContexts.put(ctx.toString(), ctx);
            if (log.isDebugEnabled()) {
                log.debug("context started with id: {}", ctx.getId());
            }
            decorateProxyCandidate(ctx);
            detectApplicationListeners(ctx);
        } finally {
            lock.unlock();
        }
    }

    private void detectApplicationListeners(ApplicationContext ctx) {
        String[] applicationListenerBeanNames = ctx.getBeanNamesForType(ApplicationListener.class);
        if (applicationListenerBeanNames != null && applicationListenerBeanNames.length > 0) {
            if (log.isDebugEnabled()) {
                log.debug("The child context <{}> contains the following listeners: {}", ctx.getDisplayName(), applicationListenerBeanNames);
            }
            GenericApplicationListenerAdapter[] adapters = new GenericApplicationListenerAdapter[applicationListenerBeanNames.length];
            int i = 0;
            for (String applicationListenerBeanName : applicationListenerBeanNames) {
                adapters[i++] = new GenericApplicationListenerAdapter((ApplicationListener<?>) ctx.getBean(applicationListenerBeanName));
            }
            childApplicationListeners.put(ctx.toString(), adapters);
        }
    }

    private void onContextStopped(ApplicationContext ctx) {
        if (ctx.getId().endsWith(":leader")) {
            return;
        }
        lock.lock();
        try {
            if (log.isDebugEnabled()) {
                log.debug("context stopped with id: {}", ctx.getId());
            }
            ApplicationContext removed = childContexts.remove(ctx.toString());
            childApplicationListeners.remove(ctx.toString());
            if (removed == null) {
                log.warn("The stopped context {} can not be found in registered contexts", ctx);
            } else {
                // reset all proxy candidates (he target become the origin bean)
                for (ProxyRegistry candidateProxyRegistryEntry : overridableCandidates.values()) {
                    candidateProxyRegistryEntry.reset();
                }
                // rebuild proxies with the remaining child contexts
                for (ApplicationContext childContext : childContexts.values()) {
                    decorateProxyCandidate(childContext);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void onApplicationContextEvent(ApplicationContextEvent e) {
        if (e instanceof ContextStartedEvent) {
            onContextStarted(e.getApplicationContext());
        } else if (e instanceof ContextStoppedEvent) {
            onContextStopped(e.getApplicationContext());
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent e) {
        if (e instanceof ApplicationContextEvent) {
            onApplicationContextEvent((ApplicationContextEvent) e);
        } else if (e instanceof AlienEvent) {
            onAlienEvent((AlienEvent) e);
        }
    }

    /**
     * Broadcast {@link AlienEvent}s to child context {@link ApplicationListener} beans.
     */
    private void onAlienEvent(AlienEvent e) {
        // Alien events are published to child contexts
        // we can't publish directly into child context because it will re-publish to it's parent causing a stack overflow !
        for (Entry<String, GenericApplicationListenerAdapter[]> childListenersEntry : childApplicationListeners.entrySet()) {
            ApplicationContext ctx = childContexts.get(childListenersEntry.getKey());
            if (ctx != null) {
                for (GenericApplicationListenerAdapter childListener : childListenersEntry.getValue()) {
                    if (childListener.supportsEventType(e.getClass())) {
                        childListener.onApplicationEvent(e);
                    }
                }
            }
        }
    }

    private void decorateProxyCandidate(ApplicationContext ctx) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        // we need to use the child context classLoader
        Thread.currentThread().setContextClassLoader(ctx.getClassLoader());
        try {
            AnnotationAwareAspectJAutoProxyCreator annotationAwareAspectJAutoProxyCreator = new AnnotationAwareAspectJAutoProxyCreator();
            DefaultListableBeanFactory lbf = new DefaultListableBeanFactory(ctx);
            annotationAwareAspectJAutoProxyCreator.setBeanFactory(lbf);
            for (ProxyRegistry candidateProxyRegistry : overridableCandidates.values()) {
                Object bean = candidateProxyRegistry.target;
                Object advicedBean = annotationAwareAspectJAutoProxyCreator.postProcessAfterInitialization(bean, candidateProxyRegistry.beanName);
                if (bean != advicedBean) {
                    log.info("The bean with name {} is now proxied by {}", candidateProxyRegistry.beanName, advicedBean);
                    candidateProxyRegistry.target = advicedBean;
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * This {@link InvocationHandler} will invoke methods:
     * <ul>
     * <li>on a proxy if aspects have been defined for this bean in plugin child context.
     * <li>on the original bean if no plugin child context have defined any aspect for it.
     * </ul>
     */
    private class DynamicProxyInvocationHandler implements InvocationHandler {

        /**
         * The original bean that is eventually overridden.
         */
        private Object obj;

        public DynamicProxyInvocationHandler(Object obj) {
            super();
            this.obj = obj;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            lock.lock();
            Object target = obj;
            try {
                ProxyRegistry proxyRegistry = overridableCandidates.get(obj);
                if (proxyRegistry != null) {
                    if (log.isDebugEnabled()) {
                        if (proxyRegistry.target != proxyRegistry.original) {
                            log.debug("Invoking method <{}> on proxy", method);
                        } else {
                            log.debug("Invoking method <{}> on native bean (no proxy found)", method);
                        }
                    }
                    target = proxyRegistry.target;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Invoking method <{}> on native bean (no proxy registry found)", method);
                    }
                }
            } finally {
                lock.unlock();
            }
            Object result = null;
            try {
                result = ReflectionUtils.invokeMethod(method, target, args);
            } catch (Exception e) {
                try {
                    ReflectionUtils.handleReflectionException(e);
                } catch (UndeclaredThrowableException ute) {
                    throw ute.getUndeclaredThrowable();
                }
            }
            return result;
        }
    }

    private static class ProxyRegistry {
        /** The bean name in the main application context. */
        private String beanName;

        /** The dynamic proxy for the bean. */
        private Object proxy;

        /** The target : the original bean eventually proxied by child context aspects. */
        private Object target;

        /** The original bean that is candidate for being proxied by child context aspects. */
        private Object original;

        /** The target become the origin, like just after main context startup. */
        public void reset() {
            this.target = this.original;
        }
    }

}
