package alien4cloud.dao;

public interface IESMetaPropertiesSearchContextBuilder {

    <T> IESMetaPropertiesSearchContext getContext(Class<T> clazz);
}
