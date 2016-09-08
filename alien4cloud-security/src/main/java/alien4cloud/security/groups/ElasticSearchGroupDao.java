package alien4cloud.security.groups;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.MappingBuilder;
import org.springframework.stereotype.Component;

import alien4cloud.dao.ESGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.security.model.Group;

@Component("group-dao")
public class ElasticSearchGroupDao extends ESGenericSearchDAO implements IAlienGroupDao {
    @Resource
    private MappingBuilder mappingBuilder;

    @PostConstruct
    public void initEnvironment() {
        // init ES annotation scanning
        try {
            mappingBuilder.initialize(Group.class.getPackage().getName());
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }
        // init indexes and mapped classes
        initIndices(Group.class.getSimpleName().toLowerCase(), null, new Class<?>[] { Group.class });
        initCompleted();
    }

    @Override
    public void save(Group group) {
        super.save(group);
    }

    @Override
    public Group find(String id) {
        return super.findById(Group.class, id);
    }

    @Override
    public void delete(String groupId) {
        delete(Group.class, groupId);
    }

    @Override
    public boolean isGroupWithNameExist(String groupName) {
        return super.count(Group.class, QueryBuilders.termQuery("name", groupName)) > 0;
    }

    @Override
    public GetMultipleDataResult search(String searchQuery, Map<String, String[]> filters, int from, int size) {
        return super.search(Group.class, searchQuery, filters, from, size);
    }

    @Override
    public GetMultipleDataResult find(Map<String, String[]> filters, int maxElements) {
        return super.find(Group.class, filters, maxElements);
    }

    @Override
    public List<Group> find(String... ids) {
        return super.findByIds(Group.class, ids);
    }

    @Override
    public Group findByName(String groupName) {
        return super.customFind(Group.class, QueryBuilders.termQuery("name", groupName));
    }

}
