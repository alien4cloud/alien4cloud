package alien4cloud.orchestrator;

import alien4cloud.component.dao.AbstractDAOTest;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.orchestrators.services.OrchestratorService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by lucboutier on 16/06/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@Slf4j
@DirtiesContext
public class OrchestratorDaoTest extends AbstractDAOTest {
    @Resource
    private OrchestratorService orchestratorService;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    @Test
    public void getEnabledOrchestratorTest() throws NoSuchFieldException, IllegalAccessException {
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setId("a");
        orchestrator.setState(OrchestratorState.DISABLED);
        dao.save(orchestrator);
        orchestrator.setId("b");
        orchestrator.setState(OrchestratorState.CONNECTED);
        dao.save(orchestrator);

        List<Orchestrator> enabledOrchestrators = orchestratorService.getAllEnabledOrchestrators();
        System.out.println(enabledOrchestrators.size());
        // alienDAO
    }
}
