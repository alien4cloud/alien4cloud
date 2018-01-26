package alien4cloud.utils;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.git.RepositoryManager;
import alien4cloud.model.components.CSARSource;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.paas.function.BadUsageKeywordException;
import alien4cloud.paas.function.FunctionEvaluator;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.plan.TopologyTreeBuilderService;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;
import alien4cloud.security.model.Role;
import alien4cloud.test.utils.SecurityTestUtils;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.AbstractToscaParserSimpleProfileTest;
import alien4cloud.tosca.parser.ParserTestUtil;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.services.ApplicationUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.alien4cloud.tosca.catalog.ArchiveUploadService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractInstantiableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@DirtiesContext
public class ComplexPropertyMergeTest {
    private static boolean INITIALIZED = false;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private ArchiveUploadService archiveUploadService;

    @Resource
    private TopologyTreeBuilderService treeBuilder;

    @Resource
    private ApplicationUtil applicationUtil;

    @Value("${directories.alien}/${directories.csar_repository}")
    private String alienRepoDir;

    private Path artifactsDirectory = Paths.get("target/git-artifacts");
    private RepositoryManager repositoryManager = new RepositoryManager();
    private Topology topology;

    @PostConstruct
    public void postConstruct() throws Throwable {
        if (!INITIALIZED) {
            if (Files.exists(Paths.get(alienRepoDir))) {
                try {
                    FileUtil.delete(Paths.get(alienRepoDir));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            SecurityTestUtils.setTestAuthentication(Role.ADMIN);

            alienDAO.delete(Csar.class, QueryBuilders.matchAllQuery());
            String normativeLocalName = "tosca-normative-types";
            repositoryManager.cloneOrCheckout(artifactsDirectory, "https://github.com/alien4cloud/tosca-normative-types.git", "1.2.0", normativeLocalName);

            Path typesPath = artifactsDirectory.resolve(normativeLocalName);
            Path typesZipPath = artifactsDirectory.resolve(normativeLocalName + ".zip");
            FileUtil.zip(typesPath, typesZipPath);
            ParsingResult<Csar> result = archiveUploadService.upload(typesZipPath, CSARSource.OTHER, AlienConstants.GLOBAL_WORKSPACE_ID);
            ParserTestUtil.displayErrors(result);

            AbstractToscaParserSimpleProfileTest.assertNoBlocker(result);

            INITIALIZED = true;
        }

        ParsingResult<ArchiveRoot> result = applicationUtil.parseYamlTopology("src/test/resources/data/csars/merge_complex/tosca");
        // AbstractToscaParserSimpleProfileTest.assertNoBlocker(result);
        topology = result.getResult().getTopology();
        topology.setId(UUID.randomUUID().toString());
        topology.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
    }

    @Test
    public void mergeComplexProperty() {
        int max = 5;

        for (int i=1; i<max; i++) {
            NodeTemplate source = topology.getNodeTemplates().get("Source" + i);
            NodeTemplate target = topology.getNodeTemplates().get("Target" + i);
            NodeTemplate expected = topology.getNodeTemplates().get("Merged" + i);
            Set<String> untouchable = Sets.newHashSet();
            Map<String, AbstractPropertyValue> actual = PropertyUtil.merge(source.getProperties(), target.getProperties(), true, untouchable);
            Assert.assertEquals("Merge result of Source" + i + " and Target" + i, expected.getProperties(), actual);
        }

    }
}
