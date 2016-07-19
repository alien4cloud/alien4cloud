package alien4cloud.security.users;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.stereotype.Component;

import alien4cloud.dao.ESGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.security.model.User;

import com.google.common.collect.Maps;

@Component("user-dao")
public class ElasticSearchUserDao extends ESGenericSearchDAO implements IAlienUserDao {
    @Resource
    private MappingBuilder mappingBuilder;

    @PostConstruct
    public void initEnvironment() {
        // init ES annotation scanning
        try {
            mappingBuilder.initialize(User.class.getPackage().getName());
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }
        // init indexes and mapped classes
        initIndices(User.class.getSimpleName().toLowerCase(), null, new Class<?>[] { User.class });
        initCompleted();
    }

    @Override
    public void save(User user) {
        super.save(user);
    }

    @Override
    public User find(String username) {
        return super.findById(User.class, username);
    }

    @Override
    public void delete(String userName) {
        super.delete(User.class, userName);
    }

    @Override
    public FacetedSearchResult search(String searchQuery, String group, int from, int size) {
        Map<String, String[]> groupFilter = Maps.newHashMap();
        if (group != null && !group.isEmpty()) {
            groupFilter.put("", new String[] { group });
        }
        return super.facetedSearch(User.class, searchQuery, groupFilter, null, from, size);
    }

    @Override
    public GetMultipleDataResult find(Map<String, String[]> filters, int maxElements) {

        // you've to put filters in lowercase for an analyzed field
        // should be moved to es-mapping project (?)
        List<String> lowerCaseFilters;
        for (Map.Entry<String, String[]> filter : filters.entrySet()) {
            lowerCaseFilters = new ArrayList<String>();
            for (String f : filter.getValue()) {
                lowerCaseFilters.add(f.toLowerCase());
            }
            filter.setValue(lowerCaseFilters.toArray(new String[filter.getValue().length]));
        }

        return super.find(User.class, filters, maxElements);
    }

    @Override
    public List<User> find(String... usernames) {
        return super.findByIds(User.class, usernames);
    }
}