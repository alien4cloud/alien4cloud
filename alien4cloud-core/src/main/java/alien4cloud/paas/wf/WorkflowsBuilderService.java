package alien4cloud.paas.wf;

import java.util.Map;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Maps;
import org.springframework.stereotype.Component;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.paas.plan.TopologyTreeBuilderService;

@Component
@Slf4j
public class WorkflowsBuilderService {

    @Resource
    private TopologyTreeBuilderService topologyTreeBuilderService;

    @Resource
    private InstallWorkflowBuilder installWorkflowBuilder;

    @Resource
    private UninstallWorkflowBuilder uninstallWorkflowBuilder;

    @Resource
    private CustomWorkflowBuilder customWorkflowBuilder;

    public void initWorkflows(Topology topology) {
        Map<String, Workflow> wfs = topology.getWorkflows();
        if (wfs == null) {
            wfs = Maps.newHashMap();
            topology.setWorkflows(wfs);
        }
        if (wfs.isEmpty()) {
            Workflow install = new Workflow();
            install.setStandard(true);
            install.setName(Workflow.INSTALL_WF);
            wfs.put(Workflow.INSTALL_WF, install);
            Workflow uninstall = new Workflow();
            uninstall.setStandard(true);
            uninstall.setName(Workflow.UNINSTALL_WF);
            wfs.put(Workflow.UNINSTALL_WF, uninstall);
        }
        debugWorkflow(topology);
    }

    public Workflow ceateWorkflow(Topology topology) {
        String workflowName = getWorkflowName(topology, "newWf", 0);
        Workflow wf = new Workflow();
        wf.setName(workflowName);
        wf.setStandard(false);
        Map<String, Workflow> wfs = topology.getWorkflows();
        if (wfs == null) {
            wfs = Maps.newHashMap();
            topology.setWorkflows(wfs);
        }
        wfs.put(workflowName, wf);
        return wf;
    }

    private String getWorkflowName(Topology topology, String name, int index) {
        String workflowName = name;
        if (index > 0) {
            workflowName += "_" + index;
        }
        if (topology.getWorkflows() != null && topology.getWorkflows().containsKey(workflowName)) {
            return getWorkflowName(topology, name, ++index);
        } else {
            return workflowName;
        }
    }

    private void debugWorkflow(Topology topology) {
        for (Workflow wf : topology.getWorkflows().values()) {
            debugWorkflow(wf);
        }
    }

    private void debugWorkflow(Workflow wf) {
        StringBuilder stringBuilder = new StringBuilder("\n ====== http://www.webgraphviz.com/ ========\n");
        int subgraphCount = 0;
        stringBuilder.append("\ndigraph ").append(wf.getName()).append(" {");
        stringBuilder.append("\n  node [shape=box];");

        for (String host : wf.getHosts()) {
            stringBuilder.append("\n  subgraph cluster_").append(++subgraphCount).append(" {");
            stringBuilder.append("\n    label = \"").append(host).append("\";\n    color=blue;");
            for (AbstractStep step : wf.getSteps().values()) {
                if (step instanceof NodeActivityStep && host.equals(((NodeActivityStep) step).getHostId())) {
                    stringBuilder.append("\n    \"").append(step.getName()).append("\";");
                }
            }
            stringBuilder.append("\n  }\n");
        }
        for (AbstractStep step : wf.getSteps().values()) {
            if (step.getFollowingSteps() != null) {
                for (String following : step.getFollowingSteps()) {
                    stringBuilder.append("\n  \"").append(step.getName()).append("\" -> \"").append(following).append("\";");
                }
            }
            if (step.getFollowingSteps() == null || step.getFollowingSteps().isEmpty()) {
                stringBuilder.append("\n  \"").append(step.getName()).append("\" -> end;");
            }
            if (step.getPrecedingSteps() == null || step.getPrecedingSteps().isEmpty()) {
                stringBuilder.append("\n  start -> \"").append(step.getName()).append("\";");
            }
        }
        stringBuilder.append("\n  start [shape=doublecircle];\n");
        stringBuilder.append("\n  end [shape=circle];\n");
        stringBuilder.append("\n}\n");
        stringBuilder.append("\n======================\n");
        log.info(stringBuilder.toString());
    }

    public void addNode(Topology topology, String nodeName, NodeTemplate nodeTemplate) {
        initWorkflows(topology);
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        PaaSNodeTemplate paaSNodeTemplate = paaSTopology.getAllNodes().get(nodeName);
        boolean isCompute = WorkflowUtils.isCompute(paaSNodeTemplate) /* || WorkflowUtils.isNetwork(paaSNodeTemplate) */;
        for (Workflow wf : topology.getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
            builder.addNode(wf, paaSTopology, paaSNodeTemplate, isCompute);
            builder.fillHostId(wf, paaSTopology);
        }
        debugWorkflow(topology);
    }

    public void removeNode(Topology topology, String nodeName, NodeTemplate nodeTemplate) {
        initWorkflows(topology);
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        for (Workflow wf : topology.getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
            builder.removeNode(wf, paaSTopology, nodeName);
            builder.fillHostId(wf, paaSTopology);
        }
        debugWorkflow(topology);
    }

    public void addRelationship(Topology topology, String nodeTemplateName, String relationshipName) {
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        PaaSNodeTemplate paaSNodeTemplate = paaSTopology.getAllNodes().get(nodeTemplateName);
        PaaSRelationshipTemplate pasSRelationshipTemplate = paaSNodeTemplate.getRelationshipTemplate(relationshipName, nodeTemplateName);
        for (Workflow wf : topology.getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
            builder.addRelationship(wf, paaSTopology, paaSNodeTemplate, pasSRelationshipTemplate);
            builder.fillHostId(wf, paaSTopology);
        }
        debugWorkflow(topology);
    }

    public void removeRelationship(Topology topology, String nodeTemplateName, String relationshipName, RelationshipTemplate relationshipTemplate) {
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        PaaSNodeTemplate paaSNodeTemplate = paaSTopology.getAllNodes().get(nodeTemplateName);
        String relationhipTarget = relationshipTemplate.getTarget();
        for (Workflow wf : topology.getWorkflows().values()) {
            AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
            builder.removeRelationship(wf, paaSTopology, paaSNodeTemplate, relationhipTarget);
            builder.fillHostId(wf, paaSTopology);
        }
    }

    public Workflow removeEdge(Topology topology, String workflowName, String from, String to) {
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            // throw ex
        }
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.removeEdge(wf, paaSTopology, from, to);
        return wf;
    }

    public Workflow connectStepFrom(Topology topology, String workflowName, String stepId, String[] stepNames) {
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            // throw ex
        }
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.connectStepFrom(wf, paaSTopology, stepId, stepNames);
        return wf;
    }

    public Workflow connectStepTo(Topology topology, String workflowName, String stepId, String[] stepNames) {
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            // throw ex
        }
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.connectStepTo(wf, paaSTopology, stepId, stepNames);
        return wf;
    }

    private AbstractWorkflowBuilder getWorkflowBuilder(Workflow workflow) {
        if (workflow.isStandard()) {
            if (workflow.getName().equals(Workflow.INSTALL_WF)) {
                return installWorkflowBuilder;
            } else if (workflow.getName().equals(Workflow.UNINSTALL_WF)) {
                return uninstallWorkflowBuilder;
            }
        }
        return customWorkflowBuilder;
    }

    public Workflow removeStep(Topology topology, String workflowName, String stepId) {
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            // throw ex
        }
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.removeStep(wf, paaSTopology, stepId);
        debugWorkflow(wf);
        return wf;
    }

    public Workflow renameStep(Topology topology, String workflowName, String stepId, String newStepName) {
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            // throw ex
        }
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.renameStep(wf, paaSTopology, stepId, newStepName);
        debugWorkflow(wf);
        return wf;
    }

    public Workflow addActivity(Topology topology, String workflowName, String relatedStepId, boolean before, AbstractActivity activity) {
        Workflow wf = topology.getWorkflows().get(workflowName);
        if (wf == null) {
            // throw ex
        }
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(topology);
        AbstractWorkflowBuilder builder = getWorkflowBuilder(wf);
        builder.addActivity(wf, paaSTopology, relatedStepId, before, activity);
        builder.fillHostId(wf, paaSTopology);
        debugWorkflow(wf);
        return wf;
    }

}
