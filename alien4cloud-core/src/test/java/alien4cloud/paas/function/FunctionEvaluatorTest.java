package alien4cloud.paas.function;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.git.RepositoryManager;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ConcatPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IOperationParameter;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.components.Operation;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.plan.TopologyTreeBuilderService;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;
import alien4cloud.security.Role;
import alien4cloud.test.utils.SecurityTestUtils;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.YamlParserUtil;

import com.google.common.collect.Maps;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
public class FunctionEvaluatorTest {

    @Resource
    private ArchiveUploadService archiveUploadService;

    @Resource
    private TopologyTreeBuilderService treeBuilder;

    @Value("${directories.alien}/${directories.csar_repository}")
    private String alienRepoDir;

    private Path artifactsDirectory = Paths.get("target/git-artifacts");
    private RepositoryManager repositoryManager = new RepositoryManager();
    private Map<String, PaaSNodeTemplate> builtPaaSNodeTemplates;

    @PostConstruct
    public void postConstruct() throws Throwable {

        if (Files.exists(Paths.get(alienRepoDir))) {
            try {
                FileUtil.delete(Paths.get(alienRepoDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        SecurityTestUtils.setTestAuthentication(Role.ADMIN);

        String normativeLocalName = "tosca-normative-types";
        repositoryManager.cloneOrCheckout(artifactsDirectory, "https://github.com/alien4cloud/tosca-normative-types.git", "master", normativeLocalName);
        String sampleLocalName = "samples";
        repositoryManager.cloneOrCheckout(artifactsDirectory, "https://github.com/alien4cloud/samples.git", "master", sampleLocalName);
        String extendedLocalName = "alien-extended-types";
        repositoryManager.cloneOrCheckout(artifactsDirectory, "https://github.com/alien4cloud/alien4cloud-extended-types.git", "master", extendedLocalName);

        Path typesPath = artifactsDirectory.resolve(normativeLocalName);
        Path typesZipPath = artifactsDirectory.resolve(normativeLocalName + ".zip");
        FileUtil.zip(typesPath, typesZipPath);
        archiveUploadService.upload(typesZipPath);

        typesPath = artifactsDirectory.resolve(extendedLocalName).resolve("alien-base-types-1.0-SNAPSHOT");
        typesZipPath = artifactsDirectory.resolve("alien-base-types-1.0-SNAPSHOT.zip");
        FileUtil.zip(typesPath, typesZipPath);
        archiveUploadService.upload(typesZipPath);

        typesPath = artifactsDirectory.resolve(sampleLocalName).resolve("tomcat-war");
        typesZipPath = artifactsDirectory.resolve("tomcat_war.zip");
        FileUtil.zip(typesPath, typesZipPath);
        archiveUploadService.upload(typesZipPath);

        typesPath = Paths.get("src/test/resources/alien/paas/function/csars/test-types");
        typesZipPath = artifactsDirectory.resolve("target/test-types.zip");
        FileUtil.zip(typesPath, typesZipPath);
        archiveUploadService.upload(typesZipPath);

        Topology topology = YamlParserUtil.parseFromUTF8File(Paths.get("src/test/resources/alien/paas/function/topology/badFunctionsTomcatWar.yml"),
                Topology.class);
        topology.setId(UUID.randomUUID().toString());
        builtPaaSNodeTemplates = treeBuilder.buildPaaSTopology(topology).getAllNodes();
    }

    @Test
    public void testParseAttributConcatScalar() {

        Map<String, NodeTemplate> nodeTemplates = Maps.newHashMap();
        NodeTemplate nodeTemplate1 = new NodeTemplate();
        nodeTemplate1.setProperties(MapUtil.newHashMap(new String[] { "the_property_name_1" }, new AbstractPropertyValue[] { new ScalarPropertyValue(
                "the_property_value_1") }));
        nodeTemplates.put("the_node_tempalte_1", nodeTemplate1);
        NodeTemplate nodeTemplate2 = new NodeTemplate();
        nodeTemplate2.setProperties(MapUtil.newHashMap(new String[] { "the_property_name_2" }, new AbstractPropertyValue[] { new ScalarPropertyValue(
                "the_property_value_2") }));
        nodeTemplates.put("the_node_tempalte_2", nodeTemplate2);
        Topology topology = new Topology();
        topology.setNodeTemplates(nodeTemplates);

        Map<String, Map<String, InstanceInformation>> runtimeInformations = Maps.newHashMap();

        // Create a IAttributeValue
        ConcatPropertyValue concatAttributeValue = new ConcatPropertyValue();
        ScalarPropertyValue scalarParameter1 = new ScalarPropertyValue();
        ScalarPropertyValue scalarParameter2 = new ScalarPropertyValue();
        ScalarPropertyValue scalarParameter3 = new ScalarPropertyValue();
        ScalarPropertyValue scalarParameter4 = new ScalarPropertyValue();

        scalarParameter1.setValue("http://");
        scalarParameter2.setValue("mywebsiteurl");
        scalarParameter3.setValue(":");
        scalarParameter4.setValue("port");

        concatAttributeValue.setParameters(new ArrayList<IOperationParameter>());
        concatAttributeValue.getParameters().add(scalarParameter1);
        concatAttributeValue.getParameters().add(scalarParameter2);
        concatAttributeValue.getParameters().add(scalarParameter3);
        concatAttributeValue.getParameters().add(scalarParameter4);

        String parsedConcatString = FunctionEvaluator.parseAttribute(null, concatAttributeValue, topology, runtimeInformations, "0", null, null);
        String fullUrl = scalarParameter1.getValue() + scalarParameter2.getValue() + scalarParameter3.getValue() + scalarParameter4.getValue();
        Assert.assertEquals(fullUrl, parsedConcatString);

    }

    @Test
    public void nodeTemplatesGetPropertyKeywordsSucessTest() throws Throwable {

        String computeName = "comp_tomcat_war";
        PaaSNodeTemplate computePaaS = builtPaaSNodeTemplates.get(computeName);
        Operation configOp = computePaaS.getIndexedToscaElement().getInterfaces().get(ToscaNodeLifecycleConstants.STANDARD).getOperations()
                .get(ToscaNodeLifecycleConstants.CONFIGURE);
        IOperationParameter param = configOp.getInputParameters().get("customHostName");

        Assert.assertEquals(getPropertyValue(computePaaS, "customHostName"),
                FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, computePaaS, builtPaaSNodeTemplates));

        // HOST keyword
        String tomcatName = "tomcat";
        PaaSNodeTemplate tomcatPaaS = builtPaaSNodeTemplates.get(tomcatName);
        Operation customHelloOp = tomcatPaaS.getIndexedToscaElement().getInterfaces().get("custom").getOperations().get("helloCmd");
        param = customHelloOp.getInputParameters().get("customHostName");
        Assert.assertEquals(getPropertyValue(computePaaS, "customHostName"),
                FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, tomcatPaaS, builtPaaSNodeTemplates));
    }

    @Test
    public void relationshipGetPropertyKeywordsSucessTest() throws Throwable {

        String warName = "war_1";
        String warName_2 = "war_2";
        String tomcatName = "tomcat";
        PaaSNodeTemplate warPaaS = builtPaaSNodeTemplates.get(warName);
        PaaSNodeTemplate warPaaS_2 = builtPaaSNodeTemplates.get(warName_2);
        PaaSNodeTemplate tomcatPaaS = builtPaaSNodeTemplates.get(tomcatName);
        PaaSRelationshipTemplate hostedOnRelTemp = warPaaS.getRelationshipTemplate("hostedOnTomcat", "war_1");
        PaaSRelationshipTemplate hostedOnRelTemp_2 = warPaaS_2.getRelationshipTemplate("hostedOnTomcat", "war_2");

        Operation configOp = hostedOnRelTemp.getIndexedToscaElement().getInterfaces().get(ToscaRelationshipLifecycleConstants.CONFIGURE).getOperations()
                .get(ToscaRelationshipLifecycleConstants.POST_CONFIGURE_SOURCE);

        // test SOURCE keyword
        IOperationParameter param = configOp.getInputParameters().get("contextPath");
        Assert.assertEquals(getPropertyValue(warPaaS, "contextPath"),
                FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, hostedOnRelTemp, builtPaaSNodeTemplates));

        // test TARGET keyword
        param = configOp.getInputParameters().get("tomcatVersion");
        Assert.assertEquals(getPropertyValue(tomcatPaaS, "version"),
                FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, hostedOnRelTemp, builtPaaSNodeTemplates));

        // test SELF keyword on relationship
        param = configOp.getInputParameters().get("relName");
        Assert.assertEquals(getPropertyValue(hostedOnRelTemp, "relName"),
                FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, hostedOnRelTemp, builtPaaSNodeTemplates));

        Assert.assertEquals(getPropertyValue(hostedOnRelTemp_2, "relName"),
                FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, hostedOnRelTemp_2, builtPaaSNodeTemplates));

    }

    private String getPropertyValue(IPaaSTemplate<? extends IndexedToscaElement> paaSTemplate, String propertyName) {
        return ((ScalarPropertyValue) paaSTemplate.getTemplate().getProperties().get(propertyName)).getValue();
    }

    @Test(expected = BadUsageKeywordException.class)
    public void getPropertyWrongDefOrUSageTest() throws Throwable {

        String computeName = "comp_tomcat_war";
        PaaSNodeTemplate computePaaS = builtPaaSNodeTemplates.get(computeName);
        Operation configOp = computePaaS.getIndexedToscaElement().getInterfaces().get(ToscaNodeLifecycleConstants.STANDARD).getOperations()
                .get(ToscaNodeLifecycleConstants.CONFIGURE);

        // case keyword SOURCE used on a NodeType
        IOperationParameter param = configOp.getInputParameters().get("keywordSourceBadUsage");
        try {
            FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, computePaaS, builtPaaSNodeTemplates);
        } catch (BadUsageKeywordException e) {
            // case keyword TARGET used on a NodeType
            param = configOp.getInputParameters().get("KeywordTargetBadUsage");
            FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, computePaaS, builtPaaSNodeTemplates);
        }

    }

}