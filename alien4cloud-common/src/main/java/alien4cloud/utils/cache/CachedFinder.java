package alien4cloud.utils.cache;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.collect.Maps;

@Slf4j
public class CachedFinder<T> implements IFinder<T> {

    private IFinder<T> wrapped;

    public CachedFinder(IFinder<T> wrapped) {
        super();
        this.wrapped = wrapped;
    }

    private Map<Class<? extends T>, Map<String, T>> cache = Maps.newHashMap();

    @SuppressWarnings("unchecked")
    @Override
    public <K extends T> K find(Class<K> clazz, String id) {
        Map<String, T> typeCache = cache.get(clazz);
        if (typeCache == null) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("cache not found for type <%s>, init one ...", clazz.getSimpleName()));
            }
            typeCache = Maps.newHashMap();
            cache.put(clazz, typeCache);
        } else {
            if (log.isTraceEnabled()) {
                log.trace(String.format("cache found for type <%s>, using it !", clazz.getSimpleName()));
            }
        }
        T element = typeCache.get(id);
        if (element == null) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Element not found from cache for type <%s> id <%s>, look for in source ...", clazz.getSimpleName(), id));
            }
            element = wrapped.find(clazz, id);
            typeCache.put(id, element);
        } else {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Element found from cache for type <%s> id <%s>, hit !", clazz.getSimpleName(), id));
            }
        }
        return (K) element;
    }

}
