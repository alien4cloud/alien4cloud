package alien4cloud.component.dao;

import java.util.Date;

import javax.annotation.Resource;

import alien4cloud.common.AlienConstants;
import org.elasticsearch.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.Constants;
import alien4cloud.component.NodeTypeScoreService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.utils.MapUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
public class NodeTypeScoreServiceTest {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    @Resource
    NodeTypeScoreService scoreService;

    @Test
    public void testScoreService() throws InterruptedException {
        // Initialize test data
        NodeType indexedNodeType = new NodeType();
        indexedNodeType.setElementId("mordor");
        indexedNodeType.setArchiveName("middleEarth");
        indexedNodeType.setArchiveVersion("1.0.0");
        indexedNodeType.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
        indexedNodeType.setCreationDate(new Date());
        indexedNodeType.setLastUpdateDate(new Date());
        indexedNodeType.setDefaultCapabilities(Lists.newArrayList("very_evil"));
        dao.save(indexedNodeType);
        String mordor100Id = indexedNodeType.getId();

        indexedNodeType.setArchiveVersion("1.0.1");
        indexedNodeType.setCreationDate(new Date());
        indexedNodeType.setLastUpdateDate(new Date());
        indexedNodeType.setDefaultCapabilities(Lists.newArrayList("deprecated_evil"));
        dao.save(indexedNodeType);
        String mordor101Id = indexedNodeType.getId();

        indexedNodeType.setElementId("isengard");
        indexedNodeType.setArchiveName("middleEarth");
        indexedNodeType.setArchiveVersion("1.0.0");
        indexedNodeType.setCreationDate(new Date());
        indexedNodeType.setLastUpdateDate(new Date());
        indexedNodeType.setDefaultCapabilities(Lists.newArrayList("evil"));
        dao.save(indexedNodeType);
        String isengard100Id = indexedNodeType.getId();

        Topology topology = new Topology();
        topology.setId("topology");
        topology.setArchiveName("test-archive");
        topology.setArchiveVersion("1.0.0");
        topology.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
        topology.setNodeTemplates(MapUtil.newHashMap(new String[] { "isengard" },
                new NodeTemplate[] { new NodeTemplate(indexedNodeType.getId(), null, null, null, null, null, null, null) }));
        dao.save(topology);

        indexedNodeType.setElementId("osgiliath");
        indexedNodeType.setArchiveName("middleEarth");
        indexedNodeType.setArchiveVersion("1.0.0");
        indexedNodeType.setCreationDate(new Date());
        indexedNodeType.setLastUpdateDate(new Date());
        indexedNodeType.setDefaultCapabilities(null);
        dao.save(indexedNodeType);
        String osgiliath100Id = indexedNodeType.getId();

        // perform scoring
        scoreService.run();

        // check that order on query is correct
        GetMultipleDataResult data = dao.search(NodeType.class, "", null, Constants.DEFAULT_ES_SEARCH_SIZE);
        Assert.assertEquals(4, data.getData().length);
        Assert.assertEquals(isengard100Id, ((NodeType) data.getData()[0]).getId());
        Assert.assertEquals(1011, ((NodeType) data.getData()[0]).getAlienScore());
        Assert.assertEquals(mordor101Id, ((NodeType) data.getData()[1]).getId());
        Assert.assertEquals(1010, ((NodeType) data.getData()[1]).getAlienScore());
        Assert.assertEquals(osgiliath100Id, ((NodeType) data.getData()[2]).getId());
        Assert.assertEquals(1000, ((NodeType) data.getData()[2]).getAlienScore());
        Assert.assertEquals(mordor100Id, ((NodeType) data.getData()[3]).getId());
        Assert.assertEquals(10, ((NodeType) data.getData()[3]).getAlienScore());
    }

}
