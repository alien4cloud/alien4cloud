package alien4cloud.paas.wf.SUPALIEN_1202;

import alien4cloud.paas.wf.model.Path;
import alien4cloud.paas.wf.util.WorkflowGraphUtils;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.rest.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
public class ParseWorkflow {


    private static void parseFile(File file, String... worflowNames) throws Exception {
        String fileContent = new String(Files.readAllBytes(Paths.get(file.getPath())), StandardCharsets.UTF_8);
        log.info("======================================");
        log.info("Parsing file " + file.getName());
        ParsedTopology topology = JsonUtil.readObject(fileContent, ParsedTopology.class);
        log.info("Analysing topology " + topology.get_id());
        log.info("--------------------------------------");

        if (worflowNames != null && worflowNames.length > 0) {
            for (String worflowName : worflowNames) {
                processWorkflow(topology.get_id(), worflowName, topology.get_source().getUnprocessedWorkflows().get(worflowName));
            }
        } else {
            for (Map.Entry<String, Workflow> e : topology.get_source().getUnprocessedWorkflows().entrySet()) {
                processWorkflow(topology.get_id(), e.getKey(), e.getValue());
            }
        }

    }

    private static void processWorkflow(String topologyId, String name, Workflow wf) {
        log.info("Worflow {}.{} has {} steps (hasCustomModifications: {})", topologyId, name, wf.getSteps().size(), wf.isHasCustomModifications());
/*
        System.out.println(WorkflowUtils.debugWorkflow(wf, false));
*/
        log.info("Analysing worflow {}.{}", topologyId, name);
        long startTime = System.currentTimeMillis();
        List<Path> cycles = WorkflowGraphUtils.getWorkflowGraphCycles(wf);
        long duration = System.currentTimeMillis() - startTime;
        log.info("Worflow analysis for {}.{} took {} ms", topologyId, name, duration);
    }

    @Test
    @Ignore
    public void parseFile() throws Exception {

        File file = new File("/Users/xdegenne/work/atos/sg/cases/SUPALIEN-1202/guilty/IPJ_SIMULATEUR_PPI-0.1.1-DEV-SNAPSHOT_before_reset.json");
        ParseWorkflow.parseFile(file, "install");
    }

    @Test
    @Ignore
    public void parseFolder() throws Exception {
        File dir = new File("/Users/xdegenne/work/atos/sg/cases/SUPALIEN-1202/guilty/");
        log.info("Looking for files in {}", dir.getPath());
        File[] directoryListing = dir.listFiles();
        Arrays.sort(directoryListing, new Comparator<File>()
        {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        if (directoryListing != null) {
            for (File child : directoryListing) {
                ParseWorkflow.parseFile(child);
            }
        } else {
            // Handle the case where dir is not really a directory.
            // Checking dir.isDirectory() above would not be sufficient
            // to avoid race conditions with another process that deletes
            // directories.
        }
    }

}
