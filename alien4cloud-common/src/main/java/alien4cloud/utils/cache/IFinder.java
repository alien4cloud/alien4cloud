package alien4cloud.utils.cache;

public interface IFinder<T> {
    <K extends T> K find(Class<K> clazz, String id);
}
