package alien4cloud.rest.form;

import java.beans.IntrospectionException;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.model.common.MetaPropConfiguration;
import org.alien4cloud.tosca.model.types.NodeType;
import alien4cloud.orchestrators.services.OrchestratorConfigurationService;
import alien4cloud.plugin.PluginManager;
import alien4cloud.repository.services.RepositoryService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.ui.form.PojoFormDescriptorGenerator;
import alien4cloud.ui.form.ToscaPropertyFormDescriptorGenerator;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Component
@RestController
@RequestMapping({ "/rest/formdescriptor", "/rest/v1/formdescriptor", "/rest/latest/formdescriptor" })
public class FormDescriptorController {
    @Resource
    private PojoFormDescriptorGenerator pojoFormDescriptorGenerator;
    @Resource
    private ToscaPropertyFormDescriptorGenerator toscaPropertyFormDescriptorGenerator;
    @Resource
    private PluginManager pluginManager;
    @Resource
    private OrchestratorConfigurationService orchestratorConfigurationService;
    @Resource
    private RepositoryService repositoryService;

    @ApiIgnore
    @RequestMapping(value = "/nodetype", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<Map<String, Object>> getNodeTypeFormDescriptor() throws IntrospectionException {
        return RestResponseBuilder.<Map<String, Object>> builder().data(pojoFormDescriptorGenerator.generateDescriptor(NodeType.class)).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/complex-tosca-type", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER', 'COMPONENTS_BROWSER')")
    public RestResponse<Map<String, Object>> getComplexToscaTypeFormDescriptor(@RequestBody @Valid ComplexToscaTypeFormDescriptorRequest request) {
        return RestResponseBuilder.<Map<String, Object>> builder()
                .data(toscaPropertyFormDescriptorGenerator.generateDescriptor(request.getPropertyDefinition(), request.getDependencies())).build();
    }

    @RequestMapping(value = "/tagconfiguration", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<Map<String, Object>> getTagConfigurationFormDescriptor() throws IntrospectionException {
        return RestResponseBuilder.<Map<String, Object>> builder().data(pojoFormDescriptorGenerator.generateDescriptor(MetaPropConfiguration.class)).build();
    }

    @RequestMapping(value = "/pluginConfig/{pluginId:.+}", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<Map<String, Object>> getPluginConfigurationFormDescriptor(@PathVariable String pluginId) throws IntrospectionException {
        if (pluginManager.isPluginConfigurable(pluginId)) {
            Class<?> configType = pluginManager.getConfigurationType(pluginId);
            return RestResponseBuilder.<Map<String, Object>> builder().data(pojoFormDescriptorGenerator.generateDescriptor(configType)).build();
        }
        return RestResponseBuilder.<Map<String, Object>> builder().build();
    }

    @RequestMapping(value = "/orchestratorConfig/{orchestratorId:.+}", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<Map<String, Object>> getOrchestratorConfigurationFormDescriptor(@PathVariable String orchestratorId) throws IntrospectionException {
        Class<?> configurationClass = orchestratorConfigurationService.getConfigurationType(orchestratorId);
        if (configurationClass != null) {
            return RestResponseBuilder.<Map<String, Object>> builder().data(pojoFormDescriptorGenerator.generateDescriptor(configurationClass)).build();
        }
        return RestResponseBuilder.<Map<String, Object>> builder().build();
    }

    @RequestMapping(value = "/repositoryConfig/{pluginId:.+}", method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'COMPONENTS_MANAGER')")
    public RestResponse<Map<String, Object>> getRepositoryConfigurationFormDescriptor(@PathVariable String pluginId) throws IntrospectionException {
        Class<?> configurationClass = repositoryService.getRepositoryConfigurationType(pluginId);
        if (configurationClass != null) {
            return RestResponseBuilder.<Map<String, Object>> builder().data(pojoFormDescriptorGenerator.generateDescriptor(configurationClass)).build();
        }
        return RestResponseBuilder.<Map<String, Object>> builder().build();
    }
}
