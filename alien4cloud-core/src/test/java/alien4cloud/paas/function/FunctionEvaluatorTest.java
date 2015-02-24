package alien4cloud.paas.function;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IOperationParameter;
import alien4cloud.model.components.Operation;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
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
    public void testParseString() {
        Map<String, NodeTemplate> nodeTemplates = Maps.newHashMap();
        NodeTemplate nodeTemplate1 = new NodeTemplate();
        nodeTemplate1.setProperties(MapUtil.newHashMap(new String[] { "the_property_name_1" }, new String[] { "the_property_value_1" }));
        nodeTemplates.put("the_node_tempalte_1", nodeTemplate1);
        NodeTemplate nodeTemplate2 = new NodeTemplate();
        nodeTemplate2.setProperties(MapUtil.newHashMap(new String[] { "the_property_name_2" }, new String[] { "the_property_value_2" }));
        nodeTemplates.put("the_node_tempalte_2", nodeTemplate2);
        Topology topology = new Topology();
        topology.setNodeTemplates(nodeTemplates);

        Map<String, Map<String, InstanceInformation>> runtimeInformations = Maps.newHashMap();

        String parsedString = FunctionEvaluator.parseString(
                "http://get_property: [the_node_tempalte_1, the_property_name_1]:get_property: [the_node_tempalte_2, the_property_name_2 ]/super", topology,
                runtimeInformations, "0");
        Assert.assertEquals("http://the_property_value_1:the_property_value_2/super", parsedString);
    }

    @Test
    public void testParseConcatString() {
        Map<String, NodeTemplate> nodeTemplates = Maps.newHashMap();
        NodeTemplate nodeTemplate1 = new NodeTemplate();
        nodeTemplate1.setProperties(MapUtil.newHashMap(new String[] { "the_property_name_1" }, new String[] { "the_property_value_1" }));
        nodeTemplates.put("the_node_tempalte_1", nodeTemplate1);
        NodeTemplate nodeTemplate2 = new NodeTemplate();
        nodeTemplate2.setProperties(MapUtil.newHashMap(new String[] { "the_property_name_2" }, new String[] { "the_property_value_2" }));
        nodeTemplates.put("the_node_tempalte_2", nodeTemplate2);
        Topology topology = new Topology();
        topology.setNodeTemplates(nodeTemplates);

        Map<String, Map<String, InstanceInformation>> runtimeInformations = Maps.newHashMap();

        String parsedConcatString = FunctionEvaluator.parseString("http://concat: [the_node_tempalte_value_1, the_node_tempalte_value_2]:/super", topology,
                runtimeInformations, "0");
        Assert.assertEquals("http://the_node_tempalte_1the_property_name_1:the_property_value_2/super", parsedConcatString);
    }

    @Test
    public void getPropertySELFAndHOSTKeywordsSucessTest() throws Throwable {

        String computeName = "comp_tomcat_war";
        PaaSNodeTemplate computePaaS = builtPaaSNodeTemplates.get(computeName);
        Operation configOp = computePaaS.getIndexedToscaElement().getInterfaces().get(ToscaNodeLifecycleConstants.STANDARD).getOperations()
                .get(ToscaNodeLifecycleConstants.CONFIGURE);
        IOperationParameter param = configOp.getInputParameters().get("customHostName");

        Assert.assertEquals(computePaaS.getNodeTemplate().getProperties().get("customHostName"),
                FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, computePaaS, builtPaaSNodeTemplates));

        // HOST keyword
        String tomcatName = "tomcat";
        PaaSNodeTemplate tomcatPaaS = builtPaaSNodeTemplates.get(tomcatName);
        Operation customHelloOp = tomcatPaaS.getIndexedToscaElement().getInterfaces().get("custom").getOperations().get("helloCmd");
        param = customHelloOp.getInputParameters().get("customHostName");
        Assert.assertEquals(computePaaS.getNodeTemplate().getProperties().get("customHostName"),
                FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, tomcatPaaS, builtPaaSNodeTemplates));
    }

    @Test
    public void getPropertySOURCEAndTARGETKeywordsSucessTest() throws Throwable {

        String warName = "war_1";
        String tomcatName = "tomcat";
        PaaSNodeTemplate warPaaS = builtPaaSNodeTemplates.get(warName);
        PaaSNodeTemplate tomcatPaaS = builtPaaSNodeTemplates.get(tomcatName);
        PaaSRelationshipTemplate hostedOnRelTemp = warPaaS.getRelationshipTemplate("hostedOnTomcat");

        Operation configOp = hostedOnRelTemp.getIndexedToscaElement().getInterfaces().get(ToscaRelationshipLifecycleConstants.CONFIGURE).getOperations()
                .get(ToscaRelationshipLifecycleConstants.POST_CONFIGURE_SOURCE);

        // test SOURCE keyword
        IOperationParameter param = configOp.getInputParameters().get("contextPath");
        Assert.assertEquals(warPaaS.getNodeTemplate().getProperties().get("contextPath"),
                FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, hostedOnRelTemp, builtPaaSNodeTemplates));

        // test TARGET keyword
        param = configOp.getInputParameters().get("tomcatVersion");
        Assert.assertEquals(tomcatPaaS.getNodeTemplate().getProperties().get("version"),
                FunctionEvaluator.evaluateGetPropertyFuntion((FunctionPropertyValue) param, hostedOnRelTemp, builtPaaSNodeTemplates));

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