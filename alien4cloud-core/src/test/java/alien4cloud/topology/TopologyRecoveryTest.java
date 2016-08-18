package alien4cloud.topology;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.git.RepositoryManager;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.CSARSource;
import alien4cloud.model.components.Csar;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.security.model.Role;
import alien4cloud.test.utils.SecurityTestUtils;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.FileUtil;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:function-application-context-test.xml")
// @Ignore
public class TopologyRecoveryTest {

    private static final String SOURCE_COMPONENT_NAME = "TestComponentSource";
    private static final String TARGET_COMPONENT_NAME = "TestComponent";
    private static final String TEST_ARCHIVE_NAME = "test-topo-recovery-types";
    private static final String TEST_ARCHIVE_VERSION = "0.1-SNAPSHOT";
    private static final String TEST_TOPOLOGY_NAME = "test-recovery-topology";

    private static final String TEST_TYPES_PATH = "src/test/resources/data/csars/topology_recovery/test-topo-recovery-types.yaml";
    private static final String TEST_TOPOLOGY_PATH = "src/test/resources/data/csars/topology_recovery/sample-topology-test-recovery.yml";
    private static final String NODE_TYPE_DELETED_TYPES_PATH = "src/test/resources/data/csars/topology_recovery/test-recovery-nodetype-deleted-types.yaml";
    private static final String RELATIONSHIP_TYPE_DELETED_TYPES_PATH = "src/test/resources/data/csars/topology_recovery/test-recovery-reltype-deleted-types.yaml";
    private static final String REQ_CAPA_DELETED_TYPES_PATH = "src/test/resources/data/csars/topology_recovery/test-recovery-capa-requirement-deleted-types.yaml";
    private static final String REQ_CAPA_BOUND_REACHED_TYPES_PATH = "src/test/resources/data/csars/topology_recovery/test-recovery-bound-reached-types.yaml";
    @Inject
    private ArchiveUploadService archiveUploadService;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private TopologyService topologyService;
    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource
    private TopologyTemplateVersionService versionService;

    @Value("${directories.alien}/${directories.csar_repository}")
    private String alienRepoDir;
    @Inject
    private TopologyRecoveryService topologyRecoveryService;
    private Path artifactsDirectory = Paths.get("target/git-artifacts");
    private RepositoryManager repositoryManager = new RepositoryManager();

    private Topology intialTopology;

    @PostConstruct
    private void PostConstruct() throws Throwable {
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

        Path typesPath = artifactsDirectory.resolve(normativeLocalName);
        Path typesZipPath = artifactsDirectory.resolve(normativeLocalName + ".zip");
        FileUtil.zip(typesPath, typesZipPath);
        ParsingResult<Csar> result = archiveUploadService.upload(typesZipPath, CSARSource.OTHER);
        for (ParsingError error : result.getContext().getParsingErrors()) {
            System.out.println(error.getErrorLevel() + " " + error.getProblem());
        }

    }

    @Before
    public void beforeTest() throws Throwable {
        // clean indexes
        alienDAO.delete(Topology.class, QueryBuilders.matchAllQuery());
        assertEquals(0, alienDAO.count(Topology.class, QueryBuilders.matchAllQuery()));
        alienDAO.delete(TopologyTemplate.class, QueryBuilders.matchAllQuery());
        assertEquals(0, alienDAO.count(TopologyTemplate.class, QueryBuilders.matchAllQuery()));

        BoolQueryBuilder query = QueryBuilders.boolQuery().mustNot(QueryBuilders.matchPhraseQuery("name", "tosca-normative-types"));
        // alienDAO.delete(Csar.class, new Csar(TEST_ARCHIVE_NAME, TEST_ARCHIVE_VERSION).getId());
        alienDAO.delete(Csar.class, query);
        assertEquals(1, alienDAO.count(Csar.class, QueryBuilders.matchAllQuery()));

        // upload test archives
        uploadWithoutError(TEST_TYPES_PATH);
        uploadWithoutError(TEST_TOPOLOGY_PATH);

        Topology topology = getTestTopology();
        assertEquals(3, topology.getNodeTemplates().size());
        NodeTemplate testComponentSource = topology.getNodeTemplates().get(SOURCE_COMPONENT_NAME);
        assertEquals(8, testComponentSource.getRelationships().size());
        intialTopology = topology;
    }

    private ParsingResult<Csar> upload(String path) throws Throwable {
        Path typesPath;
        Path zipPath;
        typesPath = Paths.get(path);
        zipPath = Files.createTempFile("test-csar", ".zip");
        FileUtil.zip(typesPath, zipPath);
        return archiveUploadService.upload(zipPath, CSARSource.OTHER);
    }

    private ParsingResult<Csar> uploadWithoutError(String path) throws Throwable {
        ParsingResult<Csar> result = upload(path);
        assertFalse(result.hasError(ParsingErrorLevel.ERROR));
        return result;
    }

    private Topology getTestTopology() {
        TopologyTemplate topologyTeplate = topologyServiceCore.searchTopologyTemplateByName(TEST_TOPOLOGY_NAME);
        Topology topology = alienDAO.customFind(Topology.class, QueryBuilders.matchQuery("delegateId", topologyTeplate.getId()));
        return topology;
    }

    @Test
    public void testDeleteNodeType() throws Throwable {
        assertNotNull(intialTopology.getNodeTemplates().get(TARGET_COMPONENT_NAME));
        ParsingResult<Csar> result = uploadWithoutError(NODE_TYPE_DELETED_TYPES_PATH);
        Set<CSARDependency> dependencies = assertUpdatedDependency(result);
        topologyRecoveryService.recoverTopology(dependencies, intialTopology);
        Topology topology = getTestTopology();
        assertEquals(2, topology.getNodeTemplates().size());
        assertFalse(topology.getNodeTemplates().containsKey(TARGET_COMPONENT_NAME));
        assertEquals(1, topology.getNodeTemplates().get(SOURCE_COMPONENT_NAME).getRelationships().size());
    }

    private Set<CSARDependency> assertUpdatedDependency(ParsingResult<Csar> result) {
        Set<CSARDependency> dependencies = topologyRecoveryService.getUpdatedDependencies(intialTopology);
        assertEquals(1, dependencies.size());
        CSARDependency dependency = dependencies.iterator().next();
        assertEquals(result.getResult().getName(), dependency.getName());
        assertEquals(result.getResult().getVersion(), dependency.getVersion());
        return dependencies;
    }

    @Test
    public void testDeleteRelationshipType() throws Throwable {
        NodeTemplate nodeTemplate = intialTopology.getNodeTemplates().get(SOURCE_COMPONENT_NAME);
        assertTrue(nodeTemplate.getRelationships().containsKey("testComponentConnectsToTestComponent"));

        ParsingResult<Csar> result = uploadWithoutError(RELATIONSHIP_TYPE_DELETED_TYPES_PATH);
        Set<CSARDependency> dependencies = assertUpdatedDependency(result);
        topologyRecoveryService.recoverTopology(dependencies, intialTopology);

        Topology recoveredTopology = getTestTopology();
        assertEquals(3, recoveredTopology.getNodeTemplates().size());
        nodeTemplate = recoveredTopology.getNodeTemplates().get(SOURCE_COMPONENT_NAME);
        assertTrue(MapUtils.isNotEmpty(nodeTemplate.getRelationships()));
        assertEquals(7, nodeTemplate.getRelationships().size());
        assertFalse(nodeTemplate.getRelationships().containsKey("testComponentConnectsToTestComponent"));
    }

    @Test
    public void testDeleteRequirementAndCapability() throws Throwable {
        NodeTemplate sourceNodeTemplate = intialTopology.getNodeTemplates().get(SOURCE_COMPONENT_NAME);
        assertTrue(sourceNodeTemplate.getRequirements().containsKey("req_to_be_deleted"));
        assertTrue(sourceNodeTemplate.getRelationships().containsKey("reqToBeDeletedTestComponent"));
        assertTrue(sourceNodeTemplate.getRelationships().containsKey("capaToBeDeletedTestComponent"));
        NodeTemplate targetNodeTemplate = intialTopology.getNodeTemplates().get(TARGET_COMPONENT_NAME);
        assertTrue(targetNodeTemplate.getCapabilities().containsKey("capa_to_be_deleted"));

        ParsingResult<Csar> result = uploadWithoutError(REQ_CAPA_DELETED_TYPES_PATH);
        Set<CSARDependency> dependencies = assertUpdatedDependency(result);
        topologyRecoveryService.recoverTopology(dependencies, intialTopology);

        Topology recoveredTopology = getTestTopology();
        assertEquals(3, recoveredTopology.getNodeTemplates().size());
        sourceNodeTemplate = recoveredTopology.getNodeTemplates().get(SOURCE_COMPONENT_NAME);
        targetNodeTemplate = recoveredTopology.getNodeTemplates().get(TARGET_COMPONENT_NAME);
        assertFalse(sourceNodeTemplate.getRequirements().containsKey("req_to_be_deleted"));
        assertFalse(targetNodeTemplate.getCapabilities().containsKey("capa_to_be_deleted"));

        assertEquals(6, sourceNodeTemplate.getRelationships().size());
        assertFalse(sourceNodeTemplate.getRelationships().containsKey("reqToBeDeletedTestComponent"));
        assertFalse(sourceNodeTemplate.getRelationships().containsKey("capaToBeDeletedTestComponent"));
    }

    @Test
    public void testRequirementCapaBoundReached() throws Throwable {
        NodeTemplate sourceNodeTemplate = intialTopology.getNodeTemplates().get(SOURCE_COMPONENT_NAME);
        assertTrue(sourceNodeTemplate.getRelationships().containsKey("capaUpperBoundTestTestComponent"));
        assertTrue(sourceNodeTemplate.getRelationships().containsKey("capaUpperBoundTestTestComponent1"));
        assertTrue(sourceNodeTemplate.getRelationships().containsKey("reqUpperBoundTestTestComponent"));
        assertTrue(sourceNodeTemplate.getRelationships().containsKey("reqUpperBoundTestTestComponent1"));

        ParsingResult<Csar> result = uploadWithoutError(REQ_CAPA_BOUND_REACHED_TYPES_PATH);
        Set<CSARDependency> dependencies = assertUpdatedDependency(result);
        topologyRecoveryService.recoverTopology(dependencies, intialTopology);

        Topology recoveredTopology = getTestTopology();
        assertEquals(3, recoveredTopology.getNodeTemplates().size());
        sourceNodeTemplate = recoveredTopology.getNodeTemplates().get(SOURCE_COMPONENT_NAME);
        assertTrue(MapUtils.isNotEmpty(sourceNodeTemplate.getRelationships()));
        assertEquals(6, sourceNodeTemplate.getRelationships().size());
        assertFalse(sourceNodeTemplate.getRelationships().containsKey("capaUpperBoundTestTestComponent"));
        // assertFalse(sourceNodeTemplate.getRelationships().containsKey("capaUpperBoundTestTestComponent1"));
        assertFalse(sourceNodeTemplate.getRelationships().containsKey("reqUpperBoundTestTestComponent"));
        // assertFalse(sourceNodeTemplate.getRelationships().containsKey("reqUpperBoundTestTestComponent1"));
    }

}
