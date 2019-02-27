package org.alien4cloud.tosca.catalog.index;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;

import java.util.concurrent.ExecutionException;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CsarServiceTest {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;
    @Inject
    private CsarService csarService;

    @Test
    public void isArchiveDeployedTest() throws ExecutionException, InterruptedException {
        //alienDao.getClient().prepareDeleteByQuery(new String[] { "csar" }).setQuery(QueryBuilders.matchAllQuery()).execute().get();
        alienDao.delete (Csar.class, QueryBuilders.matchAllQuery());
        Csar csar = new Csar("archive", "1.0.0-SNAPSHOT");
        csar.setDependencies(Sets.newHashSet(new CSARDependency("toto", "1.0.0"), new CSARDependency("titi", "2.0.0")));
        alienDao.save(csar);
        csar = new Csar("archive2", "1.0.0-SNAPSHOT");
        csar.setDependencies(Sets.newHashSet(new CSARDependency("tata", "1.0.0"), new CSARDependency("tutu", "2.0.0")));
        alienDao.save(csar);

        Csar[] csars = csarService.getDependantCsars("toto", "2.0.0");
        Assert.assertEquals(0, csars.length);

        csar = new Csar("archive3", "1.0.0-SNAPSHOT");
        csar.setDependencies(Sets.newHashSet(new CSARDependency("tata", "1.0.0"), new CSARDependency("toto", "2.0.0")));
        alienDao.save(csar);

        csars = csarService.getDependantCsars("toto", "2.0.0");
        Assert.assertEquals(1, csars.length);

        csar = new Csar("archive4", "1.0.0-SNAPSHOT");
        csar.setDependencies(Sets.newHashSet(new CSARDependency("tata", "1.0.0"), new CSARDependency("toto", "2.0.0")));
        alienDao.save(csar);

        csars = csarService.getDependantCsars("toto", "2.0.0");
        Assert.assertEquals(2, csars.length);
    }
}
