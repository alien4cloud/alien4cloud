package alien4cloud.tool.compilation.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.InitializingBean;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.rest.component.ElementFromArchiveRequest;
import alien4cloud.rest.component.QueryComponentType;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.rest.utils.RestClient;
import alien4cloud.tool.compilation.exception.CompilationToolRuntimeException;

@Slf4j
public class CheckElementExistService implements ICSARRepositorySearchService, InitializingBean {

    private static final String CHECK_EXIST_PATH = "/rest/components/exist";

    @Setter
    private String alienUrl;

    @Setter
    private String user;

    @Setter
    private String password;

    private RestClient restClient;

    @Override
    public boolean isElementExistInDependencies(Class<? extends IndexedToscaElement> elementClass, String elementName, Collection<CSARDependency> dependencies) {
        ElementFromArchiveRequest request = new ElementFromArchiveRequest(elementName, QueryComponentType.valueOf(elementClass), dependencies);
        try {
            RestResponse<Boolean> response = JsonUtil.read(this.restClient.postJSon(CHECK_EXIST_PATH, JsonUtil.toString(request)), Boolean.class);
            if (response.getError() != null) {
                log.error("Error happened while perform online dependencies checking, code [" + response.getError().getCode() + "], message ["
                        + response.getError().getMessage() + "]");
                throw new CompilationToolRuntimeException("Error happened while perform online dependencies checking, code [" + response.getError().getCode()
                        + "], message [" + response.getError().getMessage() + "]");
            } else {
                return response.getData().booleanValue();
            }
        } catch (IOException e) {
            log.error("Error happened while perform online dependencies checking", e);
            throw new CompilationToolRuntimeException("Error happened while perform online dependencies checking", e);
        }
    }

    public void destroy() {
        try {
            this.restClient.close();
        } catch (IOException e) {
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.restClient = new RestClient(alienUrl);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("j_username", this.user));
        nvps.add(new BasicNameValuePair("j_password", this.password));
        nvps.add(new BasicNameValuePair("submit", "Login"));
        this.restClient.postUrlEncoded("/j_spring_security_check", nvps);
    }

    @Override
    public <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies) {
        throw new NotImplementedException("This method is not used in the compilation tool.");
    }

    @Override
    public <T extends IndexedToscaElement> T getRequiredElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies)
            throws NotFoundException {
        throw new NotImplementedException("This method is not used in the compilation tool.");
    }

    @Override
    public <T extends IndexedToscaElement> T getParentOfElement(Class<T> elementClass, T indexedToscaElement, String parentElementId) {
        throw new NotImplementedException("This method is not used in the compilation tool.");
    }

    @Override
    public FacetedSearchResult search(Class<? extends IndexedToscaElement> classNameToQuery, String query, Integer from, Integer size,
            Map<String, String[]> filters, boolean queryAllVersions) {
        throw new NotImplementedException("This method is not used in the compilation tool.");
    }

}
