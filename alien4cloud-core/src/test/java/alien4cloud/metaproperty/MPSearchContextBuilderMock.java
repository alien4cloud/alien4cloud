package alien4cloud.metaproperty;

import alien4cloud.dao.IESMetaPropertiesSearchContext;
import alien4cloud.dao.IESMetaPropertiesSearchContextBuilder;
import alien4cloud.dao.model.FacetedSearchResult;
import com.google.common.collect.Lists;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.mapping.IFacetBuilderHelper;

import java.util.List;
import java.util.Map;

public class MPSearchContextBuilderMock implements IESMetaPropertiesSearchContextBuilder {

    private class Context implements IESMetaPropertiesSearchContext {
        @Override
        public List<IFacetBuilderHelper> getFacetBuilderHelpers() {
            return Lists.newArrayList();
        }

        @Override
        public FilterBuilder[] getFilterBuilders(Map<String, String[]> filters) {
            return new FilterBuilder[0];
        }

        @Override
        public void preProcess(Map<String, String[]> filters) {

        }

        @Override
        public void postProcess(FacetedSearchResult result) {

        }
    }

    @Override
    public <T> IESMetaPropertiesSearchContext getContext(Class<T> clazz) {
        return new Context();
    }

}
