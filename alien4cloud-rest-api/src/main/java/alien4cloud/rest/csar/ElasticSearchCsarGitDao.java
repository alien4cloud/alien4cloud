package alien4cloud.rest.csar;

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
import alien4cloud.security.model.CsarGitRepository;

import com.google.common.collect.Maps;

@Component("csargit-dao")
public class ElasticSearchCsarGitDao extends ESGenericSearchDAO implements IAlienCsarGitDao {
    @Resource
    private MappingBuilder mappingBuilder;

    @PostConstruct
    public void initEnvironment() {
        // init ES annotation scanning
        try {
            mappingBuilder.initialize(CsarGitRepository.class.getPackage().getName());
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }
        // init indexes and mapped classes
        initIndices(CsarGitRepository.class.getSimpleName().toLowerCase(), null, new Class<?>[] { CsarGitRepository.class });
        initCompleted();
    }

    @Override
    public void save(CsarGitRepository csargit) {
        super.save(csargit);
    }

    @Override
    public CsarGitRepository find(String id) {
        return super.findById(CsarGitRepository.class, id);
    }

    @Override
    public void delete(String id) {
        super.delete(CsarGitRepository.class, id);
    }

    @Override
    public FacetedSearchResult search(String searchQuery, String group, int from, int size) {
        Map<String, String[]> groupFilter = Maps.newHashMap();
        if (group != null && !group.isEmpty()) {
            groupFilter.put("", new String[] { group });
        }
        return super.facetedSearch(CsarGitRepository.class, searchQuery, groupFilter, null, from, size);
    }

    @Override
    public GetMultipleDataResult find(Map<String, String[]> filters, int maxElements) {
        List<String> lowerCaseFilters;
        for (Map.Entry<String, String[]> filter : filters.entrySet()) {
            lowerCaseFilters = new ArrayList<String>();
            for (String f : filter.getValue()) {
                lowerCaseFilters.add(f.toLowerCase());
            }
            filter.setValue(lowerCaseFilters.toArray(new String[filter.getValue().length]));
        }
        return super.find(CsarGitRepository.class, filters, maxElements);
    }

    @Override
    public List<CsarGitRepository> find(String... ids) {
        return super.findByIds(CsarGitRepository.class, ids);
    }
    
    
}
