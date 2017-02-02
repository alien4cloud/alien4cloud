package alien4cloud.it.service;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.security.AuthenticationStepDefinitions;
import alien4cloud.it.topology.EditorStepDefinitions;
import alien4cloud.it.topology.TopologyStepDefinitions;
import alien4cloud.it.topology.TopologyTemplateStepDefinitions;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.model.common.Tag;
import alien4cloud.rest.application.model.ApplicationEnvironmentDTO;
import alien4cloud.rest.application.model.ApplicationEnvironmentRequest;
import alien4cloud.rest.application.model.CreateApplicationRequest;
import alien4cloud.rest.application.model.UpdateApplicationEnvironmentRequest;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.component.UpdateTagRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.service.model.CreateServiceResourceRequest;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.utils.ReflectionUtil;
import alien4cloud.utils.VersionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Maps;
import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static alien4cloud.it.Context.getInstance;
import static alien4cloud.it.Context.getRestClientInstance;
import static alien4cloud.it.utils.TestUtils.nullable;
import static org.junit.Assert.*;

@Slf4j
public class ServiceStepDefinitions {

    @When("^I create a service with name \"(.*?)\", version \"(.*?)\", type \"(.*?)\", archive name \"(.*?)\", archive version \"(.*?)\", deploymentId \"(.*?)\"$")
    public void i_create_a_service_with_name_version_type_archive_name_archive_version(String serviceName, String serviceVersion, String type, String archiveName, String archiveVersion, String deploymentId) throws Throwable {
        CreateServiceResourceRequest request = new CreateServiceResourceRequest(nullable(serviceName), nullable(serviceVersion), nullable(type), nullable(archiveName), nullable(archiveVersion), nullable(deploymentId));
        Context.getInstance().registerRestResponse(getRestClientInstance().postJSon("/rest/v1/services/", JsonUtil.toString(request)));
    }

}
