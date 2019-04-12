package org.alien4cloud.tosca.catalog.index;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.Csar;
import org.springframework.stereotype.Component;

import alien4cloud.dao.FilterUtil;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.utils.AlienConstants;

@Component
public class CsarSearchService implements ICsarSearchService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO csarDAO;

    @Override
    public FacetedSearchResult search(String query, int from, int size, Map<String, String[]> filters) {
System.out.println("### csar search " + query + " " + filters);
        return csarDAO.facetedSearch(Csar.class, query, FilterUtil.singleKeyFilter(filters, "workspace", AlienConstants.GLOBAL_WORKSPACE_ID), null, from, size);
    }
}
